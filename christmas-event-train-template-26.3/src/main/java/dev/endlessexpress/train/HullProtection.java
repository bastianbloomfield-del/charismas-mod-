package dev.endlessexpress.train;

import dev.endlessexpress.core.EventClock;
import dev.endlessexpress.core.GameState;
import net.minecraft.core.BlockPos;

/** Outer walls, floors, roofs and couplings are unbreakable until the Week 3 crash. */
public final class HullProtection {
    private HullProtection() {}

    public static boolean active() {
        return EventClock.beforeFinale() && !GameState.I.crashed;
    }

    public static boolean isHull(BlockPos p) {
        int k = TrainLayout.carriageAt(p);
        if (k < 0) return false;
        int lz = p.getZ() - k * TrainLayout.LEN;
        int ly = p.getY() - TrainLayout.BASE_Y;
        int ax = Math.abs(p.getX());
        return ax == 4 || ly == 0 || ly == 6 || lz == 0 || lz == TrainLayout.LEN - 1;
    }
}
