package dev.endlessexpress.player;

import dev.endlessexpress.compat.VersionBridge;
import dev.endlessexpress.core.GameState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.List;

/** Eliminated players: Frozen Ghost (spectator) until the crash, then Frost Zombie allies of the mob waves. */
public final class GhostAllyManager {
    private GhostAllyManager() {}

    public static void setupTeams(MinecraftServer s) {
        VersionBridge.runCommand(s, "team add ee_frost");
        VersionBridge.runCommand(s, "team modify ee_frost color aqua");
        VersionBridge.runCommand(s, "team modify ee_frost friendlyFire false");
        VersionBridge.runCommand(s, "team modify ee_frost prefix {\"text\":\"[Frost Zombie] \"}");
        VersionBridge.runCommand(s, "team add ee_survivors");
        VersionBridge.runCommand(s, "team modify ee_survivors color green");
    }

    public static boolean isAlly(ServerPlayer p) { return GameState.I.allies.contains(p.getUUID()); }

    public static boolean isSurvivor(ServerPlayer p) {
        GameState s = GameState.I;
        return s.livesOf(p.getUUID()) > 0 && !s.allies.contains(p.getUUID()) && !p.isSpectator() && p.isAlive();
    }

    public static List<ServerPlayer> survivors(MinecraftServer server) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) if (isSurvivor(p)) out.add(p);
        return out;
    }

    public static void makeGhost(ServerPlayer p) {
        VersionBridge.gameMode(p, GameType.SPECTATOR);
        VersionBridge.msg(p, "You are a Frozen Ghost. Watch from the shadows until the train crashes...");
    }

    public static void makeAlly(ServerPlayer p) {
        MinecraftServer s = VersionBridge.serverOf(p);
        GameState.I.allies.add(p.getUUID());
        VersionBridge.gameMode(p, GameType.SURVIVAL);
        p.setHealth(p.getMaxHealth());
        String n = p.getScoreboardName();
        VersionBridge.runCommand(s, "team join ee_frost " + n);
        String[] slots = {"head", "chest", "legs", "feet"};
        String[] gear = {"helmet", "chestplate", "leggings", "boots"};
        for (int i = 0; i < 4; i++)
            VersionBridge.runCommand(s, "item replace entity " + n + " armor." + slots[i]
                    + " with minecraft:leather_" + gear[i] + "[minecraft:dyed_color=11393254]");
        VersionBridge.effect(p, MobEffects.NIGHT_VISION, 20 * 60 * 20, 0);
        VersionBridge.effect(p, MobEffects.SPEED, 20 * 60 * 20, 0);
        VersionBridge.msg(p, "You rise as a FROST ZOMBIE! Help the winter waves overwhelm the survivors. You cannot break the Bonfire directly.");
        VersionBridge.title(p, "Frost Zombie", "Help the mobs");
    }

    public static void removeAlly(ServerPlayer p) {
        GameState.I.allies.remove(p.getUUID());
        VersionBridge.runCommand(VersionBridge.serverOf(p), "team leave " + p.getScoreboardName());
    }
}
