package dev.endlessexpress.train.carriage;

import dev.endlessexpress.compat.VersionBridge;
import dev.endlessexpress.core.GameState;
import dev.endlessexpress.player.GhostAllyManager;
import dev.endlessexpress.player.GiftTable;
import dev.endlessexpress.player.Team;
import dev.endlessexpress.player.TeamManager;
import dev.endlessexpress.task.TaskManager.Task;
import dev.endlessexpress.task.TaskType;
import dev.endlessexpress.train.TrainBuilder;
import dev.endlessexpress.train.TrainLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.*;

/** Dining room: post-departure feast, gift table, dishes, restocking barrels and the exit door. */
public final class DiningCarriage {
    private DiningCarriage() {}
    static final int K = TrainLayout.DINING;
    public static final int FEAST_TICKS = 3600; // 3 minutes to eat and claim

    static BlockPos eatTable() { return TrainLayout.pos(K, 0, 1, 6); }
    static BlockPos giftTable() { return TrainLayout.pos(K, 0, 1, 10); }
    static BlockPos dish(int i) {
        return switch (i) {
            case 0 -> TrainLayout.pos(K, -1, 2, 6); case 1 -> TrainLayout.pos(K, 1, 2, 6);
            case 2 -> TrainLayout.pos(K, -1, 2, 8); case 3 -> TrainLayout.pos(K, 1, 2, 8);
            default -> TrainLayout.pos(K, 1, 2, 10);
        };
    }
    static BlockPos[] barrels() {
        return new BlockPos[]{TrainLayout.pos(K, -3, 1, 3), TrainLayout.pos(K, -3, 1, 4), TrainLayout.pos(K, 3, 1, 3), TrainLayout.pos(K, 3, 1, 4)};
    }

    public static void build(ServerLevel l) {
        TrainBuilder.shell(l, K);
        for (int lx = -1; lx <= 1; lx++) for (int lz = 5; lz <= 11; lz++)
            TrainBuilder.set(l, TrainLayout.pos(K, lx, 1, lz), Blocks.SPRUCE_PLANKS.defaultBlockState());
        TrainBuilder.set(l, eatTable(), Blocks.CAKE.defaultBlockState());
        TrainBuilder.set(l, giftTable(), Blocks.CHEST.defaultBlockState());
        for (BlockPos b : barrels()) {
            TrainBuilder.set(l, b, Blocks.BARREL.defaultBlockState());
            if (l.getBlockEntity(b) instanceof Container c) c.setItem(0, new ItemStack(Items.BREAD, 8));
        }
        // Exit door on the east wall, framed with sea lanterns (cells x=4, y=1..2, lz=7..8 stay open)
        for (int ly = 1; ly <= 2; ly++) for (int lz = 7; lz <= 8; lz++) TrainBuilder.set(l, TrainLayout.pos(K, 4, ly, lz), Blocks.AIR.defaultBlockState());
        for (int lz = 6; lz <= 9; lz++) TrainBuilder.set(l, TrainLayout.pos(K, 4, 3, lz), Blocks.SEA_LANTERN.defaultBlockState());
        for (int ly = 1; ly <= 2; ly++) { TrainBuilder.set(l, TrainLayout.pos(K, 4, ly, 6), Blocks.SEA_LANTERN.defaultBlockState()); TrainBuilder.set(l, TrainLayout.pos(K, 4, ly, 9), Blocks.SEA_LANTERN.defaultBlockState()); }
        TrainBuilder.set(l, TrainLayout.pos(K, 0, 5, 7), TrainBuilder.hangingLantern());
    }

    public static void register(Map<BlockPos, Task> m) {
        m.put(eatTable(), new Task(TaskType.EAT_TABLE, 0));
        m.put(giftTable(), new Task(TaskType.GIFT_TABLE, 0));
        for (int i = 0; i < 5; i++) m.put(dish(i), new Task(TaskType.DISH, i));
    }

    /** True if the player's block position is inside the exit doorway. */
    public static boolean inExitDoor(ServerPlayer p) {
        BlockPos b = p.blockPosition();
        if (TrainLayout.carriageAt(b) != K) return false;
        int lz = b.getZ() - K * TrainLayout.LEN, ly = b.getY() - TrainLayout.BASE_Y;
        return b.getX() >= 4 && ly >= 1 && ly <= 2 && lz >= 7 && lz <= 8 || (b.getX() == 3 && ly >= 1 && ly <= 2 && lz >= 7 && lz <= 8 && p.getX() > 3.7);
    }

    // ------------------------------------------------------------------ interaction

