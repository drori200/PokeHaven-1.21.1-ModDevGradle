package net.havencore.pokehaven.guilds;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.havencore.pokehaven.guilds.exceptions.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class GuildSavedData extends SavedData {
    private final EnumMap<GuildName, Guild> guilds = new EnumMap<>(GuildName.class);
    private final ConcurrentHashMap<UUID, GuildName> playerGuildMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> playerFactionMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, GuildName> factionNameToGuildMap = new ConcurrentHashMap<>();

    private final ReentrantLock playerGuildLock = new ReentrantLock();
    private final ReentrantLock playerFactionLock = new ReentrantLock();
    private final ReentrantLock factionNameLock = new ReentrantLock();

    // UUID codec
    public static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    // Faction codec
    public static final Codec<Faction> FACTION_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(Faction::getName),
            UUID_CODEC.fieldOf("leader").forGetter(Faction::getLeader),
            UUID_CODEC.listOf().fieldOf("players").forGetter(Faction::getPlayers)
    ).apply(instance, Faction::new));

    // Guild codec
    public static final Codec<Guild> GUILD_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(GuildName::valueOf, Enum::name).fieldOf("name").forGetter(Guild::getName),
            UUID_CODEC.listOf().fieldOf("players").forGetter(Guild::getPlayers),
            FACTION_CODEC.listOf().fieldOf("factions").forGetter(Guild::getFactions)
    ).apply(instance, (name, players, factions) -> {
        Guild g = new Guild(name);
        players.forEach(g::addPlayer);
        factions.forEach(g::addFaction);
        return g;
    }));

    public static final Codec<Map<GuildName, Guild>> ALL_GUILDS_CODEC = Codec.unboundedMap(
            Codec.STRING.xmap(GuildName::valueOf, Enum::name),
            GUILD_CODEC
    );

    public GuildSavedData() {
        for(GuildName name : GuildName.values()) {
            guilds.put(name, new Guild(name));
        }
    }

    public static GuildSavedData create(){
        return new GuildSavedData();
    }
    public static GuildSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        var result = ALL_GUILDS_CODEC.parse(NbtOps.INSTANCE, tag.get("Guilds"));
        GuildSavedData data = GuildSavedData.create();
        result.resultOrPartial(System.err::println).ifPresent(map -> {
            for (Map.Entry<GuildName, Guild> e : map.entrySet()) {
                data.guilds.put(e.getKey(), e.getValue());
                for (UUID player : e.getValue().getPlayers()) {
                    data.playerGuildMap.put(player, e.getKey());
                }
                for (Faction faction : e.getValue().getFactions()) {
                    String factionKey = faction.getName().toLowerCase(Locale.ROOT);
                    data.factionNameToGuildMap.put(factionKey, e.getKey());
                    for (UUID member : faction.getPlayers()) {
                        data.playerFactionMap.put(member, factionKey);
                    }
                }
            }
        });
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ALL_GUILDS_CODEC.encodeStart(NbtOps.INSTANCE, this.guilds)
                .resultOrPartial(System.err::println)
                .ifPresent(guildsTag -> tag.put("Guilds", guildsTag));
        return tag;
    }

    // === Access ===

    public static GuildSavedData get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        assert overworld != null;
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(GuildSavedData::create, GuildSavedData::load),
                "pokehaven_guilds"
        );
    }

    public Map<GuildName, Guild> getGuilds() {
        return Collections.unmodifiableMap(new EnumMap<>(this.guilds));
    }

    // === Guild Membership ===

    /** Returns the GuildName the player belongs to, or null if none. */
    public GuildName getGuildOfPlayer(UUID player) {
        return playerGuildMap.get(player);
    }

    /** Returns the Guild instance for the player, or empty if none. */
    public Optional<Guild> getGuildForPlayer(UUID player) {
        GuildName name = playerGuildMap.get(player);
        if (name == null) return Optional.empty();
        return Optional.ofNullable(guilds.get(name));
    }

    public void addPlayerToGuild(GuildName guild, UUID player) {
        playerGuildLock.lock();
        try {
            if (playerGuildMap.containsKey(player)) {
                throw new GuildMembershipException("Player already in a guild");
            }
            playerGuildMap.put(player, guild);
            guilds.get(guild).addPlayer(player);
        } finally {
            playerGuildLock.unlock();
        }
    }

    public void addPlayerToGuildSave(GuildName guild, UUID player) {
        addPlayerToGuild(guild, player);
        setDirty();
    }

    public void removePlayerFromGuild(GuildName guild, UUID player) {
        playerGuildLock.lock();
        try {
            if (!playerGuildMap.containsKey(player)) return;
            if (playerGuildMap.get(player) != guild) return;
            playerGuildMap.remove(player);
            guilds.get(guild).removePlayer(player);
        } finally {
            playerGuildLock.unlock();
        }
    }

    public void removePlayerFromGuildSave(GuildName guild, UUID player) {
        removePlayerFromGuild(guild, player);
        setDirty();
    }

    // === Faction Creation ===

    public void createFaction(GuildName guild, String factionName, UUID leader, Collection<UUID> players) {
        factionNameLock.lock();
        try {
            String key = factionName.toLowerCase(Locale.ROOT);
            if (factionNameToGuildMap.containsKey(key)) {
                throw new FactionNamingException("Faction name already exists globally");
            }

            if(!players.contains(leader))
                players.add(leader);
            Faction faction = new Faction(factionName, leader, players);
            Guild g = guilds.get(guild);
            g.addFaction(faction);

            factionNameToGuildMap.put(key, guild);
            for (UUID player : players) {
                playerFactionMap.put(player, key);
            }
        } finally {
            factionNameLock.unlock();
        }
    }

    public void createFaction(GuildName guild, String name, UUID leader) {
        createFaction(guild, name, leader, List.of(leader));
    }

    public void createFactionSave(GuildName guild, String factionName, UUID leader, Collection<UUID> players) {
        createFaction(guild, factionName, leader, players);
        setDirty();
    }

    public void removeFaction(GuildName guild, String factionName) {
        factionNameLock.lock();
        try {
            Guild g = guilds.get(guild);
            Optional<Faction> opt = g.getFactionByName(factionName);
            if (opt.isEmpty()) throw new FactionNotFoundException("Faction does not exist");
            Faction f = opt.get();

            g.removeFaction(f);
            factionNameToGuildMap.remove(f.getName().toLowerCase(Locale.ROOT));
            for (UUID player : f.getPlayers()) {
                playerFactionMap.remove(player);
            }
        } finally {
            factionNameLock.unlock();
        }
    }

    public void removeFactionSave(GuildName guild, String factionName) {
        removeFaction(guild, factionName);
        setDirty();
    }

    // === Faction Membership ===

    public String getFactionOfPlayer(UUID player){
        return playerFactionMap.get(player);
    }
    public void addPlayerToFaction(GuildName guild, String factionName, UUID player) {
        playerFactionLock.lock();
        try {
            Guild g = guilds.get(guild);
            Optional<Faction> of = g.getFactionByName(factionName);
            if (of.isEmpty()) throw new FactionNotFoundException("Faction not found");
            Faction f = of.get();

            if (!g.containsPlayer(player)) {
                throw new FactionMembershipException("Player not in guild");
            }

            f.addPlayer(player);
            playerFactionMap.put(player, f.getName().toLowerCase(Locale.ROOT));
        } finally {
            playerFactionLock.unlock();
        }
    }

    public void addPlayerToFactionSave(GuildName guild, String factionName, UUID player) {
        addPlayerToFaction(guild, factionName, player);
        setDirty();
    }

    public void removePlayerFromFaction(GuildName guild, String factionName, UUID player) {
        playerFactionLock.lock();
        try {
            Guild g = guilds.get(guild);
            Optional<Faction> of = g.getFactionByName(factionName);
            if (of.isEmpty()) throw new FactionNotFoundException("Faction not found");
            Faction f = of.get();

            String key = factionName.toLowerCase(Locale.ROOT);
            if (!key.equals(playerFactionMap.get(player))) {
                throw new FactionMembershipException("Player is not in faction");
            }

            f.removePlayer(player);
            playerFactionMap.remove(player);
        } finally {
            playerFactionLock.unlock();
        }
    }

    public void removePlayerFromFactionSave(GuildName guild, String factionName, UUID player) {
        removePlayerFromFaction(guild, factionName, player);
        setDirty();
    }

    // === Faction Leader ===

    public void changeFactionLeader(GuildName guild, String factionName, UUID newLeader) {
        Guild g = guilds.get(guild);
        Optional<Faction> of = g.getFactionByName(factionName);
        if (of.isEmpty()) throw new FactionNotFoundException("Faction not found");
        Faction f = of.get();
        f.changeLeader(newLeader);
    }

    public void changeFactionLeaderSave(GuildName guild, String factionName, UUID newLeader) {
        changeFactionLeader(guild, factionName, newLeader);
        setDirty();
    }

    // === Faction Rename ===

    public void renameFaction(GuildName guild, String oldName, String newName) {
        factionNameLock.lock();
        try {
            String newKey = newName.toLowerCase(Locale.ROOT);
            if (factionNameToGuildMap.containsKey(newKey)) {
                throw new FactionNamingException("Faction name already exists globally: " + newName);
            }

            Guild g = guilds.get(guild);
            Optional<Faction> of = g.getFactionByName(oldName);
            if (of.isEmpty()) throw new FactionNotFoundException("Faction not found");

            Faction f = of.get();
            g.renameFaction(oldName, newName);

            factionNameToGuildMap.remove(oldName.toLowerCase(Locale.ROOT));
            factionNameToGuildMap.put(newKey, guild);

            for (UUID player : f.getPlayers()) {
                playerFactionMap.put(player, newKey);
            }
        } finally {
            factionNameLock.unlock();
        }
    }

    public void renameFactionSave(GuildName guild, String oldName, String newName) {
        renameFaction(guild, oldName, newName);
        setDirty();
    }
}
