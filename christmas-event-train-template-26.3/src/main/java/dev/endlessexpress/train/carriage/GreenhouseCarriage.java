package dev.endlessexpress.train.carriage;

import dev.endlessexpress.compat.VersionBridge;
import dev.endlessexpress.core.GameState;
import dev.endlessexpress.task.TaskManager.Task;
import dev.endlessexpress.task.TaskType;
import dev.endlessexpress.train.TrainBuilder;
import dev.endlessexpress.train.TrainLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;

import java.util.Map;

/** The only place crops grow: farmland, water channel, composters, grow-lamps. */
public final class GreenhouseCarriage {
    private GreenhouseCarriage() {}
    static final int K = TrainLayout.GREEN;
    public static final int LAMPS = 6;

    /** Lamp i: x = -2 for 0..2, +2 for 3..5; lz = 4, 8, 12. */
    public static BlockPos lamp(int i) { return TrainLayout.pos(K, i < 3 ? -2 : 2, 5, 4 + 4 * (i % 3)); }

    public static void build(ServerLevel l) {
        TrainBuilder.shell(l, K);
        var soil = Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7);
        for (int lz = 3; lz <= 12; lz++) for (int lx = -3; lx <= 3; lx++)
            if (lx != 0) TrainBuilder.set(l, TrainLayout.pos(K, lx, 1, lz), soil);
        TrainBuilder.set(l, TrainLayout.pos(K, 0, 1, 2), Blocks.SPRUCE_PLANKS.defaultBlockState());
        TrainBuilder.set(l, TrainLayout.pos(K, 0, 1, 13), Blocks.SPRUCE_PLANKS.defaultBlockState());
        setChannel(l, false);
        TrainBuilder.set(l, TrainLayout.pos(K, -3, 1, 13), Blocks.COMPOSTER.defaultBlockState());
        TrainBuilder.set(l, TrainLayout.pos(K, 3, 1, 13), Blocks.COMPOSTER.defaultBlockState());
        for (int lx : new int[]{-3, 3}) {
            BlockPos b = TrainLayout.pos(K, lx, 1, 2);
            TrainBuilder.set(l, b, Blocks.BARREL.defaultBlockState());
            if (l.getBlockEntity(b) instanceof Container c) {
                c.setItem(0, new ItemStack(Items.WHEAT_SEEDS, 16));
                c.setItem(1, new ItemStack(Items.CARROT, 8));
                c.setItem(2, new ItemStack(Items.POTATO, 8));
                c.setItem(3, new ItemStack(Items.BEETROOT_SEEDS, 8));
                c.setItem(4, new ItemStack(Items.BONE_MEAL, 8));
            }
        }
        for (int i = 0; i < LAMPS; i++) setLamp(l, i, true);
    }

    public static void setLamp(ServerLevel l, int i, boolean lit) {
        TrainBuilder.set(l, lamp(i), (lit ? Blocks.GLOWSTONE : Blocks.BLACKSTONE).defaultBlockState());
    }

    /** Water channel down the middle; turns to ice when the boiler runs cold. */
    public static void setChannel(ServerLevel l, boolean frozen) {
        for (int lz = 3; lz <= 12; lz++)
            TrainBuilder.set(l, TrainLayout.pos(K, 0, 1, lz), (frozen ? Blocks.ICE : Blocks.WATER).defaultBlockState());
        GameState.I.greenFrozen = frozen;
    }

    public static void register(Map<BlockPos, Task> m) {
        for (int i = 0; i < LAMPS; i++) m.put(lamp(i), new Task(TaskType.LAMP, i));
    }

    public static boolean use(ServerPlayer p, Task t) {
        GameState s = GameState.I;
        if (!s.lampsOut.contains(t.index())) { VersionBridge.actionBar(p, "This grow-lamp is burning bright."); return true; }
        s.lampsOut.remove(Integer.valueOf(t.index()));
        setLamp(VersionBridge.levelOf(p), t.index(), true);
        VersionBridge.actionBar(p, "Grow-lamp relit.");
        return true;
    }
}
