package dev.endlessexpress.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.endlessexpress.compat.VersionBridge;
import dev.endlessexpress.config.ModConfig;
import dev.endlessexpress.core.EventClock;
import dev.endlessexpress.core.GameState;
import dev.endlessexpress.core.TrainSchedule;
import dev.endlessexpress.finale.Bonfire;
import dev.endlessexpress.player.LivesManager;
import dev.endlessexpress.player.TeamManager;
import dev.endlessexpress.portal.StationPortal;
import dev.endlessexpress.world.StationLayout;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Admin commands. NOTE: `hasPermission(2)` may need to become
 * `Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)` on newer Minecraft versions.
 */
public final class EeCommands {
    private EeCommands() {}

    private static void say(CommandSourceStack src, String text) { src.sendSuccess(() -> Component.literal(text), false); }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("ee").requires(src -> src.hasPermission(2))
            .then(Commands.literal("status").executes(c -> {
                GameState s = GameState.I;
                say(c.getSource(), "phase=" + EventClock.phase() + " train=" + s.trainState + " station=" + s.stationIndex
                        + " clock=" + TrainSchedule.clockText() + " engine=" + (int) s.engine + " boiler=" + (int) s.boiler
                        + " pipe=" + (int) s.pipe + " fuel=" + s.fuel + " water=" + s.water + " lava=" + s.lava
                        + " bonfire=" + (int) s.bonfire);
                return 1;
            }))
            .then(Commands.literal("start").executes(c -> {
                GameState s = GameState.I; s.started = true; s.startMillis = System.currentTimeMillis();
                say(c.getSource(), "Event started."); return 1;
            }))
            .then(Commands.literal("phase").then(Commands.argument("name", StringArgumentType.word()).executes(c -> {
                String n = StringArgumentType.getString(c, "name");
                GameState.I.forcedPhase = n.equalsIgnoreCase("auto") ? "" : n;
                say(c.getSource(), "Phase set to " + (n.equalsIgnoreCase("auto") ? "automatic" : n.toUpperCase()) + " (prologue|journey|finale|auto)");
                return 1;
            })))
            .then(Commands.literal("depart").executes(c -> {
                TrainSchedule.depart(c.getSource().getServer()); return 1;
            }))
            .then(Commands.literal("arrive").executes(c -> {
                TrainSchedule.arrive(c.getSource().getServer()); return 1;
            }))
            .then(Commands.literal("teams").executes(c -> {
                TeamManager.assign(c.getSource().getServer()); say(c.getSource(), "Teams re-rolled."); return 1;
            }))
            .then(Commands.literal("lives")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("n", IntegerArgumentType.integer(0, 99)).executes(c -> {
                        ServerPlayer p = EntityArgument.getPlayer(c, "player");
                        LivesManager.set(p, IntegerArgumentType.getInteger(c, "n"));
                        say(c.getSource(), p.getScoreboardName() + " now has " + LivesManager.get(p) + " lives."); return 1;
                    }))))
            .then(Commands.literal("bonfire").then(Commands.argument("percent", IntegerArgumentType.integer(0, 100)).executes(c -> {
                GameState.I.bonfire = IntegerArgumentType.getInteger(c, "percent");
                if (GameState.I.bonfire >= 5) GameState.I.bonfireLit = true;
                if (GameState.I.bonfireBuilt) Bonfire.visual(VersionBridge.overworld(c.getSource().getServer()));
                say(c.getSource(), "Bonfire set."); return 1;
            })))
            .then(Commands.literal("station").then(Commands.argument("index", IntegerArgumentType.integer(0, ModConfig.I.maxStations)).executes(c -> {
                int i = IntegerArgumentType.getInteger(c, "index");
                ServerPlayer p = c.getSource().getPlayerOrException();
                Vec3 v = StationLayout.platformSpot(i);
                VersionBridge.teleport(p, VersionBridge.overworld(c.getSource().getServer()), v.x, v.y, v.z, 90f, 0f);
                return 1;
            })))
            .then(Commands.literal("board").executes(c -> { StationPortal.board(c.getSource().getPlayerOrException()); return 1; }))
        );
    }
}
