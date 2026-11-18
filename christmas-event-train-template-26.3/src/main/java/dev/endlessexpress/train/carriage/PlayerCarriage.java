package dev.endlessexpress.train.carriage;

import dev.endlessexpress.train.TrainBuilder;
import dev.endlessexpress.train.TrainLayout;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;

/** Personal carriage: an empty 7x5x14 interior with a bed, chest and crafting table to start. */
public final class PlayerCarriage {
    private PlayerCarriage() {}

    public static void build(ServerLevel l, int k) {
        TrainBuilder.shell(l, k);
        var foot = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH).setValue(BedBlock.PART, BedPart.FOOT);
        var head = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH).setValue(BedBlock.PART, BedPart.HEAD);
        TrainBuilder.set(l, TrainLayout.pos(k, -3, 1, 12), foot);
        TrainBuilder.set(l, TrainLayout.pos(k, -3, 1, 13), head);
        TrainBuilder.set(l, TrainLayout.pos(k, 3, 1, 13), Blocks.CHEST.defaultBlockState());
        TrainBuilder.set(l, TrainLayout.pos(k, 3, 1, 12), Blocks.CRAFTING_TABLE.defaultBlockState());
        TrainBuilder.set(l, TrainLayout.pos(k, 0, 5, 7), TrainBuilder.hangingLantern());
    }
}
