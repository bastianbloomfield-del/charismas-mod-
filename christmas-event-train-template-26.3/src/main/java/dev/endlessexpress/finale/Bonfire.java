package dev.endlessexpress.finale;

import dev.endlessexpress.compat.VersionBridge;
import dev.endlessexpress.core.GameState;
import dev.endlessexpress.player.GhostAllyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;

/** The Signal Bonfire: feed it wood, coal and rare items to reach 100%; waves drain it. */
public final class Bonfire {
    private Bonfire() {}
    private static final int MAX_HEIGHT = 6;

    public static boolean near(BlockPos p) {
        GameState s = GameState.I;
        return s.bonfireBuilt && Math.abs(p.getX() - s.bonfireX) <= 3 && Math.abs(p.getZ() - s.bonfireZ) <= 3
                && p.getY() >= s.bonfireY && p.getY() <= s.bonfireY + 10;
    }

    public static boolean isFeedPos(BlockPos p) {
        GameState s = GameState.I;
        return s.bonfireBuilt && Math.abs(p.getX() - s.bonfireX) <= 1 && Math.abs(p.getZ() - s.bonfireZ) <= 1
                && p.getY() >= s.bonfireY && p.getY() <= s.bonfireY + MAX_HEIGHT + 2;
    }

    public static void build(MinecraftServer server, int x, int z) {
        GameState s = GameState.I;
        ServerLevel ow = VersionBridge.overworld(server);
        ow.getChunk(x >> 4, z >> 4);
        int y = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        s.bonfireX = x; s.bonfireY = y; s.bonfireZ = z; s.bonfireBuilt = true;
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
            ow.setBlock(new BlockPos(x + dx, y - 1, z + dz), Blocks.STONE_BRICKS.defaultBlockState(), 2);
            for (int dy = 0; dy < 10; dy++) ow.setBlock(new BlockPos(x + dx, y + dy, z + dz), Blocks.AIR.defaultBlockState(), 2);
        }
        visual(ow);
    }

    /** Value of one item as Bonfire progress (percent). */
    public static double value(ItemStack st) {
        if (st.isEmpty()) return 0;
        if (st.is(Items.NETHERITE_INGOT)) return 15;
        if (st.is(Items.TOTEM_OF_UNDYING)) return 10;
        if (st.is(Items.DIAMOND)) return 5;
        if (st.is(Items.BLAZE_ROD)) return 3;
        if (st.is(Items.COAL_BLOCK)) return 9;
        if (st.is(Items.COAL) || st.is(Items.CHARCOAL)) return 1;
        if (st.is(ItemTags.LOGS_THAT_BURN)) return 0.5;
        if (st.is(ItemTags.PLANKS)) return 0.15;
        return 0;
    }

    public static void feed(ServerPlayer p) {
        GameState s = GameState.I;
        if (!s.crashed || s.victory || s.ended || s.ffa) return;
        if (GhostAllyManager.isAlly(p)) { VersionBridge.msg(p, "Frost Zombies cannot touch the Bonfire."); return; }
        ItemStack st = p.getMainHandItem();
        double v = value(st);
        if (v <= 0) {
            VersionBridge.actionBar(p, "Bonfire " + (int) s.bonfire + "% - feed it logs, coal, blaze rods, diamonds...");
            return;
        }
        int n = p.isShiftKeyDown() ? st.getCount() : 1;
        st.shrink(n);
        s.bonfire = Math.min(100, s.bonfire + v * n);
        if (!s.bonfireLit && s.bonfire >= 5) {
            s.bonfireLit = true;
            VersionBridge.broadcast(VersionBridge.serverOf(p), "\u00a76The Signal Bonfire is LIT! Survive the nights and keep it burning to 100%!");
        }
        visual(VersionBridge.overworld(VersionBridge.serverOf(p)));
        VersionBridge.actionBar(p, "Bonfire " + (int) s.bonfire + "%");
    }

    public static void tickSecond(MinecraftServer server) {
        GameState s = GameState.I;
        if (!s.crashed || !s.bonfireBuilt || s.ended || s.victory || s.ffa) return;
        ServerLevel ow = VersionBridge.overworld(server);
        if (s.bonfireLit) {
            s.bonfire -= 0.03;                           // slow natural burn-down
            s.bonfire -= 0.4 * WaveManager.attackers(ow); // mobs chewing on the fire
            if (s.bonfire <= 0) {
                s.bonfire = 0;
                visual(ow);
                FfaManager.begin(server);
                return;
            }
            if (s.bonfire >= 100) { s.bonfire = 100; visual(ow); SantaSleigh.start(server); return; }
        }
        if (server.getTickCount() % 200 == 0) visual(ow);
    }

    /** Log tower grows with progress; campfire on top is lit once the Bonfire is burning. */
    public static void visual(ServerLevel ow) {
        GameState s = GameState.I;
        int h = (int) (s.bonfire / 100.0 * MAX_HEIGHT);
        for (int i = 0; i <= MAX_HEIGHT + 1; i++) {
            BlockPos p = new BlockPos(s.bonfireX, s.bonfireY + i, s.bonfireZ);
            if (i < h) ow.setBlock(p, Blocks.SPRUCE_LOG.defaultBlockState(), 2);
            else if (i == h) ow.setBlock(p, Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT, s.bonfireLit), 2);
            else ow.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        }
    }
}
