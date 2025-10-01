# PvP Capability Tutorial

This tutorial walks through implementing a **player-attached capability** dedicated to tracking PvP preferences and stats. By the end you will have a reusable component that stores:

- `enable_pvp`: whether the player allows PvP combat.
- `allow_stealing`: whether PvP opponents can loot them.
- `battles_won`: a running total of PvP victories.

The approach mirrors modern NeoForge/Forge practices: use an attachment as the persistence layer, define a capability interface for runtime access, and wire the provider into the mod bootstrap sequence. The steps below assume a NeoForge 1.21 environment, but the same concepts transfer to Forge with minimal renaming.

---

## 1. Plan the Data Model

Create an immutable attachment payload that holds the PvP values. Using Java’s `record` keeps the container concise and value-oriented.

```java
public record PvpSettingsAttachment(boolean enablePvp, boolean allowStealing, int battlesWon) {
    public static final Codec<PvpSettingsAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.fieldOf("enable_pvp").forGetter(PvpSettingsAttachment::enablePvp),
        Codec.BOOL.fieldOf("allow_stealing").forGetter(PvpSettingsAttachment::allowStealing),
        Codec.INT.fieldOf("battles_won").forGetter(PvpSettingsAttachment::battlesWon)
    ).apply(instance, PvpSettingsAttachment::new));

    public static PvpSettingsAttachment defaults() {
        return new PvpSettingsAttachment(false, false, 0);
    }

    public PvpSettingsAttachment withEnablePvp(boolean flag) {
        return new PvpSettingsAttachment(flag, allowStealing, battlesWon);
    }

    public PvpSettingsAttachment withAllowStealing(boolean flag) {
        return new PvpSettingsAttachment(enablePvp, flag, battlesWon);
    }

    public PvpSettingsAttachment withBattlesWon(int wins) {
        return new PvpSettingsAttachment(enablePvp, allowStealing, wins);
    }
}
```

Key points:

- The attachment is immutable—every change creates a new instance. NeoForge detects that change and synchronizes it to clients when `setData` is called.
- The `CODEC` ensures the attachment serializes to NBT for world saves and respawns.

---

## 2. Register the Attachment Type

Use a deferred register to hook into NeoForge’s attachment registry. This tells the game how to create and persist the PvP settings for each player.

```java
public final class PvpAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, PokeHaven.MOD_ID);

    public static final RegistryObject<AttachmentType<PvpSettingsAttachment>> PVP_SETTINGS =
        ATTACHMENTS.register("pvp_settings", () -> AttachmentType.builder(PvpSettingsAttachment::defaults)
            .serializer(PvpSettingsAttachment.CODEC)
            .copyOnDeath()
            .build());

    private PvpAttachments() {}
}
```

Call `PvpAttachments.ATTACHMENTS.register(eventBus);` inside your mod constructor. The `copyOnDeath()` choice keeps PvP settings through respawns; remove it if you want PvP state reset on death.

---

## 3. Define the Capability API

The capability interface exposes getters and setters so gameplay systems can read and mutate PvP preferences.

```java
public interface PvpCapability {
    boolean isPvpEnabled();
    void setPvpEnabled(boolean flag);

    boolean isStealingAllowed();
    void setStealingAllowed(boolean flag);

    int getBattlesWon();
    void setBattlesWon(int wins);

    default void incrementBattlesWon() {
        setBattlesWon(getBattlesWon() + 1);
    }
}
```

Register the capability during the global capability registration event.

```java
public final class PokeHavenCapabilities {
    public static final Capability<PvpCapability> PVP_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(PvpCapability.class);
    }

    private PokeHavenCapabilities() {}
}
```

---

## 4. Implement the Capability Wrapper

Connect the capability interface to the attachment snapshot stored on each player. The wrapper keeps a reference to the owning `Player` so it can push updates via `setData`.

```java
public final class PvpCapabilityImpl implements PvpCapability {
    private final Player player;

    public PvpCapabilityImpl(Player player) {
        this.player = player;
    }

    private PvpSettingsAttachment data() {
        return player.getData(PvpAttachments.PVP_SETTINGS.get());
    }

    private void update(PvpSettingsAttachment attachment) {
        player.setData(PvpAttachments.PVP_SETTINGS.get(), attachment);
    }

    @Override
    public boolean isPvpEnabled() {
        return data().enablePvp();
    }

    @Override
    public void setPvpEnabled(boolean flag) {
        update(data().withEnablePvp(flag));
    }

    @Override
    public boolean isStealingAllowed() {
        return data().allowStealing();
    }

    @Override
    public void setStealingAllowed(boolean flag) {
        update(data().withAllowStealing(flag));
    }

    @Override
    public int getBattlesWon() {
        return data().battlesWon();
    }

    @Override
    public void setBattlesWon(int wins) {
        update(data().withBattlesWon(wins));
    }
}
```

