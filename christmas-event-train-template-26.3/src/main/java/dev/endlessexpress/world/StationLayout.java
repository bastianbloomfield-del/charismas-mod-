package dev.endlessexpress.world;

import dev.endlessexpress.config.ModConfig;
import net.minecraft.world.phys.Vec3;

/** Station coordinates: station i is at Z = -i * 2500. Track at x=0, platform x=2..10, arch at x=9. */
public final class StationLayout {
    private StationLayout() {}

    public static final int ARCH_X = 9;

    public static int stationZ(int i) { return -i * ModConfig.I.stationSpacing; }

    public static int nearestStation(int z) {
        int i = Math.round(-z / (float) ModConfig.I.stationSpacing);
        return Math.max(0, Math.min(ModConfig.I.maxStations, i));
    }

    public static boolean inCorridor(int x) { return x >= -3 && x <= 11; }

    /** Where players appear when leaving the train at station i. */
    public static Vec3 platformSpot(int i) { return new Vec3(7.5, ModConfig.I.trackY, stationZ(i) + 0.5); }

    /** True if (x,y,z) is inside the boarding archway of station i. */
    public static boolean inArchway(int i, int x, int y, int z) {
        int T = ModConfig.I.trackY;
        return x == ARCH_X && Math.abs(z - stationZ(i)) <= 1 && y >= T && y <= T + 3;
    }
}
