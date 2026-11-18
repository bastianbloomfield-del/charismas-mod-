package dev.endlessexpress.ui;

import dev.endlessexpress.compat.VersionBridge;
import dev.endlessexpress.core.EventClock;
import dev.endlessexpress.core.GamePhase;
import dev.endlessexpress.core.GameState;
import dev.endlessexpress.core.TrainSchedule;
import dev.endlessexpress.player.GhostAllyManager;
import dev.endlessexpress.player.LivesManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Lives, train clock, meters and Bonfire shown on every player's action bar once per second. */
public final class ActionBarHud {
    private ActionBarHud() {}

    public static void tickSecond(MinecraftServer server) {
        GameState s = GameState.I;
        if (!s.started) return;
        GamePhase ph = EventClock.phase();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            String text;
            if (GhostAllyManager.isAlly(p)) {
                text = "\u00a7b[Frost Zombie] \u00a77Bonfire " + (int) s.bonfire + "%";
            } else if (p.isSpectator() || s.livesOf(p.getUUID()) <= 0) {
                text = "\u00a77[Frozen Ghost]";
            } else if (ph == GamePhase.FINALE || s.crashed) {
                text = LivesManager.hearts(p) + " \u00a77| \u00a76Bonfire " + (int) s.bonfire + "%"
                        + (s.ffa ? " \u00a7c| FFA" : "");
            } else {
                text = LivesManager.hearts(p) + " \u00a77| \u00a7f" + s.trainState + " " + TrainSchedule.clockText()
                        + " \u00a77| St." + s.stationIndex
                        + " \u00a7cE" + (int) s.engine + " \u00a76B" + (int) s.boiler + " \u00a7bP" + (int) s.pipe;
            }
            VersionBridge.actionBar(p, text);
        }
    }
}
