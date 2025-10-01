# PvP Capability Tutorial (Segmented Data Edition)

This tutorial demonstrates how to extend the **segmented player data capability** that already powers the guild system so it can also track player-vs-player (PvP) preferences. Instead of defining a brand-new capability, we will reuse the generic building blocks introduced earlier:

- [`PlayerDataAttachment`](../src/main/java/net/havencore/pokehaven/capabilities/PlayerDataAttachment.java) keeps every stored value grouped by segment.
- [`PlayerDataKey`](../src/main/java/net/havencore/pokehaven/capabilities/PlayerDataKey.java) and [`PlayerDataType`](../src/main/java/net/havencore/pokehaven/capabilities/PlayerDataType.java) describe individual entries and how they serialize.
- [`PlayerGuildData`](../src/main/java/net/havencore/pokehaven/capabilities/PlayerGuildData.java) exposes the capability API with generic `get`/`set` helpers alongside guild defaults.
- [`PlayerDataCapability`](../src/main/java/net/havencore/pokehaven/capabilities/PlayerDataCapability.java) is the runtime implementation that automatically synchronizes attachment changes back to clients.
- [`PlayerDataAccess`](../src/main/java/net/havencore/pokehaven/capabilities/PlayerDataAccess.java) provides a convenience accessor for retrieving the capability from any player instance.

By leaning on this infrastructure we can add new data fields simply by declaring keys and delegating to the shared helpers.

---

## 1. Define a PvP Segment and Keys

First, pick a new segment identifier so PvP data stays isolated from other gameplay systems. Inside `PlayerGuildData` add a constant `ResourceLocation` for the PvP segment along with three `PlayerDataKey` definitions—two booleans and one integer.

```java
public interface PlayerGuildData {
    ResourceLocation PVP_SEGMENT = ResourceLocation.fromNamespaceAndPath(PokeHaven.MODID, "pvp");

    PlayerDataKey<Boolean> PVP_ENABLED = PlayerDataKey.bool(PVP_SEGMENT, "enable_pvp");
    PlayerDataKey<Boolean> PVP_ALLOW_STEALING = PlayerDataKey.bool(PVP_SEGMENT, "allow_stealing");
    PlayerDataKey<Integer> PVP_BATTLES_WON = PlayerDataKey.intKey(PVP_SEGMENT, "battles_won");

    // existing guild constants …
}
```

Each key points at the PvP segment and specifies a distinct value path. The static factory methods (`bool`, `intKey`, etc.) automatically wire in the correct `PlayerDataType` so the attachment knows which codec to use when persisting the value.

---

## 2. Add Getters and Setters to the Capability View

Next, expose friendly accessors right inside `PlayerGuildData`. Because the interface already defines generic `get` and `set` helpers, the new methods become one-liners:

```java
public interface PlayerGuildData {
    // …key declarations from above…

    default boolean isPvpEnabled() {
        return get(PVP_ENABLED).orElse(false);
    }

    default void setPvpEnabled(boolean enabled) {
        set(PVP_ENABLED, enabled);
    }

    default boolean isStealingAllowed() {
        return get(PVP_ALLOW_STEALING).orElse(false);
    }

    default void setStealingAllowed(boolean allowed) {
        set(PVP_ALLOW_STEALING, allowed);
    }

    default int getBattlesWon() {
        return get(PVP_BATTLES_WON).orElse(0);
    }

    default void setBattlesWon(int wins) {
        set(PVP_BATTLES_WON, wins);
    }

    default void incrementBattlesWon() {
        setBattlesWon(getBattlesWon() + 1);
    }
}
```

The setters automatically persist changes because `PlayerDataCapability` delegates to `PlayerDataAttachment.with(...)`, replacing the immutable snapshot and syncing it to the client whenever the value actually changes. Passing `null` into `set` would remove the entry entirely if you ever need to reset to an "unset" state.

---

## 3. No Capability Plumbing Changes Required

The segmented infrastructure already registers and attaches the capability for every player:

- [`PokeHavenCapabilities`](../src/main/java/net/havencore/pokehaven/capabilities/PokeHavenCapabilities.java) registers an `EntityCapability<PlayerGuildData, Void>` and provides `PlayerDataCapability` instances to players during `RegisterCapabilitiesEvent`.
- `PlayerDataAttachments` configures the underlying [`AttachmentType`](../src/main/java/net/havencore/pokehaven/capabilities/PlayerDataAttachments.java) that persists `PlayerDataAttachment` snapshots.

Because our new PvP keys piggyback on these systems, no additional providers or event hooks are necessary—any code that obtains the `PlayerGuildData` capability automatically gains access to the new methods.

