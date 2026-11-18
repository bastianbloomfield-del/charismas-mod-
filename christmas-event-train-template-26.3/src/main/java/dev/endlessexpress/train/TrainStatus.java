package dev.endlessexpress.train;

import dev.endlessexpress.compat.VersionBridge;
import dev.endlessexpress.core.GameState;
import dev.endlessexpress.core.TrainSchedule;
import dev.endlessexpress.core.TrainState;
import dev.endlessexpress.player.GhostAllyManager;
import dev.endlessexpress.train.carriage.BoilerCarriage;
import dev.endlessexpress.train.carriage.FuelCarriage;
import dev.endlessexpress.train.carriage.GreenhouseCarriage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.concurrent.ThreadLocalRandom;

/** Engine / boiler / pipe health, pressure, random hazards and breakdown. Runs once per second. */
public final class TrainStatus {
    private TrainStatus() {}

    private static double clamp(double v) { return Math.max(0, Math.min(100, v)); }

    public static void tickSecond(MinecraftServer server) {
        GameState s = GameState.I;
        if (!s.engineLit || s.trainState == TrainState.CRASHED || s.trainState == TrainState.UNLIT) return;
        ServerLevel tl = VersionBridge.trainLevel(server);
        if (tl == null) return;
        ThreadLocalRandom r = ThreadLocalRandom.current();

        if (s.trainState == TrainState.MOVING) {
            s.secCounter++;
            int sec = s.secCounter;
            if (sec % 75 == 0) addIce(tl, r);
            if (sec % 150 == 0) addLeak(tl, r);
            if (sec % 180 == 0) lampOut(tl, r);
            if (sec % 100 == 0 && !s.brakeActive) startBrake(server);

            // pressure random walk; a jammed valve climbs fast
            s.pressure += (r.nextDouble() - 0.5) * 6 + (s.jammed ? 4 : 0);
            s.pressure = clamp(s.pressure);
            s.ash = clamp(s.ash + 0.7);

            double boilerDrain = 0.12 + (s.ash > 70 ? 0.3 : 0) + 0.25 * s.iceVents.size()
                    + (s.jammed ? 0.3 : 0) + ((s.pressure > 90 || s.pressure < 20) ? 0.35 : 0) + (s.water <= 0 ? 0.3 : 0);
            s.boiler = clamp(s.boiler - boilerDrain);
            s.engine = clamp(s.engine - 0.20 - (s.fuel <= 0 ? 0.3 : 0));
            s.pipe = clamp(s.pipe - 0.05 - 0.5 * s.leaks.size());
            if (sec % 5 == 0) s.fuel = Math.max(0, s.fuel - 1);
            if (sec % 6 == 0) s.water = Math.max(0, s.water - 1);
            s.fuel = Math.max(0, s.fuel - s.leaks.size());

            if (s.brakeActive && --s.brakeLeft <= 0) {
                s.brakeActive = false;
                s.engine = clamp(s.engine - 12);
                VersionBridge.broadcast(server, "The brakes were ignored! Engine damaged.");
            }
            if (s.engine <= 0 && s.boiler <= 0 && s.pipe <= 0) TrainSchedule.breakdown(server);
        }

        // Greenhouse water freezes when the boiler runs cold
        if (s.boiler < 25 && !s.greenFrozen) GreenhouseCarriage.setChannel(tl, true);
        else if (s.boiler >= 40 && s.greenFrozen) GreenhouseCarriage.setChannel(tl, false);

        // Neglected ice or a dead boiler: freezing damage inside the carriages every 3 s
        if ((s.iceVents.size() >= 3 || s.boiler < 15) && s.secCounter % 3 == 0) {
            for (ServerPlayer p : GhostAllyManager.survivors(server))
                if (VersionBridge.inTrain(p)) VersionBridge.freezeDamage(p, 1f);
        }
    }

    private static void addIce(ServerLevel tl, ThreadLocalRandom r) {
        GameState s = GameState.I;
        int i = r.nextInt(BoilerCarriage.VENTS);
        if (s.iceVents.contains(i)) return;
        s.iceVents.add(i);
        BoilerCarriage.setVent(tl, i, true);
    }

    private static void addLeak(ServerLevel tl, ThreadLocalRandom r) {
        GameState s = GameState.I;
        int i = r.nextInt(FuelCarriage.PIPES);
        if (s.leaks.contains(i)) return;
        s.leaks.add(i);
        FuelCarriage.setPipe(tl, i, true);
    }

    private static void lampOut(ServerLevel tl, ThreadLocalRandom r) {
        GameState s = GameState.I;
        int i = r.nextInt(GreenhouseCarriage.LAMPS);
        if (s.lampsOut.contains(i)) return;
        s.lampsOut.add(i);
        GreenhouseCarriage.setLamp(tl, i, false);
    }

    private static void startBrake(MinecraftServer server) {
        GameState s = GameState.I;
        s.brakeActive = true;
        s.brakeLeft = 25;
        ServerLevel tl = VersionBridge.trainLevel(server);
        VersionBridge.broadcast(server, "BRAKE ALARM! Someone pull the brake lever in the Locomotive!");
        for (ServerPlayer p : tl.players())
            tl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2f, 0.8f);
    }

    /** Reset hazards when the train reaches a station. */
    public static void onArrive() {
        GameState s = GameState.I;
        s.brakeActive = false;
        s.secCounter = 0;
    }

    /** After a breakdown, emergency repairs restore a little health. */
    public static void emergencyRepairs() {
        GameState s = GameState.I;
        s.engine = Math.max(s.engine, 30);
        s.boiler = Math.max(s.boiler, 30);
        s.pipe = Math.max(s.pipe, 30);
        s.fuel = Math.max(s.fuel, 100);
        s.water = Math.max(s.water, 100);
    }
}
