package dev.endlessexpress.finale;

import dev.endlessexpress.compat.VersionBridge;
import dev.endlessexpress.core.GameState;
import dev.endlessexpress.player.GhostAllyManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** Failed defence: free-for-all, last player standing wins. */
public final class FfaManager {
    private FfaManager() {}

    public static void begin(MinecraftServer server) {
        GameState s = GameState.I;
        if (s.ffa || s.ended) return;
        s.ffa = true;
        VersionBridge.broadcast(server, "\u00a74\u00a7lThe Bonfire has died. Santa never arrives. FREE-FOR-ALL - last player standing wins!");
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (GhostAllyManager.isAlly(p)) { GhostAllyManager.removeAlly(p); s.lives.put(p.getUUID(), 0); GhostAllyManager.makeGhost(p); continue; }
            if (GhostAllyManager.isSurvivor(p)) { s.lives.put(p.getUUID(), 1); VersionBridge.title(p, "FREE FOR ALL", "Last one standing wins"); }
        }
        WaveManager.clearAll(server);
    }

    public static void checkWinner(MinecraftServer server) {
        GameState s = GameState.I;
        if (!s.ffa || s.ended) return;
        List<ServerPlayer> alive = GhostAllyManager.survivors(server);
        if (alive.size() <= 1) {
            s.ended = true;
            if (alive.size() == 1) {
                ServerPlayer w = alive.get(0);
                VersionBridge.broadcast(server, "\u00a76\u00a7l" + w.getScoreboardName() + " is the last player standing and WINS!");
                VersionBridge.title(w, "VICTORY", "Last player standing");
            } else VersionBridge.broadcast(server, "Nobody survived the winter.");
        }
    }

    public static void tickSecond(MinecraftServer server) { checkWinner(server); }
}
