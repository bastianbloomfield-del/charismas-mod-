package dev.endlessexpress.finale;

import dev.endlessexpress.compat.VersionBridge;
import dev.endlessexpress.core.GameState;
import dev.endlessexpress.player.GhostAllyManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Nightly waves of winter mobs that attack survivors and the Signal Bonfire. */
public final class WaveManager {
    private WaveManager() {}
    private static final String TAG = "ee_wave";
    private static final String[] TYPES = {"minecraft:husk", "minecraft:zombie", "minecraft:stray", "minecraft:skeleton", "minecraft:zombie"};

    public static boolean isNight() {
        long t = GameState.I.dayTicks % 24000;
        return t >= 13000 && t < 23000;
    }

    public static void tickSecond(MinecraftServer server) {
        GameState s = GameState.I;
        if (!s.crashed || !s.bonfireBuilt || !s.bonfireLit || s.ffa || s.ended || s.victory) return;
        ServerLevel ow = VersionBridge.overworld(server);
        long day = s.dayTicks / 24000;
        if (!isNight()) { clearAll(server); return; }
        if (day != s.lastWaveDay) {
            s.lastWaveDay = day;
            s.waveNumber++;
            VersionBridge.broadcast(server, "\u00a7cNight " + s.waveNumber + ": the winter mobs are coming! Defend the Bonfire!");
        }
        List<Mob> mobs = waveMobs(ow);
        int cap = Math.min(40, 4 + 3 * s.waveNumber);
        if (mobs.size() < cap && s.dayTicks % 200 < 20) {
            ThreadLocalRandom r = ThreadLocalRandom.current();
            int batch = Math.min(cap - mobs.size(), 2 + s.waveNumber / 2);
            for (int i = 0; i < batch; i++) {
                double a = r.nextDouble() * Math.PI * 2, d = 28 + r.nextDouble() * 12;
                int x = (int) (s.bonfireX + Math.cos(a) * d), z = (int) (s.bonfireZ + Math.sin(a) * d);
                ow.getChunk(x >> 4, z >> 4);
                int y = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                String type = TYPES[r.nextInt(TYPES.length)];
                VersionBridge.runCommand(server, "summon " + type + " " + x + " " + y + " " + z
                        + " {Tags:[\"" + TAG + "\"],PersistenceRequired:1b}");
            }
        }
        // Steer: chase the nearest survivor, otherwise march on the Bonfire
        List<ServerPlayer> targets = GhostAllyManager.survivors(server);
        for (Mob m : mobs) {
            ServerPlayer best = null; double bd = 48 * 48;
            for (ServerPlayer p : targets) {
                if (p.level() != m.level()) continue;
                double d = p.distanceToSqr(m);
                if (d < bd) { bd = d; best = p; }
            }
            if (best != null) m.setTarget(best);
            else m.getNavigation().moveTo(s.bonfireX + 0.5, s.bonfireY + 1, s.bonfireZ + 0.5, 1.0);
        }
    }

    public static List<Mob> waveMobs(ServerLevel ow) {
        GameState s = GameState.I;
        AABB box = new AABB(s.bonfireX - 90, ow.getMinY(), s.bonfireZ - 90, s.bonfireX + 90, ow.getMaxY(), s.bonfireZ + 90);
        return ow.getEntitiesOfClass(Mob.class, box, m -> m.getTags().contains(TAG));
    }

    /** Mobs touching the Bonfire per second (used for bonfire damage). */
    public static int attackers(ServerLevel ow) {
        GameState s = GameState.I;
        int n = 0;
        for (Mob m : waveMobs(ow))
            if (Math.abs(m.getX() - s.bonfireX) < 5 && Math.abs(m.getZ() - s.bonfireZ) < 5 && Math.abs(m.getY() - s.bonfireY) < 8) n++;
        return n;
    }

    public static void clearAll(MinecraftServer server) {
        for (Mob m : waveMobs(VersionBridge.overworld(server))) m.discard();
    }
}
