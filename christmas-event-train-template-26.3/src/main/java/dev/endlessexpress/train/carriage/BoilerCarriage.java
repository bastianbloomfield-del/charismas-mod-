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
import net.minecraft.world.level.block.Blocks;

import java.util.Map;

/** Heating and de-icing: ice vents, ash furnaces, pressure gauge. */
public final class BoilerCarriage {
    private BoilerCarriage() {}
    static final int K = TrainLayout.BOILER;
    public static final int VENTS = 3;

    public static BlockPos vent(int i) {
        return switch (i) { case 0 -> TrainLayout.pos(K, -2, 5, 10); case 1 -> TrainLayout.pos(K, 2, 5, 10); default -> TrainLayout.pos(K, 0, 5, 12); };
    }
    static BlockPos gauge() { return TrainLayout.pos(K, 0, 3, 2); }
    static BlockPos ash(int i) { return TrainLayout.pos(K, i == 0 ? -2 : 2, 1, 6); }

    public static void build(ServerLevel l) {
        TrainBuilder.shell(l, K);
        TrainBuilder.set(l, gauge(), Blocks.TARGET.defaultBlockState());
        for (int i = 0; i < 2; i++) TrainBuilder.set(l, ash(i), Blocks.FURNACE.defaultBlockState());
        for (int lz = 7; lz <= 9; lz++) TrainBuilder.set(l, TrainLayout.pos(K, 0, 1, lz), Blocks.BLAST_FURNACE.defaultBlockState());
        for (int i = 0; i < VENTS; i++) setVent(l, i, false);
    }

    public static void setVent(ServerLevel l, int i, boolean ice) {
        TrainBuilder.set(l, vent(i), (ice ? Blocks.BLUE_ICE : Blocks.IRON_BARS).defaultBlockState());
    }

    public static void register(Map<BlockPos, Task> m) {
        m.put(gauge(), new Task(TaskType.GAUGE, 0));
        for (int i = 0; i < 2; i++) m.put(ash(i), new Task(TaskType.ASH_FURNACE, i));
        for (int i = 0; i < VENTS; i++) m.put(vent(i), new Task(TaskType.VENT, i));
    }

    /** Called from the block-break hook when a player mines ice off a vent. */
    public static void clearIce(ServerLevel l, ServerPlayer p, int index) {
        GameState s = GameState.I;
        s.iceVents.remove(Integer.valueOf(index));
        setVent(l, index, false);
        s.boiler = Math.min(100, s.boiler + 2 * TeamManager.efficiency(p));
        VersionBridge.actionBar(p, "Vent cleared! Boiler " + (int) s.boiler + "%");
    }

    public static boolean use(ServerPlayer p, Task t) {
        GameState s = GameState.I;
        switch (t.type()) {
            case ASH_FURNACE -> {
                if (s.ash < 10) { VersionBridge.actionBar(p, "The furnace is clean."); return true; }
                s.ash = 0;
                s.boiler = Math.min(100, s.boiler + 3 * TeamManager.efficiency(p));
                VersionBridge.actionBar(p, "Ash emptied. Heat circulates evenly again.");
            }
            case GAUGE -> {
                if (s.pressure > 90 || s.pressure < 20 || s.jammed) {
                    s.pressure = 50; s.jammed = false;
                    VersionBridge.actionBar(p, "Pressure balanced.");
                } else VersionBridge.actionBar(p, "Boiler pressure: " + (int) s.pressure + "% (stable)");
            }
            case VENT -> {
                if (s.iceVents.contains(t.index())) VersionBridge.actionBar(p, "Mine the ice off this vent!");
                else VersionBridge.actionBar(p, "Vent is clear.");
            }
            default -> { return false; }
        }
        return true;
    }
}
