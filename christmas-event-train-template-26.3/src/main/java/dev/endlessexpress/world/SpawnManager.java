package dev.endlessexpress.world;

import dev.endlessexpress.compat.VersionBridge;
import dev.endlessexpress.config.ModConfig;
import dev.endlessexpress.core.EventClock;
import dev.endlessexpress.core.GamePhase;
import dev.endlessexpress.core.GameState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.concurrent.ThreadLocalRandom;

/** First join: scatter players within a 2,000 x 2,000 block square around Station #0. */
public final class SpawnManager {
    private SpawnManager() {}

    public static void onJoin(ServerPlayer p) {
        MinecraftServer server = VersionBridge.serverOf(p);
        GameState s = GameState.I;
        if (!s.started) startEvent(server);
        VersionBridge.runCommand(server, "team join ee_survivors " + p.getScoreboardName());
        if (!s.spawned.add(p.getUUID())) return;
        ServerLevel ow = VersionBridge.overworld(server);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int rad = ModConfig.I.spawnRadius;
        int cz = 0;
        if (EventClock.phase() == GamePhase.JOURNEY) cz = StationLayout.stationZ(s.stationIndex);
        int x = r.nextInt(-rad, rad + 1), z = cz + r.nextInt(-rad, rad + 1);
        ow.getChunk(x >> 4, z >> 4);
        int y = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        VersionBridge.teleport(p, ow, x + 0.5, y, z + 0.5, 0f, 0f);
        VersionBridge.msg(p, "\u00a7bWelcome to the Endless Blizzard. Find the railway, follow it to Station #0 at (0, 0), board the train, claim your carriage and light the engine within 48 hours.");
        VersionBridge.runCommand(server, "give " + p.getScoreboardName() + " minecraft:compass 1");
    }

    private static void startEvent(MinecraftServer server) {
        GameState s = GameState.I;
        s.started = true;
        s.startMillis = System.currentTimeMillis();
        VersionBridge.runCommand(server, "setworldspawn 0 " + (ModConfig.I.trackY + 1) + " 0");
        VersionBridge.runCommand(server, "time set 0");
        VersionBridge.runCommand(server, "weather rain 1000000");
        // Stop night-skipping by sleeping (both naming schemes; the wrong one just fails harmlessly)
        VersionBridge.runCommand(server, "gamerule playersSleepingPercentage 101");
        VersionBridge.runCommand(server, "gamerule players_sleeping_percentage 101");
        VersionBridge.broadcast(server, "\u00a7b\u00a7lThe Endless Express event has begun!");
    }
}
