package dev.endlessexpress.core;

import dev.endlessexpress.compat.VersionBridge;
import dev.endlessexpress.config.ModConfig;
import dev.endlessexpress.finale.CrashEvent;
import dev.endlessexpress.player.GhostAllyManager;
import dev.endlessexpress.train.TrainStatus;
import dev.endlessexpress.train.carriage.DiningCarriage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** 10 min MOVING / 40 min STOPPED loop, departure warning, arrival, breakdown. */
public final class TrainSchedule {
    private TrainSchedule() {}

    public static void tick(MinecraftServer server) {
        GameState s = GameState.I;
        if (!s.started || s.ended) return;
        GamePhase ph = EventClock.phase();

        if (ph == GamePhase.FINALE && !s.crashed) { CrashEvent.trigger(server); return; }
        if (ph != GamePhase.JOURNEY || s.crashed) return;

        if (!s.journeyStarted) { // Week 1 begins: auto-ignite and depart in 5 minutes
            s.journeyStarted = true;
            s.engineLit = true;
            s.trainState = TrainState.STOPPED;
            s.stateTicks = ModConfig.I.warningAtStoppedTicks - 1;
            VersionBridge.broadcast(server, "The journey begins! The train departs in 5 minutes. All aboard!");
        }

        s.stateTicks++;
        switch (s.trainState) {
            case STOPPED -> {
                if (s.stateTicks == ModConfig.I.warningAtStoppedTicks) warn(server);
                if (s.stateTicks >= ModConfig.I.stoppedTicks) depart(server);
            }
            case MOVING -> { if (s.stateTicks >= ModConfig.I.movingTicks) arrive(server); }
            default -> {}
        }
    }

    private static void warn(MinecraftServer server) {
        GameState.I.hornSounded = false;
        VersionBridge.broadcast(server, "\u00a76\u00a7lThe train departs in 5 minutes! Return through the station entrance!");
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            VersionBridge.levelOf(p).playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BELL_BLOCK, SoundSource.RECORDS, 4f, 1f);
            VersionBridge.title(p, "Departing in 5:00", "Board the train now");
        }
    }

    public static void depart(MinecraftServer server) {
        GameState s = GameState.I;
        s.trainState = TrainState.MOVING;
        s.stateTicks = 0;
        s.secCounter = 0;
        VersionBridge.broadcast(server, "The train departs for Station " + (s.stationIndex + 1) + "!");
        DiningCarriage.startFeast(server);
        for (ServerPlayer p : GhostAllyManager.survivors(server))
            if (VersionBridge.inOverworld(p)) {
                VersionBridge.msg(p, "\u00a7cYou were left behind! The blizzard freezes you until the train reaches the next station.");
                VersionBridge.title(p, "STRANDED", "Freezing until the next stop");
            }
    }

    public static void arrive(MinecraftServer server) {
        GameState s = GameState.I;
        TrainStatus.onArrive();
        if (s.breakdownPending) {
            s.breakdownPending = false;
            TrainStatus.emergencyRepairs();
            VersionBridge.broadcast(server, "The train limps back into Station " + s.stationIndex + ". Breakdown - a stop was lost to emergency repairs.");
        } else {
            s.stationIndex = Math.min(s.stationIndex + 1, ModConfig.I.maxStations);
            VersionBridge.broadcast(server, "The train arrives at Station " + s.stationIndex + ". Doors are open for 40 minutes.");
        }
        s.trainState = TrainState.STOPPED;
        s.stateTicks = 0;
    }

    /** Boiler + Fuel + Engine all at 0% during transit. */
    public static void breakdown(MinecraftServer server) {
        GameState s = GameState.I;
        if (s.breakdownPending) return;
        s.breakdownPending = true;
        VersionBridge.broadcast(server, "BREAKDOWN! The train is failing and will lose a full station stop.");
        s.stateTicks = ModConfig.I.movingTicks; // ends the transit immediately
    }

    /** Players left outside while the train moves freeze until the next stop. */
    public static void strandedTick(MinecraftServer server) {
        if (GameState.I.trainState != TrainState.MOVING) return;
        for (ServerPlayer p : GhostAllyManager.survivors(server))
            if (VersionBridge.inOverworld(p)) VersionBridge.freezeDamage(p, 1f);
    }

    public static String clockText() {
        GameState s = GameState.I;
        int total = switch (s.trainState) {
            case MOVING -> ModConfig.I.movingTicks;
            case STOPPED -> ModConfig.I.stoppedTicks;
            default -> 0;
        };
        int left = Math.max(0, total - s.stateTicks) / 20;
        return String.format("%02d:%02d", left / 60, left % 60);
    }
}
