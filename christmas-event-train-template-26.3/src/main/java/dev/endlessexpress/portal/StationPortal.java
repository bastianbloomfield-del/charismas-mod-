package dev.endlessexpress.portal;

import dev.endlessexpress.compat.VersionBridge;
import dev.endlessexpress.core.GameState;
import dev.endlessexpress.core.TrainState;
import dev.endlessexpress.player.GhostAllyManager;
import dev.endlessexpress.train.CarriageManager;
import dev.endlessexpress.train.TrainLayout;
import dev.endlessexpress.world.StationLayout;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Overworld boarding archway -> Dining Room entrance in the Train Dimension. */
public final class StationPortal {
    private StationPortal() {}
    private static final Map<UUID, Integer> NOTICE = new HashMap<>();

    public static void tick(MinecraftServer server) {
        GameState s = GameState.I;
        for (ServerPlayer p : VersionBridge.overworld(server).players()) {
            if (p.isSpectator()) continue;
            int i = StationLayout.nearestStation(p.getBlockZ());
            if (!StationLayout.inArchway(i, p.getBlockX(), p.getBlockY(), p.getBlockZ())) continue;
            boolean here = i == s.stationIndex;
            boolean open = s.trainState == TrainState.STOPPED || s.trainState == TrainState.UNLIT || s.trainState == TrainState.CRASHED;
            if (!here || !open) {
                int now = server.getTickCount();
                if (now - NOTICE.getOrDefault(p.getUUID(), -100) > 60) {
                    NOTICE.put(p.getUUID(), now);
                    VersionBridge.msg(p, here ? "The train is moving - you missed it!" : "The train is not at this station.");
                }
                continue;
            }
            if (!GhostAllyManager.isSurvivor(p) && s.trainState != TrainState.CRASHED) continue;
            board(p);
        }
    }

    public static void board(ServerPlayer p) {
        GameState s = GameState.I;
        if (s.carriages.get(p.getUUID()) == null && CarriageManager.claim(p) < 0) return;
        Vec3 v = TrainLayout.diningEntry();
        VersionBridge.teleport(p, VersionBridge.trainLevel(VersionBridge.serverOf(p)), v.x, v.y, v.z, 0f, 0f);
    }
}
