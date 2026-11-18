package dev.endlessexpress.world;

import dev.endlessexpress.compat.VersionBridge;
import dev.endlessexpress.core.GameState;
import dev.endlessexpress.core.TrainState;
import dev.endlessexpress.train.TrainLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The train never moves. While MOVING, fence-post "telegraph poles" scroll past the windows on both
 * sides, snow streaks by, and train audio plays, creating the illusion of speed.
 */
public final class FakeScenery {
    private FakeScenery() {}

    private static final int SPACING = 9;
    private static int offset = 0;
    private static final BlockState POST = Blocks.SPRUCE_FENCE.defaultBlockState();
    private static final BlockState TOP = Blocks.LANTERN.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    public static void tick(MinecraftServer server) {
        GameState s = GameState.I;
        if (s.trainState != TrainState.MOVING) return;
        ServerLevel tl = VersionBridge.trainLevel(server);
        if (tl == null || tl.players().isEmpty()) return;
        int t = server.getTickCount();
        int length = TrainLayout.LEN * (TrainLayout.FIRST_PLAYER + s.nextPlayerCarriage) + 48;

        if (t % 2 == 0) {
            int prev = offset;
            offset = (offset + 1) % (length);
            for (int side : new int[]{-8, 8}) {
                for (int i = 0; i * SPACING < length; i++) {
                    int zOld = ((prev + i * SPACING) % length) - 24;
                    int zNew = ((offset + i * SPACING) % length) - 24;
                    column(tl, side, zOld, AIR, AIR);
                    column(tl, side, zNew, POST, TOP);
                }
            }
        }
        if (t % 10 == 0) {
            for (var p : tl.players()) {
                tl.sendParticles(ParticleTypes.SNOWFLAKE, p.getX() + (p.getX() < 0 ? -7 : 7), p.getY() + 1, p.getZ(), 12, 0.5, 2, 4, 0.4);
            }
        }
        if (t % 40 == 0) {
            for (var p : tl.players())
                tl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.MINECART_INSIDE, SoundSource.AMBIENT, 0.4f, 0.7f);
        }
    }

    private static void column(ServerLevel tl, int x, int z, BlockState body, BlockState top) {
        BlockPos base = new BlockPos(x, TrainLayout.BASE_Y - 1, z);
        if (!VersionBridge.loaded(tl, base)) return;
        for (int y = 0; y < 4; y++) tl.setBlock(base.above(y), body, 2);
        tl.setBlock(base.above(4), top, 2);
    }
}
