package dev.endlessexpress.train;

import dev.endlessexpress.compat.VersionBridge;
import dev.endlessexpress.config.ModConfig;
import dev.endlessexpress.core.GameState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Assigns every player a personal numbered 9x7x16 carriage on the rear of the train. */
public final class CarriageManager {
    private CarriageManager() {}

    /** Claim (or look up) the player's carriage index. Returns -1 if the train is full. */
    public static int claim(ServerPlayer p) {
        GameState s = GameState.I;
        Integer existing = s.carriages.get(p.getUUID());
        if (existing != null) return existing;
        if (s.nextPlayerCarriage >= ModConfig.I.maxPlayerCarriages) {
            VersionBridge.msg(p, "The train has no free carriages left!");
            return -1;
        }
        int k = TrainLayout.FIRST_PLAYER + s.nextPlayerCarriage++;
        ServerLevel tl = VersionBridge.trainLevel(VersionBridge.serverOf(p));
        TrainBuilder.appendPlayerCarriage(tl, k);
        s.carriages.put(p.getUUID(), k);
        VersionBridge.msg(p, "You have claimed Carriage #" + (k - TrainLayout.FIRST_PLAYER + 1)
                + " at the rear of the train. Decorate it however you like!");
        return k;
    }

    /** Personal carriages are owner-only for building until the Week 3 crash. */
    public static boolean canModify(ServerPlayer p, net.minecraft.core.BlockPos pos) {
        int k = TrainLayout.carriageAt(pos);
        if (k < TrainLayout.FIRST_PLAYER) return true;
        for (var e : GameState.I.carriages.entrySet())
            if (e.getValue() == k) return e.getKey().equals(p.getUUID());
        return true;
    }

    public static void teleportHome(ServerPlayer p) {
        Integer k = GameState.I.carriages.get(p.getUUID());
        if (k == null) return;
        Vec3 v = TrainLayout.playerSpawn(k);
        VersionBridge.teleport(p, VersionBridge.trainLevel(VersionBridge.serverOf(p)), v.x, v.y, v.z, 0f, 0f);
    }
}
