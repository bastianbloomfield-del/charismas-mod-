package dev.endlessexpress.player;

import dev.endlessexpress.compat.VersionBridge;
import dev.endlessexpress.core.GameState;
import dev.endlessexpress.task.TaskManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** Daily Nice / Naughty / Nothing split at every in-game sunrise, plus buffs and sabotage. */
public final class TeamManager {
    private TeamManager() {}

    public static Team teamOf(ServerPlayer p) {
        String t = GameState.I.teams.get(p.getUUID());
        if (t == null) return Team.NOTHING;
        try { return Team.valueOf(t); } catch (Exception e) { return Team.NOTHING; }
    }

    /** Nice List players are 1.5x as effective at repairs and fueling. */
    public static double efficiency(ServerPlayer p) { return teamOf(p) == Team.NICE ? 1.5 : 1.0; }

    public static void assign(MinecraftServer server) {
        List<ServerPlayer> list = GhostAllyManager.survivors(server);
        Collections.shuffle(list);
        int offset = ThreadLocalRandom.current().nextInt(3);
        GameState.I.teams.clear();
        for (int i = 0; i < list.size(); i++) {
            Team t = Team.values()[(i + offset) % 3];
            ServerPlayer p = list.get(i);
            GameState.I.teams.put(p.getUUID(), t.name());
            switch (t) {
                case NICE -> VersionBridge.title(p, "Nice List", "Complete train tasks for bonus gifts");
                case NAUGHTY -> VersionBridge.title(p, "Naughty List", "Secretly sabotage the train");
                case NOTHING -> VersionBridge.title(p, "Nothing List", "Total freedom today");
            }
            VersionBridge.msg(p, "A new day begins. You are on the " + t.label + ". (Keep it to yourself!)");
        }
    }

    /** Refresh buffs; call every ~5 seconds. */
    public static void tickBuffs(MinecraftServer server) {
        for (ServerPlayer p : GhostAllyManager.survivors(server)) {
            switch (teamOf(p)) {
                case NICE -> { if (VersionBridge.inTrain(p)) VersionBridge.effect(p, MobEffects.HASTE, 140, 0); }
                case NAUGHTY -> VersionBridge.effect(p, MobEffects.SPEED, 140, 0);
                case NOTHING -> { if (VersionBridge.inOverworld(p)) VersionBridge.effect(p, MobEffects.RESISTANCE, 140, 0); }
            }
        }
    }

    // ------------------------------------------------------------------ sabotage

    /**
     * Naughty List players sneak + right-click with an empty hand on a task block to sabotage it.
     * Returns true if the interaction was consumed.
     */
    public static boolean trySabotage(ServerPlayer p, TaskManager.Task t) {
        if (teamOf(p) != Team.NAUGHTY || !p.isShiftKeyDown() || !p.getMainHandItem().isEmpty()) return false;
        switch (t.type()) {
            case FUEL_BARREL, GAUGE, GIFT_TABLE -> {}
            default -> { return false; }
        }
        if (TaskManager.cooldown(p, 200)) { VersionBridge.actionBar(p, "Lay low... you just acted."); return true; }
        // Witnesses: any other living player within 7 blocks (75% to actually notice)
        for (ServerPlayer o : VersionBridge.levelOf(p).players()) {
            if (o == p || o.isSpectator() || o.distanceTo(p) > 7) continue;
            if (ThreadLocalRandom.current().nextInt(100) < 75) {
                VersionBridge.msg(p, "You were CAUGHT by " + o.getScoreboardName() + "! The sabotage failed.");
                VersionBridge.msg(o, "You caught " + p.getScoreboardName() + " trying to sabotage the train!");
                VersionBridge.effect(p, MobEffects.GLOWING, 400, 0);
                return true;
            }
        }
        GameState s = GameState.I;
        switch (t.type()) {
            case FUEL_BARREL -> {
                if (s.fuel < 50) { VersionBridge.msg(p, "There is hardly any fuel left to drain."); return true; }
                s.fuel -= 150;
                VersionBridge.msg(p, "You quietly siphon off 150 fuel.");
            }
            case GAUGE -> { s.jammed = true; VersionBridge.msg(p, "You jam the boiler valve."); }
            case GIFT_TABLE -> {
                List<UUID> victims = new ArrayList<>();
                for (var e : s.gifts.entrySet()) if (!e.getKey().equals(p.getUUID()) && !e.getValue().isEmpty()) victims.add(e.getKey());
                if (victims.isEmpty()) { VersionBridge.msg(p, "No uncollected gifts to steal."); return true; }
                UUID v = victims.get(ThreadLocalRandom.current().nextInt(victims.size()));
                String stolen = s.gifts.get(v).remove(0);
                for (String spec : GiftTable.specs(stolen)) VersionBridge.give(p, spec);
                VersionBridge.msg(p, "You stole: " + GiftTable.name(stolen));
                return true; // stealing is its own reward
            }
            default -> {}
        }
        String[] loot = {"minecraft:coal_block 2", "minecraft:tnt 2", "minecraft:diamond 1"};
        String r = loot[ThreadLocalRandom.current().nextInt(loot.length)];
        VersionBridge.give(p, r);
        VersionBridge.msg(p, "Sabotage reward received.");
        return true;
    }
}
