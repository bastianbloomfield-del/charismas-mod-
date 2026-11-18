package dev.endlessexpress.compat;

import dev.endlessexpress.EndlessExpress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

import java.util.Collections;

/**
 * Every call that touches a Minecraft API likely to differ between 26.2 and 26.3
 * (or to be renamed again) lives here. If the project fails to compile after a
 * Minecraft update, fix THIS file first.
 */
public final class VersionBridge {
    private VersionBridge() {}

    public static final ResourceKey<Level> TRAIN_DIM =
            ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("endlessexpress", "train_void"));

    public static ServerLevel trainLevel(MinecraftServer s) { return s.getLevel(TRAIN_DIM); }
    public static ServerLevel overworld(MinecraftServer s) { return s.overworld(); }
    public static ServerLevel levelOf(ServerPlayer p) { return (ServerLevel) p.level(); }
    public static MinecraftServer serverOf(ServerPlayer p) { return levelOf(p).getServer(); }
    public static boolean inTrain(ServerPlayer p) { return p.level().dimension().equals(TRAIN_DIM); }
    public static boolean inOverworld(ServerPlayer p) { return p.level().dimension().equals(Level.OVERWORLD); }

    public static void msg(ServerPlayer p, String text) { p.sendSystemMessage(Component.literal(text)); }
    public static void actionBar(ServerPlayer p, String text) { p.displayClientMessage(Component.literal(text), true); }
    public static void broadcast(MinecraftServer s, String text) {
        s.getPlayerList().broadcastSystemMessage(Component.literal(text), false);
    }

    public static void runCommand(MinecraftServer s, String cmd) {
        try {
            s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), cmd);
        } catch (Exception e) {
            EndlessExpress.LOGGER.warn("Command failed: {} ({})", cmd, e.toString());
        }
    }

    /** Title + subtitle via commands (stable across versions). Avoid double quotes in text. */
    public static void title(ServerPlayer p, String title, String sub) {
        MinecraftServer s = serverOf(p);
        String n = p.getScoreboardName();
        runCommand(s, "title " + n + " times 10 70 20");
        runCommand(s, "title " + n + " subtitle {\"text\":\"" + sub + "\"}");
        runCommand(s, "title " + n + " title {\"text\":\"" + title + "\"}");
    }

    public static void teleport(ServerPlayer p, ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        p.teleportTo(level, x, y, z, Collections.emptySet(), yaw, pitch, true);
    }

    public static void effect(ServerPlayer p, Holder<MobEffect> eff, int ticks, int amp) {
        p.addEffect(new MobEffectInstance(eff, ticks, amp, false, false, true));
    }

    public static void giveStack(ServerPlayer p, ItemStack st) {
        if (!p.getInventory().add(st)) p.drop(st, false);
    }

    /** spec example: "minecraft:diamond 2" */
    public static void give(ServerPlayer p, String spec) {
        runCommand(serverOf(p), "give " + p.getScoreboardName() + " " + spec);
    }

    public static void freezeDamage(ServerPlayer p, float amount) {
        ServerLevel l = levelOf(p);
        p.hurtServer(l, l.damageSources().freeze(), amount);
    }

    public static void gameMode(ServerPlayer p, GameType t) { p.setGameMode(t); }
    public static boolean isFood(ItemStack st) { return st.has(DataComponents.FOOD); }
    public static boolean loaded(ServerLevel l, BlockPos p) { return l.hasChunkAt(p); }
}
