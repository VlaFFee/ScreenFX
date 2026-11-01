package com.horrorfx.server;

import com.horrorfx.HorrorFX;
import com.horrorfx.fx.FxSavedData;
import com.horrorfx.fx.FxSettings;
import com.horrorfx.fx.FxType;
import com.horrorfx.net.Net;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;

/**
 * /horrorfx start <targets> <effect> [intensity 0..1] [seconds, -1 = until stopped]
 * /horrorfx stop <targets> [effect]
 * /horrorfx clear <targets>        (instant stop of everything)
 * /horrorfx gui
 * /horrorfx list
 */
@Mod.EventBusSubscriber(modid = HorrorFX.MODID)
public class FxCommands {
    private static final SuggestionProvider<CommandSourceStack> FX =
            (c, b) -> SharedSuggestionProvider.suggest(FxType.ids(), b);

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("horrorfx")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("start")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("effect", StringArgumentType.word()).suggests(FX)
                                        .executes(c -> start(c, null, null))
                                        .then(Commands.argument("intensity", FloatArgumentType.floatArg(0f, 1f))
                                                .executes(c -> start(c, FloatArgumentType.getFloat(c, "intensity"), null))
                                                .then(Commands.argument("seconds", FloatArgumentType.floatArg(-1f, 36000f))
                                                        .executes(c -> start(c,
                                                                FloatArgumentType.getFloat(c, "intensity"),
                                                                FloatArgumentType.getFloat(c, "seconds"))))))))
                .then(Commands.literal("stop")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(c -> stop(c, null))
                                .then(Commands.argument("effect", StringArgumentType.word()).suggests(FX)
                                        .executes(c -> stop(c, StringArgumentType.getString(c, "effect"))))))
                .then(Commands.literal("clear")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(FxCommands::clear)))
                .then(Commands.literal("gui").executes(FxCommands::gui))
                .then(Commands.literal("list").executes(FxCommands::list)));
    }

    private static int start(CommandContext<CommandSourceStack> c, Float intensity, Float seconds) throws CommandSyntaxException {
        String id = StringArgumentType.getString(c, "effect");
        FxType type = FxType.byId(id);
        if (type == null) {
            c.getSource().sendFailure(Component.translatable("horrorfx.cmd.unknown", id));
            return 0;
        }
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(c, "targets");
        FxSettings s = FxSavedData.get(c.getSource().getServer()).get(type).copy();
        if (intensity != null) s.intensity = intensity;
        if (seconds != null) s.duration = seconds <= 0 ? -1 : Math.round(seconds * 20f);
        s.clamp();
        for (ServerPlayer p : targets) Net.sendTo(p, new Net.PlayPacket(Net.START, type, s));
        c.getSource().sendSuccess(() -> Component.translatable("horrorfx.cmd.started", type.id, targets.size()), true);
        return targets.size();
    }

    private static int stop(CommandContext<CommandSourceStack> c, String id) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(c, "targets");
        FxType type = null;
        if (id != null && !id.equalsIgnoreCase("all")) {
            type = FxType.byId(id);
            if (type == null) {
                c.getSource().sendFailure(Component.translatable("horrorfx.cmd.unknown", id));
                return 0;
            }
        }
        for (ServerPlayer p : targets) {
            if (type == null) Net.sendTo(p, new Net.PlayPacket(Net.STOP_ALL, null, null));
            else Net.sendTo(p, new Net.PlayPacket(Net.STOP, type, null));
        }
        c.getSource().sendSuccess(() -> Component.translatable("horrorfx.cmd.stopped", targets.size()), true);
        return targets.size();
    }

    private static int clear(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(c, "targets");
        for (ServerPlayer p : targets) Net.sendTo(p, new Net.PlayPacket(Net.CLEAR, null, null));
        c.getSource().sendSuccess(() -> Component.translatable("horrorfx.cmd.stopped", targets.size()), true);
        return targets.size();
    }

    private static int gui(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Net.sendTo(p, new Net.OpenGuiPacket(FxSavedData.get(c.getSource().getServer()).all()));
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> c) {
        c.getSource().sendSuccess(() -> Component.translatable("horrorfx.cmd.list", String.join(", ", FxType.ids())), false);
        return 1;
    }
}
