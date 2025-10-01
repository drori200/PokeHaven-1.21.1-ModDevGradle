package net.havencore.pokehaven.gametest;

import net.havencore.pokehaven.guilds.Faction;
import net.havencore.pokehaven.guilds.Guild;
import net.havencore.pokehaven.guilds.GuildName;
import net.havencore.pokehaven.guilds.GuildSavedData;
import net.havencore.pokehaven.guilds.exceptions.FactionLeadershipException;
import net.havencore.pokehaven.guilds.exceptions.FactionMembershipException;
import net.havencore.pokehaven.guilds.exceptions.FactionNamingException;
import net.havencore.pokehaven.guilds.exceptions.FactionNotFoundException;
import net.havencore.pokehaven.guilds.exceptions.GuildMembershipException;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@GameTestHolder("pokehaven")
@PrefixGameTestTemplate(value = false)
public class GuildSystemGameTests {

    @GameTest(template = "flatworld")
    public static void guildDataInitializesAllGuilds(GameTestHelper helper) {
        GuildSavedData data = GuildSavedData.create();
        Map<GuildName, Guild> guildMap = data.getGuilds();

        helper.assertTrue(guildMap.size() == GuildName.values().length,
                "Expected all canonical guilds to be pre-initialized");

        for (GuildName name : GuildName.values()) {
            Guild guild = guildMap.get(name);
            helper.assertTrue(guild != null, "Missing guild entry for " + name);
            helper.assertTrue(guild.getPlayers().isEmpty(), "Guild " + name + " should start empty");
            helper.assertTrue(guild.getFactions().isEmpty(), "Guild " + name + " should have no factions");
        }

        helper.succeed();
    }

    @GameTest(template = "flatworld")
    public static void guildMembershipBulkOperations(GameTestHelper helper) {
        GuildSavedData data = GuildSavedData.create();
        List<UUID> players = IntStream.range(0, 200)
                .mapToObj(i -> new UUID(0L, i))
                .toList();

        players.forEach(p -> data.addPlayerToGuild(GuildName.ROCKET, p));

        helper.assertTrue(
                data.getGuilds().get(GuildName.ROCKET).getPlayers().size() == players.size(),
                "Bulk guild join should register every player"
        );

        boolean duplicateThrown = false;
        try {
            data.addPlayerToGuild(GuildName.ROCKET, players.get(0));
        } catch (GuildMembershipException e) {
            duplicateThrown = true;
        }
        helper.assertTrue(duplicateThrown, "Joining a second time should throw GuildMembershipException");

        players.stream().limit(100).forEach(p -> data.removePlayerFromGuild(GuildName.ROCKET, p));

        helper.assertTrue(
                data.getGuilds().get(GuildName.ROCKET).getPlayers().size() == players.size() - 100,
                "After removals exactly 100 players should remain"
        );

        players.stream().limit(100).forEach(p ->
                helper.assertTrue(data.getGuildOfPlayer(p) == null, "Removed player still tracked in guild map: " + p)
        );

        helper.succeed();
    }

    @GameTest(template = "flatworld")
    public static void factionLifecycleLargeRoster(GameTestHelper helper) {
        GuildSavedData data = GuildSavedData.create();
        List<UUID> players = IntStream.range(0, 150)
                .mapToObj(i -> new UUID(1L, i))
                .collect(Collectors.toList());
        players.forEach(p -> data.addPlayerToGuild(GuildName.MAGMA, p));

        List<UUID> factionMembers = new ArrayList<>(players);
        data.createFaction(GuildName.MAGMA, "Inferno", players.get(0), factionMembers);

        Guild guild = data.getGuilds().get(GuildName.MAGMA);
        helper.assertTrue(guild.getFactions().size() == 1, "Faction should be registered on the guild");

        players.stream().skip(50).limit(50).forEach(p -> data.removePlayerFromFaction(GuildName.MAGMA, "Inferno", p));

        players.stream().skip(50).limit(50).forEach(p ->
                helper.assertTrue(data.getFactionOfPlayer(p) == null, "Removed faction member still tracked: " + p)
        );

        data.removeFaction(GuildName.MAGMA, "Inferno");
        helper.assertTrue(guild.getFactions().isEmpty(), "Faction removal should leave guild with zero factions");
        players.forEach(p ->
                helper.assertTrue(data.getFactionOfPlayer(p) == null, "Faction removal should clear player mapping: " + p)
        );

        helper.succeed();
    }

