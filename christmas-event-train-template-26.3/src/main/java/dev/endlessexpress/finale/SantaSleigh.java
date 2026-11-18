package dev.endlessexpress.finale;

import dev.endlessexpress.compat.VersionBridge;
import dev.endlessexpress.core.GameState;
import dev.endlessexpress.player.GhostAllyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.concurrent.ThreadLocalRandom;

/** Co-op victory: Santa's sleigh descends through the storm with fireworks and rescue chests. */
public final class SantaSleigh {
    private SantaSleigh() {}
    private static final int START_HEIGHT = 60, LAND_TICKS = 2 * 52, END_TICKS = 700;

    public static void start(MinecraftServer server) {
        GameState s = GameState.I;
        if (s.victory) return;
        s.victory = true;
        s.santaTicks = 0;
        WaveManager.clearAll(server);
        VersionBridge.broadcast(server, "\u00a7a\u00a7lThe Bonfire is at 100%! Santa's Sleigh descends through the storm!");
        for (ServerPlayer p : GhostAllyManager.survivors(server)) VersionBridge.title(p, "SANTA IS HERE", "You are saved");
    }

    public static void tick(MinecraftServer server) {
        GameState s = GameState.I;
        if (!s.victory || s.santaTicks < 0 || s.ended) return;
        ServerLevel ow = VersionBridge.overworld(server);
        int t = s.santaTicks++;
        int x = s.bonfireX + 8, z = s.bonfireZ;
        if (t <= LAND_TICKS && t % 2 == 0) {
            int y = s.bonfireY + START_HEIGHT - t / 2;
            draw(ow, x, y + 1, z, false);
            draw(ow, x, y, z, true);
        }
        if (t == LAND_TICKS) placeChests(server, ow, x, s.bonfireY, z);
        if (t > LAND_TICKS && t % 20 == 0) firework(server, s);
        if (t >= END_TICKS) {
            s.ended = true;
            VersionBridge.broadcast(server, "\u00a76\u00a7lCO-OP VICTORY! All remaining survivors are rescued. Merry Christmas!");
            for (ServerPlayer p : GhostAllyManager.survivors(server)) VersionBridge.title(p, "VICTORY", "Santa saved you all");
        }
    }

    private static void draw(ServerLevel l, int cx, int y, int cz, boolean on) {
        BlockState body = on ? Blocks.SPRUCE_PLANKS.defaultBlockState() : Blocks.AIR.defaultBlockState();
        BlockState rail = on ? Blocks.RED_WOOL.defaultBlockState() : Blocks.AIR.defaultBlockState();
        BlockState runner = on ? Blocks.GOLD_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState();
        for (int dx = -3; dx <= 3; dx++) for (int dz = -1; dz <= 1; dz++) {
            l.setBlock(new BlockPos(cx + dx, y, cz + dz), body, 2);
            if (Math.abs(dz) == 1 || dx == -3) l.setBlock(new BlockPos(cx + dx, y + 1, cz + dz), rail, 2);
        }
        for (int dx = -3; dx <= 3; dx++) { l.setBlock(new BlockPos(cx + dx, y - 1, cz - 1), runner, 2); l.setBlock(new BlockPos(cx + dx, y - 1, cz + 1), runner, 2); }
        l.setBlock(new BlockPos(cx - 3, y + 2, cz), on ? Blocks.LANTERN.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
    }

    private static void placeChests(MinecraftServer server, ServerLevel ow, int x, int baseY, int z) {
        String items = "{Items:[{Slot:0b,id:\"minecraft:golden_apple\",count:8},{Slot:1b,id:\"minecraft:cooked_beef\",count:32},"
                + "{Slot:2b,id:\"minecraft:diamond\",count:6},{Slot:3b,id:\"minecraft:totem_of_undying\",count:1}]}";
        for (int i = 0; i < 6; i++) {
            int cx = x - 6 + i * 2, cz = z + 6;
            ow.getChunk(cx >> 4, cz >> 4);
            VersionBridge.runCommand(server, "setblock " + cx + " " + (baseY) + " " + cz + " minecraft:chest" + items);
        }
    }

    private static void firework(MinecraftServer server, GameState s) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int[] colors = {16711680, 65280, 16776960, 255, 16711935};
        int c = colors[r.nextInt(colors.length)];
        double x = s.bonfireX + r.nextInt(-12, 13), z = s.bonfireZ + r.nextInt(-12, 13);
        VersionBridge.runCommand(server, "summon minecraft:firework_rocket " + x + " " + (s.bonfireY + 2) + " " + z
                + " {LifeTime:30,FireworksItem:{id:\"minecraft:firework_rocket\",count:1,components:{\"minecraft:fireworks\":"
                + "{explosions:[{shape:\"large_ball\",colors:[I;" + c + "],has_twinkle:true}],flight_duration:2}}}}");
    }
}