---

## 4. Retrieve the Capability at Runtime

Whenever gameplay logic needs to read or update PvP state, grab the capability via `PlayerDataAccess` and call the new accessors. Example helper methods:

```java
public final class PvpCapabilityHelper {
    private PvpCapabilityHelper() {}

    public static void togglePvp(ServerPlayer player, boolean enabled) {
        PlayerDataAccess.get(player).ifPresent(data -> data.setPvpEnabled(enabled));
    }

    public static void toggleStealing(ServerPlayer player, boolean allowed) {
        PlayerDataAccess.get(player).ifPresent(data -> data.setStealingAllowed(allowed));
    }

    public static void awardWin(ServerPlayer player) {
        PlayerDataAccess.get(player).ifPresent(PlayerGuildData::incrementBattlesWon);
    }
}
```

The optional returned by `PlayerDataAccess.get` accounts for cases where a capability might be absent (for example, before entities finish initialization on the logical client). In dedicated server logic you can confidently call `.orElseThrow(...)` if you prefer stricter guarantees.

---

## 5. Command Showcase

Here is a Brigadier command that surfaces the PvP controls to players and administrators using the new getters and setters:

```java
public final class PvpCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pvp")
            .then(Commands.literal("enable").executes(ctx -> setEnabled(ctx, true)))
            .then(Commands.literal("disable").executes(ctx -> setEnabled(ctx, false)))
            .then(Commands.literal("stealing")
                .then(Commands.literal("enable").executes(ctx -> setStealing(ctx, true)))
                .then(Commands.literal("disable").executes(ctx -> setStealing(ctx, false))))
            .then(Commands.literal("wins")
                .then(Commands.argument("count", IntegerArgumentType.integer(0))
                    .executes(ctx -> setWins(ctx, IntegerArgumentType.getInteger(ctx, "count"))))));
    }

    private static int setEnabled(CommandContext<CommandSourceStack> ctx, boolean enabled) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PlayerDataAccess.get(player).ifPresent(data -> data.setPvpEnabled(enabled));
        ctx.getSource().sendSuccess(() -> Component.literal("PvP " + (enabled ? "enabled" : "disabled")), true);
        return 1;
    }

    private static int setStealing(CommandContext<CommandSourceStack> ctx, boolean allowed) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PlayerDataAccess.get(player).ifPresent(data -> data.setStealingAllowed(allowed));
        ctx.getSource().sendSuccess(() -> Component.literal("Stealing " + (allowed ? "enabled" : "disabled")), true);
        return 1;
    }

    private static int setWins(CommandContext<CommandSourceStack> ctx, int wins) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PlayerDataAccess.get(player).ifPresent(data -> data.setBattlesWon(wins));
        ctx.getSource().sendSuccess(() -> Component.literal("Set PvP wins to " + wins), true);
        return 1;
    }
}
```

Every handler simply resolves the capability and delegates to the default methods we added earlier—no attachment plumbing or serialization concerns leak into the command layer.

---

## 6. Gameplay Hooks

Combat events, GUI buttons, or network handlers can all modify the same data. For example, increment the victory counter whenever one player defeats another:

```java
@Mod.EventBusSubscriber(modid = PokeHaven.MODID)
public final class PvpCombatHooks {
    private PvpCombatHooks() {}

    @SubscribeEvent
    public static void onPlayerKilled(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer killer && event.getEntity() instanceof ServerPlayer victim) {
            PlayerDataAccess.get(killer).ifPresent(PlayerGuildData::incrementBattlesWon);

            PlayerDataAccess.get(victim).ifPresent(data -> {
                if (!data.isPvpEnabled()) {
                    // Cancel rewards or apply penalties as needed
                }
            });
        }
    }
}
```

Because the attachment is immutable, each setter call swaps in a fresh `PlayerDataAttachment` instance; `PlayerDataCapability` detects the change and triggers `ServerPlayer#syncData`, ensuring the client mirrors the server-side state.

---

## 7. Testing Checklist

1. Launch the game and run `/pvp enable` to confirm the flag toggles and persists after relogging.
2. Toggle stealing permissions and verify any UI or gameplay checks read the updated value.
3. Use `/pvp wins <number>` to validate manual updates.
4. Trigger PvP combat to confirm win counts increment automatically.
5. Inspect the saved player data (e.g., via `/guilddata show <player>`) to see the PvP segment entries alongside guild metadata.

With these additions the PvP system coexists cleanly with other gameplay data, and future fields are as simple as adding new `PlayerDataKey` constants plus matching default accessors.
