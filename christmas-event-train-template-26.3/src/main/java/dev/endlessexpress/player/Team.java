package dev.endlessexpress.player;

public enum Team {
    NICE("Nice List"), NAUGHTY("Naughty List"), NOTHING("Nothing List");
    public final String label;
    Team(String label) { this.label = label; }
}
