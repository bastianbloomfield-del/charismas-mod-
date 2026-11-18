package dev.endlessexpress.task;

import dev.endlessexpress.compat.VersionBridge;
import dev.endlessexpress.core.GameState;
import dev.endlessexpress.finale.Bonfire;
import dev.endlessexpress.player.TeamManager;
import dev.endlessexpress.train.carriage.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Maps block positions to tasks and dispatches right-clicks to the carriage classes. */
public final class TaskManager {
    private TaskManager() {}

    public record Task(TaskType type, int index) {}

    public static final Map<BlockPos, Task> TASKS = new HashMap<>();
    private static final Map<UUID, Integer> LAST_USE = new HashMap<>();

    public static void init() {
        TASKS.clear();
        LocomotiveCarriage.register(TASKS);
        FuelCarriage.register(TASKS);
        BoilerCarriage.register(TASKS);
        DiningCarriage.register(TASKS);
        GreenhouseCarriage.register(TASKS);
    }

    /** True if the player acted within the last `ticks` ticks (and records this use otherwise). */
    public static boolean cooldown(ServerPlayer p, int ticks) {
        int now = VersionBridge.serverOf(p).getTickCount();
        Integer last = LAST_USE.get(p.getUUID());
        if (last != null && now - last < ticks) return true;
        LAST_USE.put(p.getUUID(), now);
        return false;
    }

    public static InteractionResult onUse(Player player, Level world, InteractionHand hand, BlockHitResult hit) {
        if (world.isClientSide() || hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer sp))
            return InteractionResult.PASS;
        if (!GameState.I.started) return InteractionResult.PASS;
        BlockPos pos = hit.getBlockPos();
        if (world.dimension().equals(VersionBridge.TRAIN_DIM)) {
            Task t = TASKS.get(pos);
            if (t == null || sp.isSpectator()) return InteractionResult.PASS;
            if (TeamManager.trySabotage(sp, t)) return InteractionResult.SUCCESS;
            return dispatch(sp, t, pos) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (world.dimension().equals(Level.OVERWORLD) && Bonfire.isFeedPos(pos)) {
            Bonfire.feed(sp);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private static boolean dispatch(ServerPlayer p, Task t, BlockPos pos) {
        return switch (t.type()) {
            case FIREBOX, BRAKE, HORN -> LocomotiveCarriage.use(p, t);
            case FUEL_BARREL, WATER_TANK, LAVA_TANK, PIPE -> FuelCarriage.use(p, t);
            case GAUGE, ASH_FURNACE, VENT -> BoilerCarriage.use(p, t);
            case EAT_TABLE, GIFT_TABLE, DISH -> DiningCarriage.use(p, t, pos);
            case LAMP -> GreenhouseCarriage.use(p, t);
        };
    }
}
