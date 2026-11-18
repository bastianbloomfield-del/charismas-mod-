package dev.endlessexpress.train.carriage;

import dev.endlessexpress.compat.VersionBridge;
import dev.endlessexpress.core.GameState;
import dev.endlessexpress.core.TrainState;
import dev.endlessexpress.player.TeamManager;
import dev.endlessexpress.task.TaskManager.Task;
import dev.endlessexpress.task.TaskType;
import dev.endlessexpress.train.TrainBuilder;
import dev.endlessexpress.train.TrainLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.Map;

/** Front locomotive: firebox (stoke / ignite), brake lever, horn bell. */
public final class LocomotiveCarriage {
    private LocomotiveCarriage() {}
    static final int K = TrainLayout.LOCO;

    static BlockPos firebox() { return TrainLayout.pos(K, 0, 1, 4); }
    static BlockPos brake() { return TrainLayout.pos(K, -3, 2, 8); }
    static BlockPos horn() { return TrainLayout.pos(K, 3, 1, 8); }

    public static void build(ServerLevel l) {
        TrainBuilder.shell(l, K);
        TrainBuilder.set(l, firebox(), Blocks.BLAST_FURNACE.defaultBlockState());
        TrainBuilder.set(l, brake(), Blocks.LEVER.defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.WALL)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
        TrainBuilder.set(l, horn(), Blocks.BELL.defaultBlockState());
        TrainBuilder.set(l, TrainLayout.pos(K, -3, 1, 12), Blocks.COAL_BLOCK.defaultBlockState());
        TrainBuilder.set(l, TrainLayout.pos(K, 3, 1, 12), Blocks.COAL_BLOCK.defaultBlockState());
        TrainBuilder.set(l, TrainLayout.pos(K, 0, 5, 7), TrainBuilder.hangingLantern());
    }

    public static void register(Map<BlockPos, Task> m) {
        m.put(firebox(), new Task(TaskType.FIREBOX, 0));
        m.put(brake(), new Task(TaskType.BRAKE, 0));
        m.put(horn(), new Task(TaskType.HORN, 0));
    }

    public static boolean use(ServerPlayer p, Task t) {
        GameState s = GameState.I;
        ServerLevel l = VersionBridge.levelOf(p);
        switch (t.type()) {
            case FIREBOX -> {
                if (!s.engineLit) {
                    ItemStack st = p.getMainHandItem();
                    if (FuelCarriage.fuelValue(st) <= 0) { VersionBridge.msg(p, "Hold a fuel item (coal, charcoal or logs) and right-click to ignite the engine."); return true; }
                    st.shrink(1);
                    s.engineLit = true;
                    if (s.trainState == TrainState.UNLIT) s.trainState = TrainState.STOPPED;
                    VersionBridge.broadcast(VersionBridge.serverOf(p), p.getScoreboardName() + " has IGNITED the Locomotive Engine!");
                    return true;
                }
                if (TaskManagerCooldown.hit(p)) return true;
                if (s.fuel < 10) { VersionBridge.actionBar(p, "The Fuel Carriage is out of fuel!"); return true; }
                s.fuel -= 10;
                s.engine = Math.min(100, s.engine + 6 * TeamManager.efficiency(p));
                l.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1f, 1f);
                VersionBridge.actionBar(p, "Stoking... Engine " + (int) s.engine + "%");
            }
            case BRAKE -> {
                if (s.brakeActive) {
                    s.brakeActive = false;
                    s.engine = Math.min(100, s.engine + 4);
                    VersionBridge.msg(p, "Brakes secured. Good catch!");
                } else VersionBridge.actionBar(p, "The brakes are fine for now.");
            }
            case HORN -> {
                boolean warning = s.trainState == TrainState.STOPPED && s.stateTicks >= dev.endlessexpress.config.ModConfig.I.warningAtStoppedTicks;
                l.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 3f, 0.5f);
                if (warning && !s.hornSounded) {
                    s.hornSounded = true;
                    var server = VersionBridge.serverOf(p);
                    for (ServerPlayer o : VersionBridge.overworld(server).players())
                        VersionBridge.title(o, "ALL ABOARD", "The train departs soon");
                    VersionBridge.msg(p, "You sounded the horn across the station.");
                }
            }
            default -> { return false; }
        }
        return true;
    }

    /** Tiny helper so the stoke action cannot be spammed faster than 0.5 s. */
    static final class TaskManagerCooldown {
        static boolean hit(ServerPlayer p) { return dev.endlessexpress.task.TaskManager.cooldown(p, 10); }
    }
}
