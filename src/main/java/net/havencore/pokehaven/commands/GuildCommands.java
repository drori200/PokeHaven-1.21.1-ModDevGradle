package net.havencore.pokehaven.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.havencore.pokehaven.capabilities.PlayerDataAccess;
import net.havencore.pokehaven.capabilities.PlayerGuildData;
import net.havencore.pokehaven.guilds.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.UUID;
import java.util.function.Consumer;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@EventBusSubscriber(modid = "pokehaven")
public class GuildCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        event.getDispatcher().register(
                literal("guild")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("Guild commands: join, leave, faction"), false);
                            return 1;
                        })

                        // guild join <guildName> [uuid]
                        .then(literal("join")
                                .then(argument("guildName", StringArgumentType.word())
                                        .suggests((ctx, sb) -> {
                                            for (GuildName g : GuildName.values()) {
                                                sb.suggest(g.name().toLowerCase());
                                            }
                                            return sb.buildFuture();
                                        })
                                        .executes(ctx -> executeGuildJoin(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "guildName"),
                                                null
                                        ))
                                        .then(argument("uuid", UuidArgument.uuid())
                                                .executes(ctx -> {
                                                    UUID u = UuidArgument.getUuid(ctx, "uuid");
                                                    return executeGuildJoin(
                                                            ctx.getSource(),
                                                            StringArgumentType.getString(ctx, "guildName"),
                                                            u
                                                    );
                                                })
                                        )
                                )
                        )

                        // guild leave [uuid]
                        .then(literal("leave")
                                .executes(ctx -> executeGuildLeave(ctx.getSource(), null))
                                .then(argument("uuid", UuidArgument.uuid())
                                        .executes(ctx -> executeGuildLeave(
                                                ctx.getSource(),
                                                UuidArgument.getUuid(ctx, "uuid")
                                        ))
                                )
                        )

                        // guild faction …
                        .then(literal("faction")
                                .executes(ctx -> {
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal("Faction commands: create, join, leave, list"), false);
                                    return 1;
                                })
                                .then(literal("create")
                                        .then(argument("factionName", StringArgumentType.word())
                                                .executes(ctx -> executeFactionCreate(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "factionName")
                                                ))
                                        ))
                                // guild faction join <factionName> [uuid]
                                .then(literal("join")
                                        .then(argument("factionName", StringArgumentType.word())
                                                .executes(ctx -> executeFactionJoin(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "factionName"),
                                                        null
                                                ))
                                                .then(argument("uuid", UuidArgument.uuid())
                                                        .executes(ctx -> executeFactionJoin(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "factionName"),
                                                                UuidArgument.getUuid(ctx, "uuid")
                                                        ))
                                                )
                                        )
                                )

                                // guild faction leave [uuid]
                                .then(literal("leave")
                                        .executes(ctx -> executeFactionLeave(ctx.getSource(), null))
                                        .then(argument("uuid", UuidArgument.uuid())
                                                .executes(ctx -> executeFactionLeave(
                                                        ctx.getSource(),
                                                        UuidArgument.getUuid(ctx, "uuid")
                                                ))
                                        )
                                )

                                // guild faction list
                                .then(literal("list")
                                        .executes(ctx -> executeFactionList(ctx.getSource()))
                                )
                        )
        );
    }

    // Executor methods (similar to before)...
    private static int executeGuildJoin(CommandSourceStack src, String guildNameStr, UUID uuidArg) {
        try {
            UUID target = (uuidArg != null) ? uuidArg : src.getPlayerOrException().getUUID();
            GuildName guildName = GuildName.valueOf(guildNameStr.toUpperCase());
            GuildSavedData data = GuildSavedData.get(src.getServer());
            data.addPlayerToGuildSave(guildName, target);
            src.sendSuccess(() -> Component.literal("Joined guild " + guildName), true);
            updatePlayerData(src, target, playerData -> {
                playerData.setGuildName(guildName.name());
                playerData.setFactionName(null);
                playerData.setFactionLeader(false);
            });
            return 1;
        } catch (Exception e) {
            src.sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeGuildLeave(CommandSourceStack src, UUID uuidArg) {
        try {
            UUID target = (uuidArg != null) ? uuidArg : src.getPlayerOrException().getUUID();
            GuildSavedData data = GuildSavedData.get(src.getServer());
            GuildName current = data.getGuildOfPlayer(target);
            data.removePlayerFromGuildSave(current, target);
            src.sendSuccess(() -> Component.literal("Left guild " + current), true);
            updatePlayerData(src, target, PlayerGuildData::clearGuildMembership);
            return 1;
        } catch (Exception e) {
            src.sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeFactionCreate(CommandSourceStack src, String factionName){
        try {
            UUID leader = src.getPlayerOrException().getUUID();
            GuildSavedData data = GuildSavedData.get(src.getServer());
            GuildName guild = data.getGuildOfPlayer(leader);
            data.createFaction(guild, factionName, leader);
            data.setDirty();
            src.sendSuccess(() -> Component.literal("Created faction " + factionName), true);
            updatePlayerData(src, leader, playerData -> {
                playerData.setFactionName(factionName);
                playerData.setFactionLeader(true);
            });
            return 1;
        } catch (Exception e) {
            src.sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    private static int executeFactionJoin(CommandSourceStack src, String factionName, UUID uuidArg) {
        try {
            UUID target = (uuidArg != null) ? uuidArg : src.getPlayerOrException().getUUID();
            GuildSavedData data = GuildSavedData.get(src.getServer());
            GuildName guild = data.getGuildOfPlayer(target);
            data.addPlayerToFactionSave(guild, factionName, target);
            src.sendSuccess(() -> Component.literal("Joined faction " + factionName), true);
            updatePlayerData(src, target, playerData -> {
                playerData.setFactionName(factionName);
                playerData.setFactionLeader(false);
            });
            return 1;
        } catch (Exception e) {
            src.sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeFactionLeave(CommandSourceStack src, UUID uuidArg) {
        try {
            UUID target = (uuidArg != null) ? uuidArg : src.getPlayerOrException().getUUID();
            GuildSavedData data = GuildSavedData.get(src.getServer());
            GuildName guild = data.getGuildOfPlayer(target);
            String faction = data.getFactionOfPlayer(target);
            data.removePlayerFromFactionSave(guild, faction, target);
            src.sendSuccess(() -> Component.literal("Left faction " + faction), true);
            updatePlayerData(src, target, playerData -> {
                playerData.setFactionName(null);
                playerData.setFactionLeader(false);
            });
            return 1;
        } catch (Exception e) {
            src.sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeFactionList(CommandSourceStack src) {
        try {
            UUID player = src.getPlayerOrException().getUUID();
            GuildSavedData data = GuildSavedData.get(src.getServer());
            GuildName guildName = data.getGuildOfPlayer(player);
            Guild guild = data.getGuilds().get(guildName);
            String list = String.join(", ",
                    guild.getFactions().stream().map(Faction::getName).toList());
            src.sendSuccess(() -> Component.literal("Factions: " + list), false);
            return 1;
        } catch (Exception e) {
            src.sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static void updatePlayerData(CommandSourceStack src, UUID target, Consumer<PlayerGuildData> update) {
        var player = src.getServer().getPlayerList().getPlayer(target);
        if (player == null) {
            return;
        }
        PlayerDataAccess.get(player).ifPresent(update);
    }
}
