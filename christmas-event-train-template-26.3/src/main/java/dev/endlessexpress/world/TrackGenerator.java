package dev.endlessexpress.world;

import dev.endlessexpress.config.ModConfig;
import dev.endlessexpress.core.GameState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Lazily builds the railway and stations as chunks load (pre-generating ~900,000 blocks of track
 * at world creation is impractical, and the result is identical because it is deterministic).
 * The track runs at a constant elevation (config trackY): valleys get embankments/bridges,
 * hills get cuts/tunnels.
 */
public final class TrackGenerator {
    private TrackGenerator() {}

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState BRICK = Blocks.STONE_BRICKS.defaultBlockState();
    private static final BlockState GRAVEL = Blocks.GRAVEL.defaultBlockState();
    private static final BlockState LANTERN_ARCH = Blocks.SEA_LANTERN.defaultBlockState();

    public static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        if (!level.dimension().equals(Level.OVERWORLD)) return;
        int minX = chunk.getPos().getMinBlockX(), minZ = chunk.getPos().getMinBlockZ();
        if (minX > 11 || minX + 15 < -3) return;
        int zTop = 1200, zBottom = -(ModConfig.I.maxStations * ModConfig.I.stationSpacing) - 100;
        if (minZ > zTop || minZ + 15 < zBottom) return;
        long key = (((long) (minX >> 4)) << 32) ^ (minZ >> 4) & 0xffffffffL;
        if (!GameState.I.doneChunks.add(key)) return;

        for (int x = Math.max(minX, -3); x <= Math.min(minX + 15, 11); x++)
            for (int z = minZ; z <= minZ + 15; z++) {
                if (z > zTop || z < zBottom) continue;
                int h = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
                int ground = h - 1;
                if (Math.abs(x) <= 3) trackColumn(level, x, z, ground);
                int st = StationLayout.nearestStation(z);
                if (x >= 2 && x <= 10 && Math.abs(z - StationLayout.stationZ(st)) <= 12) stationColumn(level, x, z, st, ground);
            }
    }

    private static void set(ServerLevel l, int x, int y, int z, BlockState s) { l.setBlock(new BlockPos(x, y, z), s, 2); }

    private static void trackColumn(ServerLevel l, int x, int z, int ground) {
        int T = ModConfig.I.trackY, ax = Math.abs(x);
        if (ground < T - 5) {
            // Bridge: deck, railings, pillars every 8 blocks
            if (ax <= 1) set(l, x, T - 1, z, BRICK);
            if (ax == 2) set(l, x, T, z, Blocks.STONE_BRICK_WALL.defaultBlockState());
            if (ax == 1 && Math.floorMod(z, 8) == 0) for (int y = ground + 1; y < T - 1; y++) set(l, x, y, z, BRICK);
            for (int y = T + 1; y <= T + 6; y++) if (ax <= 2 && !l.getBlockState(new BlockPos(x, y, z)).isAir()) set(l, x, y, z, AIR);
        } else if (ground < T - 1) {
            // Embankment
            if (ax <= 2) for (int y = ground + 1; y <= T - 2; y++) set(l, x, y, z, Blocks.DIRT.defaultBlockState());
            if (ax <= 1) set(l, x, T - 1, z, GRAVEL);
            for (int y = T; y <= T + 6; y++) if (ax <= 2 && !l.getBlockState(new BlockPos(x, y, z)).isAir()) set(l, x, y, z, AIR);
        } else if (ground >= T + 3) {
            // Tunnel: bricks all around, 5 wide x 5 tall interior
            for (int y = T - 1; y <= T + 5; y++) {
                boolean shell = ax == 3 || y == T + 5 || y == T - 1;
                BlockState st = shell ? ((ax == 3 && y == T + 2 && Math.floorMod(z, 8) == 0) ? LANTERN_ARCH : (y == T - 1 && ax <= 1 ? GRAVEL : BRICK)) : AIR;
                set(l, x, y, z, st);
            }
        } else {
            // Open cut at roughly track level
            if (ax <= 1) set(l, x, T - 1, z, GRAVEL);
            for (int y = T; y <= T + 6; y++) if (ax <= 2) set(l, x, y, z, AIR);
        }
        if (x == 0) set(l, 0, T, z, Blocks.RAIL.defaultBlockState().setValue(BlockStateProperties.RAIL_SHAPE, RailShape.NORTH_SOUTH));
    }

    private static void stationColumn(ServerLevel l, int x, int z, int st, int ground) {
        int T = ModConfig.I.trackY, sz = StationLayout.stationZ(st);
        // support + floor
        for (int y = Math.max(ground + 1, T - 12); y < T - 1; y++) set(l, x, y, z, BRICK);
        set(l, x, T - 1, z, BRICK);
        for (int y = T; y <= T + 8; y++) set(l, x, y, z, AIR);
        int dz = z - sz;
        // canopy roof over x 3..7
        if (x >= 3 && x <= 7 && Math.abs(dz) <= 8) set(l, x, T + 5, z, Blocks.STONE_BRICK_SLAB.defaultBlockState());
        // canopy posts + hanging lanterns
        if (x == 7 && (dz == -8 || dz == 0 || dz == 8)) for (int y = T; y <= T + 4; y++) set(l, x, y, z, Blocks.STONE_BRICK_WALL.defaultBlockState());
        if (x == 5 && (dz == -4 || dz == 4 || dz == 0)) set(l, x, T + 4, z, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        // glowing boarding archway at x = 9
        if (x == StationLayout.ARCH_X) {
            if (Math.abs(dz) == 2) for (int y = T; y <= T + 3; y++) set(l, x, y, z, LANTERN_ARCH);
            if (Math.abs(dz) <= 2) set(l, x, T + 4, z, LANTERN_ARCH);
        }
    }
}
