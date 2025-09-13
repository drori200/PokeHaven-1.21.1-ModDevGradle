package net.havencore.pokehaven.guilds;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import net.havencore.pokehaven.guilds.exceptions.*;

public class Guild {
    private final GuildName name;
    private final List<UUID> players = new CopyOnWriteArrayList<>();
    private final List<Faction> factions = new CopyOnWriteArrayList<>();

    public Guild(GuildName name) {
        this.name = Objects.requireNonNull(name, "Guild name cannot be null");
    }

    public GuildName getName() {
        return name;
    }

    public List<UUID> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public List<Faction> getFactions() {
        return Collections.unmodifiableList(factions);
    }

    public void addPlayer(UUID player) {
        if (!players.contains(player)) {
            players.add(player);
        }
    }

    public void removePlayer(UUID player) {
        players.remove(player);
    }

    public boolean containsPlayer(UUID player) {
        return players.contains(player);
    }

    public Optional<Faction> getFactionByName(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        return factions.stream()
                .filter(f -> f.getName().equalsIgnoreCase(key))
                .findFirst();
    }

    public void addFaction(Faction faction) {
        if (getFactionByName(faction.getName()).isPresent()) {
            throw new FactionNamingException("Faction already exists in this guild: " + faction.getName());
        }
        for (UUID player : faction.getPlayers()) {
            if (!players.contains(player)) {
                throw new FactionMembershipException("Player not in guild: " + player);
            }
        }
        if (!players.contains(faction.getLeader())) {
            throw new FactionLeadershipException("Leader not in guild: " + faction.getLeader());
        }
        factions.add(faction);
    }

    public void removeFaction(Faction faction) {
        factions.remove(faction);
    }

    public void renameFaction(String oldName, String newName) {
        Optional<Faction> opt = getFactionByName(oldName);
        if (opt.isEmpty()) {
            throw new FactionNotFoundException("Faction not found: " + oldName);
        }
        Faction faction = opt.get();
        faction.rename(newName); // This will validate uniqueness globally
    }
}
