package dev.endlessexpress.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.endlessexpress.EndlessExpress;
import dev.endlessexpress.config.ModConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** All persistent game data, saved as endlessexpress.json in the world folder. */
public class GameState {
    public static GameState I = new GameState();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // --- core ---
    public boolean started, journeyStarted, engineLit, trainBuilt, crashed, ffa, ended, victory;
    public long startMillis;
    public String forcedPhase = "";
    public TrainState trainState = TrainState.UNLIT;
    public int stationIndex, stateTicks, secCounter;
    public long dayTicks;
    public boolean hornSounded, breakdownPending;

    // --- train meters ---
    public double engine = 100, boiler = 100, pipe = 100, pressure = 50, ash = 0;
    public int fuel = 400, water = 500, lava = 250;
    public boolean jammed, brakeActive, greenFrozen;
    public int brakeLeft;
    public List<Integer> leaks = new ArrayList<>();
    public List<Integer> iceVents = new ArrayList<>();
    public List<Integer> lampsOut = new ArrayList<>();
    public List<Integer> dirtyDishes = new ArrayList<>();
    public int feastTicksLeft;

    // --- players ---
    public Map<UUID, Integer> lives = new HashMap<>();
    public Map<UUID, Integer> carriages = new HashMap<>();
    public Map<UUID, String> teams = new HashMap<>();
    public Map<UUID, List<String>> gifts = new HashMap<>();
    public Set<UUID> allies = new HashSet<>();
    public Set<UUID> spawned = new HashSet<>();
    public Set<UUID> attended = new HashSet<>();
    public int nextPlayerCarriage;

    // --- world ---
    public Set<Long> doneChunks = new HashSet<>();

    // --- finale ---
    public boolean bonfireBuilt, bonfireLit;
    public int bonfireX, bonfireY, bonfireZ;
    public double bonfire;
    public int waveNumber;
    public long lastWaveDay = -1;
    public int santaTicks = -1;

    public static void load(MinecraftServer server) {
        Path p = server.getWorldPath(LevelResource.ROOT).resolve("endlessexpress.json");
        try {
            if (Files.exists(p)) I = GSON.fromJson(Files.readString(p), GameState.class);
            else I = new GameState();
        } catch (Exception e) {
            EndlessExpress.LOGGER.error("Could not load game state", e);
            I = new GameState();
        }
    }

    public static void save(MinecraftServer server) {
        Path p = server.getWorldPath(LevelResource.ROOT).resolve("endlessexpress.json");
        try { Files.writeString(p, GSON.toJson(I)); }
        catch (Exception e) { EndlessExpress.LOGGER.error("Could not save game state", e); }
    }

    public int livesOf(UUID id) { return lives.getOrDefault(id, ModConfig.I.startingLives); }
}
