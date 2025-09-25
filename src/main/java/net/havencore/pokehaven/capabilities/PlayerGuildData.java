package net.havencore.pokehaven.capabilities;

import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Capability API exposing guild-related data stored for a {@link net.minecraft.world.entity.player.Player}.
 */
public interface PlayerGuildData {
    /**
     * @return the current guild name, if the player belongs to one.
     */
    Optional<String> getGuildName();

    /**
     * Updates the guild name. Passing {@code null} clears the current guild.
     */
    void setGuildName(@Nullable String guildName);

    /**
     * @return the current faction name, if the player belongs to one.
     */
    Optional<String> getFactionName();

    /**
     * Updates the faction name. Passing {@code null} clears the current faction.
     */
    void setFactionName(@Nullable String factionName);

    /**
     * @return {@code true} if the player currently leads their faction.
     */
    boolean isFactionLeader();

    /**
     * Sets whether the player is currently the leader of their faction.
     */
    void setFactionLeader(boolean factionLeader);

    /**
     * Clears all guild and faction assignments for the player.
     */
    void clearMembership();
}