Because attachments are immutable, each setter clones the snapshot with the new value and then stores it back on the player. NeoForge automatically syncs to clients when the value actually changes.

---

## 5. Expose a Provider

Providers attach capabilities to specific entity types. When using attachments, the provider mostly delegates to the player’s attachment storage.

```java
public final class PvpCapabilityProvider implements ICapabilityProvider {
    private final Player player;
    private final LazyOptional<PvpCapability> optional;

    public PvpCapabilityProvider(Player player) {
        this.player = player;
        this.optional = LazyOptional.of(() -> new PvpCapabilityImpl(player));
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        return cap == PokeHavenCapabilities.PVP_CAPABILITY ? optional.cast() : LazyOptional.empty();
    }
}
```

Attach the provider during the player attachment event.

```java
@Mod.EventBusSubscriber(modid = PokeHaven.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PvpCapabilityEvents {
    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player player) {
            event.addCapability(new ResourceLocation(PokeHaven.MOD_ID, "pvp"), new PvpCapabilityProvider(player));
        }
    }
}
```

---

## 6. Convenience Accessors

Add a helper so any gameplay system can look up the capability on demand.

```java
public final class PvpCapabilityAccess {
    public static Optional<PvpCapability> get(Player player) {
        return player.getCapability(PokeHavenCapabilities.PVP_CAPABILITY).resolve();
    }

    public static PvpCapability require(Player player) {
        return get(player).orElseThrow(() -> new IllegalStateException("Missing PvP capability"));
    }

    private PvpCapabilityAccess() {}
}
```

---

## 7. Using the Capability In-Game

Once registered and attached, other systems can integrate with the PvP data.

### Toggle Command Example

```java
public class PvpCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pvp")
            .then(Commands.literal("enable").executes(ctx -> togglePvp(ctx, true)))
            .then(Commands.literal("disable").executes(ctx -> togglePvp(ctx, false)))
            .then(Commands.literal("stealing")
                .then(Commands.literal("enable").executes(ctx -> toggleStealing(ctx, true)))
                .then(Commands.literal("disable").executes(ctx -> toggleStealing(ctx, false))))
            .then(Commands.literal("wins")
                .then(Commands.argument("wins", IntegerArgumentType.integer(0))
                    .executes(ctx -> setWins(ctx, IntegerArgumentType.getInteger(ctx, "wins"))))));
    }

    private static int togglePvp(CommandContext<CommandSourceStack> ctx, boolean flag) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PvpCapabilityAccess.require(player).setPvpEnabled(flag);
        ctx.getSource().sendSuccess(() -> Component.literal("PvP " + (flag ? "enabled" : "disabled")), true);
        return 1;
    }

    private static int toggleStealing(CommandContext<CommandSourceStack> ctx, boolean flag) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PvpCapabilityAccess.require(player).setStealingAllowed(flag);
        ctx.getSource().sendSuccess(() -> Component.literal("Stealing " + (flag ? "enabled" : "disabled")), true);
        return 1;
    }

    private static int setWins(CommandContext<CommandSourceStack> ctx, int wins) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PvpCapabilityAccess.require(player).setBattlesWon(wins);
        ctx.getSource().sendSuccess(() -> Component.literal("Set PvP wins to " + wins), true);
        return 1;
    }
}
```

### Combat Hook Example

Update win counts automatically when PvP combat resolves.

```java
@Mod.EventBusSubscriber(modid = PokeHaven.MOD_ID)
public final class PvpCombatHooks {
    @SubscribeEvent
    public static void onPlayerKilled(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer killer && event.getEntity() instanceof ServerPlayer victim) {
            PvpCapabilityAccess.get(killer).ifPresent(cap -> cap.setBattlesWon(cap.getBattlesWon() + 1));

            PvpCapabilityAccess.get(victim).ifPresent(cap -> {
                if (!cap.isPvpEnabled()) {
                    // Cancel rewards or apply penalties as needed
                }
            });
        }
    }
}
```

---

## 8. Testing Checklist

1. Launch the game and join a world.
2. Run `/pvp enable` to allow PvP; verify the value syncs to the client (try logging out/in).
3. Run `/pvp stealing enable` and ensure the flag persists.
4. Use `/pvp wins 5` and confirm the command updates the attachment.
5. Simulate PvP kills to ensure `battles_won` increments properly.

Following these steps yields a modular capability that centralizes PvP configuration and stats, making it easy to extend with new fields (e.g., Elo rating, duel requests) by evolving the attachment record and exposing additional getters/setters in the capability implementation.
