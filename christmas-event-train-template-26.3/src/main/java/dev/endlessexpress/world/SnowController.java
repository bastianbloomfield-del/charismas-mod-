package dev.endlessexpress.world;

import dev.endlessexpress.compat.VersionBridge;
import dev.endlessexpress.config.ModConfig;
import dev.endlessexpress.core.GameState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.concurrent.ThreadLocalRandom;

/** Perpetual snow weather, plus snow that stacks layer by layer to the build limit near players. */
public final class SnowController {
    private SnowController() {}

    /** Once per second: keep the storm going. */
    public static void tickSecond(MinecraftServer server) {
        ServerLevel ow = VersionBridge.overworld(server);
        if (!ow.isRaining()) VersionBridge.runCommand(server, "weather rain 1000000");
    }

    /** Every tick: stack snow in random columns around each player. */
    public static void tick(MinecraftServer server) {
        if (!ModConfig.I.snowStacking) return;
        ServerLevel ow = VersionBridge.overworld(server);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int rad = ModConfig.I.snowRadius;
        GameState s = GameState.I;
        for (ServerPlayer p : ow.players()) {
            for (int i = 0; i < ModConfig.I.snowColumnsPerPlayerPerTick; i++) {
                int x = p.getBlockX() + r.nextInt(-rad, rad + 1);
                int z = p.getBlockZ() + r.nextInt(-rad, rad + 1);
                if (StationLayout.inCorridor(x)) continue; // keep the railway and platforms clear
                if (s.bonfireBuilt && Math.abs(x - s.bonfireX) < 14 && Math.abs(z - s.bonfireZ) < 14) continue;
                if (!ow.hasChunk(x >> 4, z >> 4)) continue;
                stack(ow, x, z);
            }
        }
    }

    private static void stack(ServerLevel ow, int x, int z) {
        int y = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z); // first free block above ground
        if (y > ModConfig.I.snowMaxY || y <= ow.getMinY()) return;
        BlockPos pos = new BlockPos(x, y, z);
        BlockPos below = pos.below();
        BlockState b = ow.getBlockState(below);
        if (b.is(Blocks.SNOW)) {
            int layers = b.getValue(SnowLayerBlock.LAYERS);
            if (layers < 8) ow.setBlock(below, b.setValue(SnowLayerBlock.LAYERS, layers + 1), 2);
            else ow.setBlock(below, Blocks.SNOW_BLOCK.defaultBlockState(), 2);
            return;
        }
        BlockState layer = Blocks.SNOW.defaultBlockState();
        if (ow.getBlockState(pos).isAir() && layer.canSurvive(ow, pos)) ow.setBlock(pos, layer, 2);
    }
}
