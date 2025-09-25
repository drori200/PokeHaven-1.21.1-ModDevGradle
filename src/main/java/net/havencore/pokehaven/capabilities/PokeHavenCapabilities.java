package net.havencore.pokehaven.capabilities;

import net.havencore.pokehaven.PokeHaven;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Central capability registration for the mod.
 */
public final class PokeHavenCapabilities {
    private PokeHavenCapabilities() {
    }

    public static final EntityCapability<PlayerGuildData, Void> PLAYER_GUILD_DATA = EntityCapability.createVoid(
            ResourceLocation.fromNamespaceAndPath(PokeHaven.MODID, "player_guild_data"),
            PlayerGuildData.class
    );

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerEntity(PLAYER_GUILD_DATA, EntityType.PLAYER, (player, context) -> new PlayerGuildCapability(player));
    }
}
