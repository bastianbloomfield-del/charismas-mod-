package dev.endlessexpress.train;

import dev.endlessexpress.core.GameState;
import dev.endlessexpress.train.carriage.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Builds carriage shells and ground; interiors are delegated to the carriage classes. */
public final class TrainBuilder {
    private TrainBuilder() {}

    public static void set(ServerLevel l, BlockPos p, BlockState s) { l.setBlock(p, s, 2); }

    public static BlockState hangingLantern() {
        return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
    }

    /** Build locomotive, fuel, boiler, dining and greenhouse carriages (first server start). */
    public static void ensureBase(MinecraftServer server) {
        GameState s = GameState.I;
        ServerLevel l = dev.endlessexpress.compat.VersionBridge.trainLevel(server);
        if (l == null) return;
        forceRange(l, TrainLayout.lastCarriage(s.nextPlayerCarriage));
        if (s.trainBuilt) return;
        LocomotiveCarriage.build(l);
        FuelCarriage.build(l);
        BoilerCarriage.build(l);
        DiningCarriage.build(l);
        GreenhouseCarriage.build(l);
        setRearSeal(l, TrainLayout.GREEN, true);
        extendGround(l, 0, TrainLayout.GREEN);
        s.trainBuilt = true;
    }

    /** Append a personal carriage k (>= FIRST_PLAYER) behind the train. */
    public static void appendPlayerCarriage(ServerLevel l, int k) {
        forceRange(l, k);
        setRearSeal(l, k - 1, false);
        PlayerCarriage.build(l, k);
        setRearSeal(l, k, true);
        extendGround(l, k, k);
    }

    public static void forceRange(ServerLevel l, int lastK) {
        for (int cz = -2; cz <= lastK + 2; cz++) {
            l.setChunkForced(-1, cz, true);
            l.setChunkForced(0, cz, true);
        }
    }

    /** Hull shell with doorways (3 wide, 3 tall) at both ends. Front of the locomotive is closed. */
    public static void shell(ServerLevel l, int k) {
        BlockState wall = Blocks.DARK_OAK_PLANKS.defaultBlockState();
        BlockState floor = Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState roof = Blocks.DEEPSLATE_TILES.defaultBlockState();
        BlockState trim = Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState();
        BlockState glass = Blocks.GLASS.defaultBlockState();
        BlockState frame = Blocks.IRON_BLOCK.defaultBlockState();
        BlockState lamp = Blocks.SEA_LANTERN.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int lx = -4; lx <= 4; lx++) for (int ly = 0; ly <= 6; ly++) for (int lz = 0; lz <= 15; lz++) {
            BlockPos p = TrainLayout.pos(k, lx, ly, lz);
            boolean boundary = Math.abs(lx) == 4 || ly == 0 || ly == 6 || lz == 0 || lz == 15;
            if (!boundary) { set(l, p, air); continue; }
            boolean endWall = lz == 0 || lz == 15;
            boolean doorway = endWall && Math.abs(lx) <= 1 && ly >= 1 && ly <= 3 && !(k == 0 && lz == 0);
            if (doorway) { set(l, p, air); continue; }
            boolean doorFrame = endWall && Math.abs(lx) == 2 && ly >= 1 && ly <= 3 && !(k == 0 && lz == 0);
            boolean edge = (Math.abs(lx) == 4 && (ly == 0 || ly == 6 || endWall)) || (ly == 0 && endWall) || (ly == 6 && endWall);
            BlockState st;
            if (doorFrame) st = frame;
            else if (edge) st = trim;
            else if (ly == 0) st = floor;
            else if (ly == 6) st = (Math.abs(lx) == 2 && (lz == 3 || lz == 12)) || (lx == 0 && lz == 8) ? lamp : roof;
            else if (Math.abs(lx) == 4 && ly >= 2 && ly <= 4 && ((lz >= 3 && lz <= 5) || (lz >= 10 && lz <= 12))) st = glass;
            else st = wall;
            set(l, p, st);
        }
    }

    /** Seal or open the rear doorway of carriage k. */
    public static void setRearSeal(ServerLevel l, int k, boolean sealed) {
        BlockState st = sealed ? Blocks.IRON_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState();
        for (int lx = -1; lx <= 1; lx++) for (int ly = 1; ly <= 3; ly++) set(l, TrainLayout.pos(k, lx, ly, 15), st);
    }

    /** Snowy ground plane beneath/around the train so the windows show a landscape. */
    public static void extendGround(ServerLevel l, int fromK, int toK) {
        int z0 = fromK * TrainLayout.LEN - (fromK == 0 ? 24 : 0), z1 = toK * TrainLayout.LEN + TrainLayout.LEN + 24;
        BlockState snow = Blocks.SNOW_BLOCK.defaultBlockState();
        for (int x = -16; x <= 15; x++) for (int z = z0; z <= z1; z++) {
            set(l, new BlockPos(x, TrainLayout.BASE_Y - 3, z), snow);
            if (Math.abs(x) > 5) set(l, new BlockPos(x, TrainLayout.BASE_Y - 2, z), snow);
        }
    }
}
