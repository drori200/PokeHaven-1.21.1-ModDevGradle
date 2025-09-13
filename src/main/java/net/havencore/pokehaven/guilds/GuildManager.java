package net.havencore.pokehaven.guilds;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class GuildManager {

    // ⬇️ Mutable data defined in record header
    public record Guild(String name, List<UUID> players, List<Faction> factions) {

        public Guild {
            // `name` is never changed at runtime; it must be one of the fixed nine.
            if (!GuildConstants.ALL_GUILD_NAMES.contains(name)) {
                throw new IllegalArgumentException("Invalid guild name: " + name);
            }
        }

        @Override
        public @NotNull String toString() {
            return "Guild{name='%s', playerCount=%d, factionCount=%d}".formatted(name, players.size(), factions.size());
        }

        //Check if a player is a faction leader or part of a faction in general.
        public boolean isPlayerInAnyFaction(UUID playerUUID) {
            if (factions == null || factions.isEmpty()) {
                return false;
            }

            return factions.stream().anyMatch(faction ->
                    faction.factionLeader().equals(playerUUID) ||
                            faction.factionPlayers().contains(playerUUID)
            );
        }


        //Overloaded method
        public void createFaction(String factionName, UUID factionLeader){
            createFaction(factionName, factionLeader, new ArrayList<>());
        }
        public void createFaction(String factionName, UUID factionLeader, List<UUID> factionPlayers){
            if(!players().contains(factionLeader)) {
                throw new GuildMembershipException("Player must be part of this guild!");
            }

            if(isPlayerInAnyFaction(factionLeader)){
                throw new RuntimeException("Player is already a leader of a faction or part of one!");
            }

            addFaction(new Faction(this, factionName, factionLeader, factionPlayers));
        }
        public void addFaction(Faction faction) {
            if (factions().contains(faction)) return;
            factions().add(faction);
        }

        public void removeFaction(Faction faction) {
            factions().remove(faction);
        }
    }

    public record Faction(Guild guild, String factionName, UUID factionLeader, List<UUID> factionPlayers) {

        public Faction {
            // Ensure the leader is added to the player list if not already present
            if (!factionPlayers().contains(factionLeader)) {
                factionPlayers().add(factionLeader);
            }
        }

        @Override
        public @NotNull String toString() {
            return "Faction{name='%s', leader=%s, players=%d}".formatted(factionName, factionLeader, factionPlayers.size());
        }

        public boolean hasPlayer(UUID playerUUID){
            return factionPlayers().contains(playerUUID);
        }

        public void addPlayer(UUID playerUUID) {
            if (!hasPlayer(playerUUID))
                factionPlayers().add(playerUUID);
        }

        public void removePlayer(UUID playerUUID) {
            if(hasPlayer(playerUUID))
                factionPlayers().remove(playerUUID);
        }

        public UUID getPlayer(UUID playerUUID) {
            return factionPlayers().stream().filter(p -> p.equals(playerUUID)).findAny().orElse(null);
        }
    }

    public static class GuildMembershipException extends RuntimeException {
        public GuildMembershipException(String message){
            super(message);
        }
    }
}
