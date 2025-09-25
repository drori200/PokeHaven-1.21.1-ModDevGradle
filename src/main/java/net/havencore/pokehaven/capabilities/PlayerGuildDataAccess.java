package net.havencore.pokehaven.capabilities;

import net.minecraft.world.entity.player.Player;

import java.util.Optional;

/**
 * Helper utilities for querying guild related data stored on players.
 */
public final class PlayerGuildDataAccess {
    private PlayerGuildDataAccess() {
    }

    public static Optional<PlayerGuildData> get(Player player) {
        return Optional.ofNullable(player.getCapability(PokeHavenCapabilities.PLAYER_GUILD_DATA));
    }
}
