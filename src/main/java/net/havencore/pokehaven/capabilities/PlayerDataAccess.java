package net.havencore.pokehaven.capabilities;

import net.havencore.pokehaven.capabilities.impl.PlayerGuildData;
import net.havencore.pokehaven.capabilities.impl.PlayerPVPData;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

/**
 * Helper utilities for querying persistent capability data stored on players.
 */
public final class PlayerDataAccess {
    private PlayerDataAccess() {
    }

    private static Optional<IPlayerData> resolve(Player player) {
        return Optional.ofNullable(player.getCapability(PokeHavenCapabilities.PLAYER_DATA));
    }

    public static Optional<PlayerGuildData> get(Player player) {
        return resolve(player)
                .filter(PlayerGuildData.class::isInstance)
                .map(PlayerGuildData.class::cast);
    }

    public static Optional<PlayerPVPData> getPvp(Player player) {
        return resolve(player)
                .filter(PlayerPVPData.class::isInstance)
                .map(PlayerPVPData.class::cast);
    }
}
