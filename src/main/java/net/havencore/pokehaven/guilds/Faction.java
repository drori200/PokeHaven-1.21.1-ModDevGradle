package net.havencore.pokehaven.guilds;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import net.havencore.pokehaven.guilds.exceptions.*;

public class Faction {
    private String name;
    private UUID leader;
    private final List<UUID> players = new CopyOnWriteArrayList<>();

    public Faction(String name, UUID leader, Collection<UUID> initialPlayers) {
        this.setName(name);
        this.setLeader(leader);
        this.players.addAll(initialPlayers);

        if (!players.contains(leader)) {
            throw new FactionLeadershipException("Leader must be a member of the faction");
        }
    }

    public String getName() {
        return name;
    }

    public UUID getLeader() {
        return leader;
    }

    public List<UUID> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public void addPlayer(UUID player) {
        if (!players.contains(player)) {
            players.add(player);
        }
    }

    public void removePlayer(UUID player) {
        if (player.equals(leader)) {
            throw new FactionLeadershipException("Cannot remove the leader from the faction");
        }
        players.remove(player);
    }

    public void forceRemovePlayer(UUID player) {
        players.remove(player);
    }

    public void changeLeader(UUID newLeader) {
        if (!players.contains(newLeader)) {
            throw new FactionLeadershipException("New leader must be a member of the faction");
        }
        if(leader.equals(newLeader)) {
            throw new FactionLeadershipException("New leader UUID is the same as current leader UUID");
        }
        this.leader = newLeader;
    }

    public void rename(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new FactionNamingException("Faction name cannot be null or blank");
        }
        this.name = newName;
    }

    private void setLeader(UUID leader) {
        if (leader == null) {
            throw new FactionLeadershipException("Leader cannot be null");
        }
        this.leader = leader;
    }

    private void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new FactionNamingException("Faction name cannot be null or blank");
        }
        this.name = name;
    }
}
