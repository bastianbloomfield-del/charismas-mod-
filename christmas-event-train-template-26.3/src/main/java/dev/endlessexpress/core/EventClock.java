package dev.endlessexpress.core;

import dev.endlessexpress.config.ModConfig;

/** Maps real-world elapsed time to the current GamePhase. */
public final class EventClock {
    private EventClock() {}

    public static GamePhase phase() {
        GameState s = GameState.I;
        if (s.ended) return GamePhase.ENDED;
        if (!s.forcedPhase.isEmpty()) {
            try { return GamePhase.valueOf(s.forcedPhase.toUpperCase()); } catch (Exception ignored) {}
        }
        if (!s.started) return GamePhase.PROLOGUE;
        double days = (System.currentTimeMillis() - s.startMillis) / (ModConfig.I.dayLengthSeconds * 1000.0);
        if (days < ModConfig.I.prologueDays) return GamePhase.PROLOGUE;
        if (days < ModConfig.I.journeyEndDay) return GamePhase.JOURNEY;
        return GamePhase.FINALE;
    }

    public static boolean beforeFinale() {
        GamePhase p = phase();
        return p == GamePhase.PROLOGUE || p == GamePhase.JOURNEY;
    }
}
