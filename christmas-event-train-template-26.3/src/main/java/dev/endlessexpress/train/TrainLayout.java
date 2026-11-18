package dev.endlessexpress.train;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Geometry of the train in endlessexpress:train_void.
 * Carriage k occupies z = k*16 .. k*16+15, x = -4..4, y = 100..106.
 * Local coords: lx -4..4, ly 0..6 (0 = floor, 6 = roof), lz 0..15.
 */
public final class TrainLayout {
    private TrainLayout() {}

    public static final int BASE_Y = 100, LEN = 16;
    public static final int LOCO = 0, FUEL = 1, BOILER = 2, DINING = 3, GREEN = 4, FIRST_PLAYER = 5;

    public static BlockPos pos(int k, int lx, int ly, int lz) {
        return new BlockPos(lx, BASE_Y + ly, k * LEN + lz);
    }

    /** Carriage index containing p, or -1. */
    public static int carriageAt(BlockPos p) {
        if (p.getY() < BASE_Y || p.getY() > BASE_Y + 6 || Math.abs(p.getX()) > 4 || p.getZ() < 0) return -1;
        return p.getZ() / LEN;
    }

    public static Vec3 center(int k) { return new Vec3(0.5, BASE_Y + 1, k * LEN + 8.5); }
    public static Vec3 diningEntry() { return new Vec3(0.5, BASE_Y + 1, DINING * LEN + 4.5); }
    public static Vec3 playerSpawn(int k) { return new Vec3(0.5, BASE_Y + 1, k * LEN + 8.5); }
    public static int lastCarriage(int claimed) { return FIRST_PLAYER + claimed - 1; }
}
