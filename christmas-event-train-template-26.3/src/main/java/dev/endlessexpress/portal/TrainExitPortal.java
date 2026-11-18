package dev.endlessexpress.portal;

import dev.endlessexpress.compat.VersionBridge;
import dev.endlessexpress.core.GameState;
import dev.endlessexpress.core.TrainState;
import dev.endlessexpress.train.TrainLayout;
import dev.endlessexpress.train.carriage.DiningCarriage;
import dev.endlessexpress.world.StationLayout;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Dining Room exit door -> active station platform (only while the train is stopped). */
public final class TrainExitPortal {
    private TrainExitPortal() {}
    private static final Map<UUID, Integer> NOTICE = new HashMap<>();

    public static void tick(MinecraftServer server) {
        GameState s = GameState.I;
        var tl = VersionBridge.trainLevel(server);
        if (tl == null) return;
        for (ServerPlayer p : tl.players()) {
            if (p.isSpectator() || !DiningCarriage.inExitDoor(p)) continue;
            boolean open = s.trainState == TrainState.STOPPED || s.trainState == TrainState.CRASHED;
            if (open) {
                Vec3 v = StationLayout.platformSpot(s.stationIndex);
                VersionBridge.teleport(p, VersionBridge.overworld(server), v.x, v.y, v.z, 90f, 0f);
            } else {
                // push back inside and explain
                Vec3 in = TrainLayout.diningEntry();
                VersionBridge.teleport(p, tl, 2.5, in.y, TrainLayout.DINING * TrainLayout.LEN + 7.5, 90f, 0f);
                int now = server.getTickCount();
                if (now - NOTICE.getOrDefault(p.getUUID(), -100) > 40) {
                    NOTICE.put(p.getUUID(), now);
                    VersionBridge.msg(p, "The doors are locked while the train is moving.");
                }
            }
        }
    }
}
