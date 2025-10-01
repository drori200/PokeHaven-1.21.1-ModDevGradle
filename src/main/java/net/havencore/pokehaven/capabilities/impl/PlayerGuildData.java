package net.havencore.pokehaven.capabilities.impl;

import net.havencore.pokehaven.PokeHaven;
import net.havencore.pokehaven.capabilities.IPlayerData;
import net.havencore.pokehaven.capabilities.PlayerDataKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Guild-specific view of the segmented player data capability. Defines keys and helpers for
 * working with guild membership, faction alignment, and leadership state.
 */
public interface PlayerGuildData extends IPlayerData {
    ResourceLocation GUILD_SEGMENT = ResourceLocation.fromNamespaceAndPath(PokeHaven.MODID, "guild");

    PlayerDataKey<String> GUILD_NAME = PlayerDataKey.string(GUILD_SEGMENT, "guild_name");
    PlayerDataKey<String> FACTION_NAME = PlayerDataKey.string(GUILD_SEGMENT, "faction_name");
    PlayerDataKey<Boolean> FACTION_LEADER = PlayerDataKey.bool(GUILD_SEGMENT, "is_faction_leader");

    default Optional<String> getGuildName() {
        return get(GUILD_NAME);
    }

    default void setGuildName(@Nullable String guildName) {
        set(GUILD_NAME, guildName);
    }

    default Optional<String> getFactionName() {
        return get(FACTION_NAME);
    }

    default void setFactionName(@Nullable String factionName) {
        set(FACTION_NAME, factionName);
    }

    default boolean isFactionLeader() {
        return get(FACTION_LEADER).orElse(false);
    }

    default void setFactionLeader(boolean factionLeader) {
        set(FACTION_LEADER, factionLeader);
    }

    default void clearGuildMembership() {
        clearSegment(GUILD_SEGMENT);
    }

    default void clearMembership() {
        clearGuildMembership();
    }
}
