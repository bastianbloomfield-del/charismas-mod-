package dev.endlessexpress.finale;

import dev.endlessexpress.compat.VersionBridge;
import dev.endlessexpress.config.ModConfig;
import dev.endlessexpress.core.GameState;
import dev.endlessexpress.core.TrainState;
import dev.endlessexpress.player.GhostAllyManager;
import dev.endlessexpress.world.StationLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

/** Week 3: the train derails permanently at the final station and the Bonfire defence begins. */
public final class CrashEvent {
    private CrashEvent() {}

    private static final BlockState[] DEBRIS = {
        Blocks.DARK_OAK_PLANKS.defaultBlockState(), Blocks.SPRUCE_PLANKS.defaultBlockState(),
        Blocks.IRON_BLOCK.defaultBlockState(), Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(),
        Blocks.DEEPSLATE_TILES.defaultBlockState(), Blocks.GLASS.defaultBlockState()
    };

    public static void trigger(MinecraftServer server) {
        GameState s = GameState.I;
        if (s.crashed) return;
        s.crashed = true;
        s.trainState = TrainState.CRASHED;
        s.brakeActive = false;
        ServerLevel ow = VersionBridge.overworld(server);
        int sz = StationLayout.stationZ(s.stationIndex);

        scatterWreckage(ow, sz);
        Bonfire.build(server, -16, sz);

        VersionBridge.broadcast(server, "\u00a74\u00a7lCRASH! The train has derailed at Station " + s.stationIndex
                + ". The carriages are torn open - salvage everything and build the Signal Bonfire!");
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (s.livesOf(p.getUUID()) <= 0 && !s.allies.contains(p.getUUID())) GhostAllyManager.makeAlly(p);
            teleportToSite(p);
            VersionBridge.title(p, "THE CRASH", "Build and defend the Bonfire");
        }
    }

    public static void teleportToSite(ServerPlayer p) {
        GameState s = GameState.I;
        Vec3 v = StationLayout.platformSpot(s.stationIndex);
        ServerLevel ow = VersionBridge.overworld(VersionBridge.serverOf(p));
        VersionBridge.teleport(p, ow, v.x, v.y, v.z, 90f, 0f);
    }

    /** Piles of salvageable train blocks scattered west of the track. */
    private static void scatterWreckage(ServerLevel ow, int sz) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < 50; i++) {
            int cx = -8 - r.nextInt(30), cz = sz + r.nextInt(-24, 25);
            ow.getChunk(cx >> 4, cz >> 4);
            int base = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz);
            int size = 1 + r.nextInt(3);
            for (int dx = -size; dx <= size; dx++) for (int dz = -size; dz <= size; dz++) {
                int h = r.nextInt(3);
                for (int dy = 0; dy <= h; dy++)
                    if (r.nextInt(100) < 70) ow.setBlock(new BlockPos(cx + dx, base + dy, cz + dz), DEBRIS[r.nextInt(DEBRIS.length)], 2);
            }
        }
    }
}
