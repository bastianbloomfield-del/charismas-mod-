package dev.endlessexpress.train.carriage;

import dev.endlessexpress.compat.VersionBridge;
import dev.endlessexpress.core.GameState;
import dev.endlessexpress.player.TeamManager;
import dev.endlessexpress.task.TaskManager.Task;
import dev.endlessexpress.task.TaskType;
import dev.endlessexpress.train.TrainBuilder;
import dev.endlessexpress.train.TrainLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;

import java.util.Map;

/** Fuel hoppers/barrels, water and lava tanks, leaking pipes. */
public final class FuelCarriage {
    private FuelCarriage() {}
    static final int K = TrainLayout.FUEL;
    public static final int FUEL_CAP = 2400, TANK_CAP = 2000, PIPES = 6;

    static BlockPos barrel(int i) { return TrainLayout.pos(K, -3, 1, 3 + i); }
    static BlockPos waterTank() { return TrainLayout.pos(K, 3, 1, 3); }
    static BlockPos lavaTank() { return TrainLayout.pos(K, 3, 1, 6); }
    public static BlockPos pipe(int i) { return TrainLayout.pos(K, 0, 5, 2 + 2 * i); }

    public static void build(ServerLevel l) {
        TrainBuilder.shell(l, K);
        for (int i = 0; i < 3; i++) TrainBuilder.set(l, barrel(i), Blocks.BARREL.defaultBlockState());
        TrainBuilder.set(l, waterTank(), Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        TrainBuilder.set(l, lavaTank(), Blocks.LAVA_CAULDRON.defaultBlockState());
        for (int i = 0; i < PIPES; i++) setPipe(l, i, false);
    }

    public static void setPipe(ServerLevel l, int i, boolean leaking) {
        TrainBuilder.set(l, pipe(i), (leaking ? Blocks.RED_STAINED_GLASS : Blocks.IRON_BARS).defaultBlockState());
    }

    public static void register(Map<BlockPos, Task> m) {
        for (int i = 0; i < 3; i++) m.put(barrel(i), new Task(TaskType.FUEL_BARREL, i));
        m.put(waterTank(), new Task(TaskType.WATER_TANK, 0));
        m.put(lavaTank(), new Task(TaskType.LAVA_TANK, 0));
        for (int i = 0; i < PIPES; i++) m.put(pipe(i), new Task(TaskType.PIPE, i));
    }

    /** Fuel units one item is worth (0 if not fuel). */
    public static int fuelValue(ItemStack st) {
        if (st.isEmpty()) return 0;
        if (st.is(Items.COAL_BLOCK)) return 72;
        if (st.is(Items.COAL) || st.is(Items.CHARCOAL)) return 8;
        if (st.is(Items.BLAZE_ROD)) return 24;
        if (st.is(ItemTags.LOGS_THAT_BURN)) return 4;
        return 0;
    }

    public static boolean use(ServerPlayer p, Task t) {
        GameState s = GameState.I;
        ItemStack st = p.getMainHandItem();
        double eff = TeamManager.efficiency(p);
        switch (t.type()) {
            case FUEL_BARREL -> {
                int v = fuelValue(st);
                if (v <= 0) { VersionBridge.actionBar(p, "Fuel: " + s.fuel + "/" + FUEL_CAP + " (hold coal, logs or blaze rods)"); return true; }
                int n = p.isShiftKeyDown() ? st.getCount() : 1;
                int used = 0;
                while (used < n && s.fuel + (int) (v * eff) <= FUEL_CAP) { s.fuel += (int) (v * eff); used++; }
                if (used == 0) { VersionBridge.actionBar(p, "The fuel store is full."); return true; }
                st.shrink(used);
                VersionBridge.actionBar(p, "Fuel: " + s.fuel + "/" + FUEL_CAP);
            }
            case WATER_TANK -> {
                if (!st.is(Items.WATER_BUCKET)) { VersionBridge.actionBar(p, "Water: " + s.water + "/" + TANK_CAP + " (use a water bucket)"); return true; }
                if (s.water + 250 > TANK_CAP) { VersionBridge.actionBar(p, "The water tank is full."); return true; }
                s.water += 250; st.shrink(1); VersionBridge.giveStack(p, new ItemStack(Items.BUCKET));
                VersionBridge.actionBar(p, "Water: " + s.water + "/" + TANK_CAP);
            }
            case LAVA_TANK -> {
                if (!st.is(Items.LAVA_BUCKET)) { VersionBridge.actionBar(p, "Lava: " + s.lava + "/" + TANK_CAP + " (use a lava bucket)"); return true; }
                if (s.lava + 250 > TANK_CAP) { VersionBridge.actionBar(p, "The lava tank is full."); return true; }
                s.lava += 250; st.shrink(1); VersionBridge.giveStack(p, new ItemStack(Items.BUCKET));
                VersionBridge.actionBar(p, "Lava: " + s.lava + "/" + TANK_CAP);
            }
            case PIPE -> {
                if (!s.leaks.contains(t.index())) { VersionBridge.actionBar(p, "This pipe is sound."); return true; }
                if (!(st.is(Items.IRON_INGOT) || st.is(Items.COPPER_INGOT))) { VersionBridge.actionBar(p, "Patch the leak with an Iron or Copper Ingot."); return true; }
                st.shrink(1);
                s.leaks.remove(Integer.valueOf(t.index()));
                setPipe(VersionBridge.levelOf(p), t.index(), false);
                s.pipe = Math.min(100, s.pipe + 8 * eff);
                VersionBridge.actionBar(p, "Pipe patched! Pipe health " + (int) s.pipe + "%");
            }
            default -> { return false; }
        }
        return true;
    }
}