    @GameTest(template = "flatworld")
    public static void factionCreationRejectsForeignMembers(GameTestHelper helper) {
        GuildSavedData data = GuildSavedData.create();
        UUID leader = new UUID(2L, 1L);
        UUID outsider = new UUID(2L, 2L);
        data.addPlayerToGuild(GuildName.AQUA, leader);

        List<UUID> roster = new ArrayList<>();
        roster.add(leader);
        roster.add(outsider);

        boolean thrown = false;
        try {
            data.createFaction(GuildName.AQUA, "Coral", leader, roster);
        } catch (FactionMembershipException e) {
            thrown = true;
        }
        helper.assertTrue(thrown, "Faction creation should reject players outside the guild");
        helper.assertTrue(data.getGuilds().get(GuildName.AQUA).getFactions().isEmpty(),
                "Failed faction creation must not leak into guild state");
        helper.assertTrue(data.getFactionOfPlayer(leader) == null, "Leader should not gain a faction on failure");
        helper.assertTrue(data.getFactionOfPlayer(outsider) == null, "Outsider should remain factionless");

        helper.succeed();
    }

    @GameTest(template = "flatworld")
    public static void factionRenameReleasesOldName(GameTestHelper helper) {
        GuildSavedData data = GuildSavedData.create();
        UUID leader = new UUID(3L, 1L);
        UUID member = new UUID(3L, 2L);
        data.addPlayerToGuild(GuildName.GALACTIC, leader);
        data.addPlayerToGuild(GuildName.GALACTIC, member);

        List<UUID> roster = new ArrayList<>();
        roster.add(leader);
        roster.add(member);
        data.createFaction(GuildName.GALACTIC, "Tide", leader, roster);

        data.renameFaction(GuildName.GALACTIC, "Tide", "Surge");
        helper.assertTrue("surge".equals(data.getFactionOfPlayer(leader)), "Leader mapping should track renamed faction");
        helper.assertTrue("surge".equals(data.getFactionOfPlayer(member)), "Member mapping should track renamed faction");

        UUID newLeader = new UUID(3L, 3L);
        data.addPlayerToGuild(GuildName.GALACTIC, newLeader);
        data.createFaction(GuildName.GALACTIC, "Tide", newLeader);
        helper.assertTrue(data.getGuilds().get(GuildName.GALACTIC).getFactions().size() == 2,
                "Renamed faction should free the old name for reuse");

        helper.succeed();
    }

    @GameTest(template = "flatworld")
    public static void factionLeaderChangeValidation(GameTestHelper helper) {
        GuildSavedData data = GuildSavedData.create();
        UUID leader = new UUID(4L, 1L);
        UUID member = new UUID(4L, 2L);
        UUID outsider = new UUID(4L, 3L);
        data.addPlayerToGuild(GuildName.FLARE, leader);
        data.addPlayerToGuild(GuildName.FLARE, member);
        data.addPlayerToGuild(GuildName.FLARE, outsider);

        List<UUID> roster = new ArrayList<>();
        roster.add(leader);
        roster.add(member);
        data.createFaction(GuildName.FLARE, "Blaze", leader, roster);

        boolean thrown = false;
        try {
            data.changeFactionLeader(GuildName.FLARE, "Blaze", outsider);
        } catch (FactionLeadershipException e) {
            thrown = true;
        }
        helper.assertTrue(thrown, "Leader change should fail when the candidate is not a faction member");

        data.addPlayerToFaction(GuildName.FLARE, "Blaze", outsider);
        data.changeFactionLeader(GuildName.FLARE, "Blaze", outsider);
        Optional<Faction> faction = data.getGuilds().get(GuildName.FLARE).getFactionByName("Blaze");
        helper.assertTrue(faction.orElseThrow().getLeader().equals(outsider),
                "Leader change should succeed once the player joins the faction");

        helper.succeed();
    }

    @GameTest(template = "flatworld")
    public static void factionJoinRequiresGuildMembership(GameTestHelper helper) {
        GuildSavedData data = GuildSavedData.create();
        UUID leader = new UUID(5L, 1L);
        UUID intruder = new UUID(5L, 2L);
        data.addPlayerToGuild(GuildName.SKULL, leader);
        data.createFaction(GuildName.SKULL, "Bones", leader);

        boolean thrown = false;
        try {
            data.addPlayerToFaction(GuildName.SKULL, "Bones", intruder);
        } catch (FactionMembershipException e) {
            thrown = true;
        }
        helper.assertTrue(thrown, "Joining a faction should require guild membership");
        helper.assertTrue(data.getFactionOfPlayer(intruder) == null, "Intruder must remain factionless");

        helper.succeed();
    }

    @GameTest(template = "flatworld")
    public static void factionNamesAreGloballyUnique(GameTestHelper helper) {
        GuildSavedData data = GuildSavedData.create();
        UUID solar = new UUID(6L, 1L);
        UUID lunar = new UUID(6L, 2L);
        data.addPlayerToGuild(GuildName.YELLOW, solar);
        data.addPlayerToGuild(GuildName.STAR, lunar);

        data.createFaction(GuildName.YELLOW, "Shine", solar);

        boolean thrown = false;
        try {
            data.createFaction(GuildName.STAR, "shine", lunar);
        } catch (FactionNamingException e) {
            thrown = true;
        }
        helper.assertTrue(thrown, "Faction names must be unique regardless of case");

        helper.succeed();
    }

