package dev.endlessexpress.player;

import dev.endlessexpress.compat.VersionBridge;
import dev.endlessexpress.config.ModConfig;
import dev.endlessexpress.core.EventClock;
import dev.endlessexpress.core.GamePhase;
import dev.endlessexpress.core.GameState;
import dev.endlessexpress.finale.FfaManager;
import dev.endlessexpress.train.CarriageManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** 6 lives, elimination, and respawning inside the personal carriage. */
public final class LivesManager {
    private LivesManager() {}

    public static int get(ServerPlayer p) { return GameState.I.livesOf(p.getUUID()); }

    public static void set(ServerPlayer p, int n) { GameState.I.lives.put(p.getUUID(), Math.max(0, n)); }

    /** Call after a player's death. */
    public static void onDeath(ServerPlayer p) {
        GameState s = GameState.I;
        if (!s.started || s.ended || GhostAllyManager.isAlly(p)) return;
        if (s.livesOf(p.getUUID()) <= 0) return;
        int left = Math.max(0, s.livesOf(p.getUUID()) - 1);
        s.lives.put(p.getUUID(), left);
        if (left == 0) {
            VersionBridge.broadcast(VersionBridge.serverOf(p), p.getScoreboardName() + " has lost all their lives!");
        }
    }

    /** Call after respawn (new ServerPlayer instance). */
    public static void onRespawn(ServerPlayer p) {
        GameState s = GameState.I;
        if (!s.started || s.ended) return;
        GamePhase ph = EventClock.phase();
        int lives = s.livesOf(p.getUUID());
        var server = VersionBridge.serverOf(p);
        if (GhostAllyManager.isAlly(p)) { GhostAllyManager.makeAlly(p); sendToSite(p); return; }
        if (lives <= 0) {
            if (ph == GamePhase.FINALE && !s.ffa) { GhostAllyManager.makeAlly(p); sendToSite(p); }
            else GhostAllyManager.makeGhost(p);
            FfaManager.checkWinner(server);
            return;
        }
        if (ph == GamePhase.FINALE || s.crashed) { sendToSite(p); return; }
        Integer k = s.carriages.get(p.getUUID());
        if (k != null) {
            ServerLevel tl = VersionBridge.trainLevel(server);
            Vec3 v = dev.endlessexpress.train.TrainLayout.playerSpawn(k);
            VersionBridge.teleport(p, tl, v.x, v.y, v.z, 0f, 0f);
        }
    }

    private static void sendToSite(ServerPlayer p) {
        dev.endlessexpress.finale.CrashEvent.teleportToSite(p);
    }

    public static String hearts(ServerPlayer p) {
        int l = get(p), max = ModConfig.I.startingLives;
        return "\u00a7c" + "\u2764".repeat(Math.max(0, Math.min(l, max))) + "\u00a78" + "\u2764".repeat(Math.max(0, max - l));
    }
}
