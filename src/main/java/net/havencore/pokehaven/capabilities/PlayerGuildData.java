package net.havencore.pokehaven.capabilities;

import net.havencore.pokehaven.PokeHaven;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Capability API exposing segment-based player data. The guild system occupies its own segment and provides
 * strongly-typed getters and setters built on top of the generic storage helpers defined here.
 *
 * <p>When introducing new values simply:</p>
 * <ol>
 *     <li>Create a {@link PlayerDataKey} constant referencing the segment, value path, and {@link PlayerDataType} to use.</li>
 *     <li>Add default getter/setter methods that forward to {@link #get(PlayerDataKey)} and {@link #set(PlayerDataKey, Object)}.</li>
 *     <li>Update gameplay code to call the new accessors.</li>
 * </ol>
 */
public interface PlayerGuildData {
    ResourceLocation GUILD_SEGMENT = ResourceLocation.fromNamespaceAndPath(PokeHaven.MODID, "guild");

    PlayerDataKey<String> GUILD_NAME = PlayerDataKey.string(GUILD_SEGMENT, "guild_name");
    PlayerDataKey<String> FACTION_NAME = PlayerDataKey.string(GUILD_SEGMENT, "faction_name");
    PlayerDataKey<Boolean> FACTION_LEADER = PlayerDataKey.bool(GUILD_SEGMENT, "is_faction_leader");

    /**
     * Retrieves a value stored for the supplied key.
     */
    <T> Optional<T> get(PlayerDataKey<T> key);

    /**
     * Stores a value under the supplied key. Passing {@code null} removes the value from the segment.
     */
    <T> void set(PlayerDataKey<T> key, @Nullable T value);

    /**
     * Removes every value stored in the supplied segment.
     */
    void clearSegment(ResourceLocation segmentId);

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
