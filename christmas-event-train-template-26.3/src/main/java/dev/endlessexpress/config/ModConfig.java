package dev.endlessexpress.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Tunable values. Written to config/endlessexpress.json on first launch. */
public final class ModConfig {
    public static ModConfig I = new ModConfig();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public int stationSpacing = 2500;
    public int maxStations = 400;
    public int trackY = 84;
    public int movingTicks = 12000;           // 10 min
    public int stoppedTicks = 48000;          // 40 min
    public int warningAtStoppedTicks = 42000; // 35:00 into the stop
    public int startingLives = 6;
    public double dayLengthSeconds = 86400;   // real-life day (lower for testing)
    public int prologueDays = 2;
    public int journeyEndDay = 14;
    public int spawnRadius = 1000;
    public int snowMaxY = 319;
    public int snowColumnsPerPlayerPerTick = 3;
    public int snowRadius = 48;
    public int maxPlayerCarriages = 40;
    public boolean snowStacking = true;

    public static void load() {
        Path p = FabricLoader.getInstance().getConfigDir().resolve("endlessexpress.json");
        try {
            if (Files.exists(p)) I = GSON.fromJson(Files.readString(p), ModConfig.class);
            else Files.writeString(p, GSON.toJson(I));
        } catch (Exception e) {
            System.err.println("[EndlessExpress] config error: " + e);
        }
    }
}
