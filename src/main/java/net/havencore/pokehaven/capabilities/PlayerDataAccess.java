package net.havencore.pokehaven.capabilities;

import net.minecraft.world.entity.player.Player;

import java.util.Optional;

/**
 * Helper utilities for querying persistent capability data stored on players.
 */
public final class PlayerDataAccess {
    private PlayerDataAccess() {
    }

    public static Optional<PlayerGuildData> get(Player player) {
        return Optional.ofNullable(player.getCapability(PokeHavenCapabilities.PLAYER_GUILD_DATA));
    }
}
