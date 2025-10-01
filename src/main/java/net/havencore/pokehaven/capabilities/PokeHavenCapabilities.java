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

    public static final EntityCapability<IPlayerData, Void> PLAYER_DATA = EntityCapability.createVoid(
            ResourceLocation.fromNamespaceAndPath(PokeHaven.MODID, "player_data"),
            IPlayerData.class
    );

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerEntity(PLAYER_DATA, EntityType.PLAYER, (player, context) -> new PlayerDataCapability(player));
    }
}
