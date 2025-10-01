package net.havencore.pokehaven.capabilities.impl;

import net.havencore.pokehaven.PokeHaven;
import net.havencore.pokehaven.capabilities.IPlayerData;
import net.havencore.pokehaven.capabilities.PlayerDataKey;
import net.minecraft.resources.ResourceLocation;

/**
 * PvP-focused view of the segmented player data capability. Provides toggles for combat
 * participation, stealing permissions, and a running win counter.
 */
public interface PlayerPVPData extends IPlayerData {
    ResourceLocation PVP_SEGMENT = ResourceLocation.fromNamespaceAndPath(PokeHaven.MODID, "pvp");

    PlayerDataKey<Boolean> PVP_ENABLED = PlayerDataKey.bool(PVP_SEGMENT, "enable_pvp");
    PlayerDataKey<Boolean> PVP_ALLOW_STEALING = PlayerDataKey.bool(PVP_SEGMENT, "allow_stealing");
    PlayerDataKey<Integer> PVP_BATTLES_WON = PlayerDataKey.intKey(PVP_SEGMENT, "battles_won");

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