    @GameTest(template = "flatworld")
    public static void factionLeaderCannotBeRemoved(GameTestHelper helper) {
        GuildSavedData data = GuildSavedData.create();
        UUID leader = new UUID(7L, 1L);
        UUID member = new UUID(7L, 2L);
        data.addPlayerToGuild(GuildName.STAR, leader);
        data.addPlayerToGuild(GuildName.STAR, member);
        List<UUID> roster = new ArrayList<>();
        roster.add(leader);
        roster.add(member);
        data.createFaction(GuildName.STAR, "Comet", leader, roster);

        boolean thrown = false;
        try {
            data.removePlayerFromFaction(GuildName.STAR, "Comet", leader);
        } catch (FactionLeadershipException e) {
            thrown = true;
        }
        helper.assertTrue(thrown, "Removing the faction leader should throw an exception");
        helper.assertTrue("comet".equals(data.getFactionOfPlayer(leader)),
                "Leader must remain mapped to the faction after failed removal");

        helper.succeed();
    }

    @GameTest(template = "flatworld")
    public static void massFactionMembershipChurn(GameTestHelper helper) {
        GuildSavedData data = GuildSavedData.create();
        List<UUID> players = IntStream.range(0, 160)
                .mapToObj(i -> new UUID(8L, i))
                .collect(Collectors.toList());
        players.forEach(p -> data.addPlayerToGuild(GuildName.ROCKET, p));

        List<UUID> initialMembers = new ArrayList<>(players.subList(0, 80));
        data.createFaction(GuildName.ROCKET, "Nebula", players.get(0), initialMembers);

        players.subList(80, players.size()).forEach(p -> data.addPlayerToFaction(GuildName.ROCKET, "Nebula", p));
        helper.assertTrue(
                data.getGuilds().get(GuildName.ROCKET).getFactionByName("Nebula").orElseThrow().getPlayers().size() == players.size(),
                "Faction should contain every guild member after bulk additions"
        );

        players.subList(1, 121).forEach(p -> data.removePlayerFromFaction(GuildName.ROCKET, "Nebula", p));

        players.subList(1, 121).forEach(p ->
                helper.assertTrue(data.getFactionOfPlayer(p) == null, "Removed player still mapped to faction: " + p)
        );
        helper.assertTrue(
                data.getGuilds().get(GuildName.ROCKET).getFactionByName("Nebula").orElseThrow().getPlayers().size() == players.size() - 120,
                "Faction should retain exactly the unremoved members"
        );
        helper.assertTrue("nebula".equals(data.getFactionOfPlayer(players.get(0))),
                "Leader should still be associated with the faction");

        helper.succeed();
    }

    @GameTest(template = "flatworld")
    public static void removePlayerNotInFactionFails(GameTestHelper helper) {
        GuildSavedData data = GuildSavedData.create();
        UUID leader = new UUID(9L, 1L);
        UUID member = new UUID(9L, 2L);
        UUID stranger = new UUID(9L, 3L);
        data.addPlayerToGuild(GuildName.MAGMA, leader);
        data.addPlayerToGuild(GuildName.MAGMA, member);
        data.addPlayerToGuild(GuildName.MAGMA, stranger);
        data.createFaction(GuildName.MAGMA, "Crimson", leader);
        data.addPlayerToFaction(GuildName.MAGMA, "Crimson", member);

        boolean thrown = false;
        try {
            data.removePlayerFromFaction(GuildName.MAGMA, "Crimson", stranger);
        } catch (FactionMembershipException e) {
            thrown = true;
        }
        helper.assertTrue(thrown, "Removing a non-member should throw a FactionMembershipException");
        helper.assertTrue(data.getFactionOfPlayer(stranger) == null,
                "Stranger should never be mapped to the faction");

        helper.succeed();
    }

    @GameTest(template = "flatworld")
    public static void removeNonexistentFactionFailsGracefully(GameTestHelper helper) {
        GuildSavedData data = GuildSavedData.create();
        UUID leader = new UUID(10L, 1L);
        data.addPlayerToGuild(GuildName.AQUA, leader);
        data.createFaction(GuildName.AQUA, "Delta", leader);

        boolean thrown = false;
        try {
            data.removeFaction(GuildName.AQUA, "Echo");
        } catch (FactionNotFoundException e) {
            thrown = true;
        }
        helper.assertTrue(thrown, "Removing an unknown faction should throw FactionNotFoundException");

        helper.succeed();
    }
}