    public static boolean use(ServerPlayer p, Task t, BlockPos pos) {
        GameState s = GameState.I;
        ServerLevel l = VersionBridge.levelOf(p);
        switch (t.type()) {
            case EAT_TABLE -> {
                if (s.feastTicksLeft <= 0) { VersionBridge.actionBar(p, "No feast right now. Meals are served after each departure."); return true; }
                if (s.attended.contains(p.getUUID())) { VersionBridge.actionBar(p, "You have already eaten."); return true; }
                if (!consumeFood(l)) { VersionBridge.msg(p, "The Dining Room barrels are out of food! Restock them from the Greenhouse."); return true; }
                p.getFoodData().setFoodLevel(20);
                p.getFoodData().setSaturation(10f);
                s.attended.add(p.getUUID());
                VersionBridge.msg(p, "You enjoy the feast. Claim your gift at the gift chest!");
            }
            case GIFT_TABLE -> {
                List<String> list = s.gifts.getOrDefault(p.getUUID(), new ArrayList<>());
                if (list.isEmpty()) { VersionBridge.actionBar(p, "You have no gifts waiting."); return true; }
                for (String g : list) {
                    for (String spec : GiftTable.specs(g)) VersionBridge.give(p, spec);
                    VersionBridge.msg(p, "Gift claimed: " + GiftTable.name(g));
                }
                list.clear();
            }
            case DISH -> {
                if (!s.dirtyDishes.contains(t.index())) return true;
                s.dirtyDishes.remove(Integer.valueOf(t.index()));
                TrainBuilder.set(l, pos, Blocks.AIR.defaultBlockState());
                VersionBridge.actionBar(p, "Dish cleared.");
            }
            default -> { return false; }
        }
        return true;
    }

    private static boolean consumeFood(ServerLevel l) {
        for (BlockPos b : barrels()) {
            BlockEntity be = l.getBlockEntity(b);
            if (!(be instanceof Container c)) continue;
            for (int i = 0; i < c.getContainerSize(); i++) {
                ItemStack st = c.getItem(i);
                if (!st.isEmpty() && VersionBridge.isFood(st)) { st.shrink(1); c.setChanged(); return true; }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ feast

    /** Called when the train leaves a station: summon everyone aboard, roll gifts, start the meal timer. */
    public static void startFeast(MinecraftServer server) {
        GameState s = GameState.I;
        ServerLevel tl = VersionBridge.trainLevel(server);
        boolean dirtyBefore = s.dirtyDishes.size() >= 4;
        s.attended.clear();
        s.feastTicksLeft = FEAST_TICKS;
        int seat = 0;
        for (ServerPlayer p : GhostAllyManager.survivors(server)) {
            if (!VersionBridge.inTrain(p)) continue;
            double x = (seat % 2 == 0) ? -2.5 : 2.5;
            double z = TrainLayout.DINING * TrainLayout.LEN + 6.5 + (seat / 2) % 5;
            VersionBridge.teleport(p, tl, x + 0.5 * 2, TrainLayout.BASE_Y + 1, z, seat % 2 == 0 ? -90f : 90f, 0f);
            seat++;
            List<String> list = s.gifts.computeIfAbsent(p.getUUID(), k -> new ArrayList<>());
            if (list.size() < 6) list.add(GiftTable.roll());
            if (TeamManager.teamOf(p) == Team.NICE && list.size() < 6) list.add(GiftTable.rollBonus());
            VersionBridge.title(p, "Feast Time", "Eat at the table, then claim your gift");
            if (dirtyBefore) VersionBridge.effect(p, MobEffects.SLOWNESS, 1200, 0);
        }
        s.dirtyDishes.clear();
        for (int i = 0; i < 5; i++) { s.dirtyDishes.add(i); TrainBuilder.set(tl, dish(i), Blocks.FLOWER_POT.defaultBlockState()); }
    }

    /** Per-second feast timer; punishes players who skipped the meal. */
    public static void tickSecond(MinecraftServer server) {
        GameState s = GameState.I;
        if (s.feastTicksLeft <= 0) return;
        s.feastTicksLeft -= 20;
        if (s.feastTicksLeft > 0) return;
        for (ServerPlayer p : GhostAllyManager.survivors(server)) {
            if (s.attended.contains(p.getUUID())) continue;
            VersionBridge.effect(p, MobEffects.HUNGER, 1200, 1);
            VersionBridge.effect(p, MobEffects.MINING_FATIGUE, 2400, 0);
            VersionBridge.msg(p, "You skipped the meal! Hunger and Mining Fatigue set in.");
        }
        s.attended.clear();
    }
}
