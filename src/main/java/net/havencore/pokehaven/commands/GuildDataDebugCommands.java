package net.havencore.pokehaven.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.havencore.pokehaven.PokeHaven;
import net.havencore.pokehaven.capabilities.PlayerDataAccess;
import net.havencore.pokehaven.capabilities.PlayerGuildData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = PokeHaven.MODID)
public final class GuildDataDebugCommands {
    private static final SimpleCommandExceptionType MISSING_CAPABILITY =
            new SimpleCommandExceptionType(Component.literal("Guild data capability is unavailable for this player."));

    private GuildDataDebugCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("guilddata")
                .requires(source -> source.hasPermission(2))
                .executes(GuildDataDebugCommands::show)
                .then(Commands.literal("show").executes(GuildDataDebugCommands::show))
                .then(Commands.literal("set")
                        .then(Commands.literal("guild")
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(GuildDataDebugCommands::setGuild)))
                        .then(Commands.literal("faction")
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(GuildDataDebugCommands::setFaction)))
                        .then(Commands.literal("leader")
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(GuildDataDebugCommands::setLeader))))
                .then(Commands.literal("clear")
                        .executes(GuildDataDebugCommands::clearAll)
                        .then(Commands.literal("guild").executes(GuildDataDebugCommands::clearGuild))
                        .then(Commands.literal("faction").executes(GuildDataDebugCommands::clearFaction))));
    }

    private static int show(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerGuildData data = requireData(player);
        String guild = data.getGuildName().orElse("<none>");
        String faction = data.getFactionName().orElse("<none>");
        boolean leader = data.isFactionLeader();
        context.getSource().sendSuccess(
                () -> Component.literal("Guild: " + guild + ", Faction: " + faction + ", Leader: " + leader),
                false);
        return 1;
    }

    private static int setGuild(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerGuildData data = requireData(player);
        String guild = StringArgumentType.getString(context, "name");
        data.setGuildName(guild);
        data.setFactionName(null);
        data.setFactionLeader(false);
        context.getSource().sendSuccess(() -> Component.literal("Stored guild " + guild + " for " + player.getName().getString()), false);
        return 1;
    }

    private static int setFaction(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerGuildData data = requireData(player);
        String faction = StringArgumentType.getString(context, "name");
        data.setFactionName(faction);
        context.getSource().sendSuccess(() -> Component.literal("Stored faction " + faction + " for " + player.getName().getString()), false);
        return 1;
    }

    private static int setLeader(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerGuildData data = requireData(player);
        boolean leader = BoolArgumentType.getBool(context, "value");
        data.setFactionLeader(leader);
        context.getSource().sendSuccess(() -> Component.literal("Leader flag set to " + leader + " for " + player.getName().getString()), false);
        return 1;
    }

    private static int clearAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerGuildData data = requireData(player);
        data.clearGuildMembership();
        context.getSource().sendSuccess(() -> Component.literal("Cleared all guild data for " + player.getName().getString()), false);
        return 1;
    }

    private static int clearGuild(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerGuildData data = requireData(player);
        data.setGuildName(null);
        data.setFactionName(null);
        data.setFactionLeader(false);
        context.getSource().sendSuccess(() -> Component.literal("Cleared guild entry for " + player.getName().getString()), false);
        return 1;
    }

    private static int clearFaction(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerGuildData data = requireData(player);
        data.setFactionName(null);
        data.setFactionLeader(false);
        context.getSource().sendSuccess(() -> Component.literal("Cleared faction entry for " + player.getName().getString()), false);
        return 1;
    }

    private static PlayerGuildData requireData(ServerPlayer player) throws CommandSyntaxException {
        return PlayerDataAccess.get(player).orElseThrow(MISSING_CAPABILITY::create);
    }
}

