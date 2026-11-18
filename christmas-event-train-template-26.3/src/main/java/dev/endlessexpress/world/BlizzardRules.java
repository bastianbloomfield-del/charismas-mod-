package dev.endlessexpress.world;

import dev.endlessexpress.compat.VersionBridge;
import dev.endlessexpress.core.GameState;
import dev.endlessexpress.player.GhostAllyManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Overworld blizzard rules: the eternal storm freezes all farmland and saplings, so crops and trees
 * cannot be planted (only the Greenhouse works), and players in the open get a frostbite overlay.
 */
public final class BlizzardRules {
    private BlizzardRules() {}

    public static InteractionResult onUse(Player player, Level world, InteractionHand hand, BlockHitResult hit) {
        if (world.isClientSide() || !(player instanceof ServerPlayer sp) || !GameState.I.started) return InteractionResult.PASS;
        if (!world.dimension().equals(Level.OVERWORLD)) return InteractionResult.PASS;
        ItemStack st = player.getItemInHand(hand);
        if (st.isEmpty()) return InteractionResult.PASS;
        boolean hoe = st.is(ItemTags.HOES);
        boolean plantable = st.getItem() instanceof BlockItem bi && (bi.getBlock() instanceof CropBlock
                || bi.getBlock() instanceof SaplingBlock || bi.getBlock() instanceof StemBlock
                || bi.getBlock() instanceof SweetBerryBushBlock || bi.getBlock() instanceof CocoaBlock);
        if (hoe || plantable) {
            VersionBridge.actionBar(sp, "The blizzard has frozen the ground solid. Only the Greenhouse can grow things.");
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    /** Once per second: light frostbite overlay (no damage) for survivors out in the open. */
    public static void tickSecond(MinecraftServer server) {
        for (ServerPlayer p : GhostAllyManager.survivors(server)) {
            if (!VersionBridge.inOverworld(p)) continue;
            if (StationLayout.inCorridor(p.getBlockX())
                    && Math.abs(p.getBlockZ() - StationLayout.stationZ(StationLayout.nearestStation(p.getBlockZ()))) <= 14) continue; // sheltered on a platform
            if (p.getTicksFrozen() < 90) p.setTicksFrozen(90);
        }
    }
}
