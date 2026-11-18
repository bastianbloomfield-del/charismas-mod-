package dev.endlessexpress;

import dev.endlessexpress.command.EeCommands;
import dev.endlessexpress.compat.VersionBridge;
import dev.endlessexpress.config.ModConfig;
import dev.endlessexpress.core.GameState;
import dev.endlessexpress.core.TrainSchedule;
import dev.endlessexpress.finale.*;
import dev.endlessexpress.player.GhostAllyManager;
import dev.endlessexpress.player.LivesManager;
import dev.endlessexpress.player.TeamManager;
import dev.endlessexpress.portal.StationPortal;
import dev.endlessexpress.portal.TrainExitPortal;
import dev.endlessexpress.task.TaskManager;
import dev.endlessexpress.task.TaskType;
import dev.endlessexpress.train.HullProtection;
import dev.endlessexpress.train.TrainBuilder;
import dev.endlessexpress.train.TrainStatus;
import dev.endlessexpress.train.carriage.BoilerCarriage;
import dev.endlessexpress.train.carriage.DiningCarriage;
import dev.endlessexpress.ui.ActionBarHud;
import dev.endlessexpress.world.FakeScenery;
import dev.endlessexpress.world.SnowController;
import dev.endlessexpress.world.SpawnManager;
import dev.endlessexpress.world.TrackGenerator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Endless Express - server-side Fabric mod (Geyser/Floodgate friendly: no client content). */
public class EndlessExpress implements ModInitializer {
    public static final String MOD_ID = "endlessexpress";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModConfig.load();
        TaskManager.init();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            GameState.load(server);
            TrainBuilder.ensureBase(server);
            GhostAllyManager.setupTeams(server);
            LOGGER.info("Endless Express ready (train built: {}).", GameState.I.trainBuilt);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(GameState::save);

        ServerChunkEvents.CHUNK_LOAD.register(TrackGenerator::onChunkLoad);
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> EeCommands.register(dispatcher));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> SpawnManager.onJoin(handler.getPlayer()));
        UseBlockCallback.EVENT.register(TaskManager::onUse);
        UseBlockCallback.EVENT.register(dev.endlessexpress.world.BlizzardRules::onUse);

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer sp) LivesManager.onDeath(sp);
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> LivesManager.onRespawn(newPlayer));

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, be) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer sp) || !GameState.I.started) return true;
            if (world.dimension().equals(VersionBridge.TRAIN_DIM)) {
                TaskManager.Task t = TaskManager.TASKS.get(pos);
                if (t != null && t.type() == TaskType.VENT && state.is(Blocks.BLUE_ICE)) {
                    BoilerCarriage.clearIce((ServerLevel) world, sp, t.index());
                    return false;
                }
                if (HullProtection.active() && !dev.endlessexpress.train.CarriageManager.canModify(sp, pos)) {
                    VersionBridge.actionBar(sp, "This is another player's carriage.");
                    return false;
                }
                if (HullProtection.active() && (HullProtection.isHull(pos) || t != null)) {
                    VersionBridge.actionBar(sp, "The train is reinforced - it cannot be broken yet.");
                    return false;
                }
                return true;
            }
            if (world.dimension().equals(Level.OVERWORLD) && Bonfire.near(pos)) {
                if (GhostAllyManager.isAlly(sp) || Bonfire.isFeedPos(pos)) return false;
            }
            return true;
        });

        ServerTickEvents.END_SERVER_TICK.register(EndlessExpress::tick);
    }

    private static void tick(MinecraftServer server) {
        GameState s = GameState.I;
        if (!s.started) return;
        int t = server.getTickCount();
        s.dayTicks++;

        TrainSchedule.tick(server);
        FakeScenery.tick(server);
        SnowController.tick(server);
        SantaSleigh.tick(server);
        if (t % 4 == 0) { StationPortal.tick(server); TrainExitPortal.tick(server); }
        if (s.dayTicks % 24000 == 0 && dev.endlessexpress.core.EventClock.beforeFinale()) TeamManager.assign(server);
        if (t % 100 == 0) TeamManager.tickBuffs(server);

        if (t % 20 == 0) {
            TrainStatus.tickSecond(server);
            DiningCarriage.tickSecond(server);
            Bonfire.tickSecond(server);
            WaveManager.tickSecond(server);
            FfaManager.tickSecond(server);
            SnowController.tickSecond(server);
            dev.endlessexpress.world.BlizzardRules.tickSecond(server);
            ActionBarHud.tickSecond(server);
            if (t % 40 == 0) TrainSchedule.strandedTick(server);
        }
        if (t % 6000 == 0) GameState.save(server);
    }
}
