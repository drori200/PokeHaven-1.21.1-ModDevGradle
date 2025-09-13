package net.havencore.pokehaven.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LeverBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;

@GameTestHolder("pokehaven")
@PrefixGameTestTemplate(value = false)
public class ExampleGameTests {

    @GameTest
    public static void flatworld(GameTestHelper helper) {
        var lever = helper.relativePos(new BlockPos(1, 1, 1));
        var lamp  = helper.relativePos(new BlockPos(1, 1, 2));

        // Flip the lever (simplest is to set the state directly)
        helper.setBlock(lever, net.minecraft.world.level.block.Blocks.LEVER.defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED, true));

        // Wait a couple ticks and assert the lamp is lit
        helper.runAtTickTime(2, () -> {
            var state = helper.getBlockState(lamp);
            if (state.getBlock() == net.minecraft.world.level.block.Blocks.REDSTONE_LAMP &&
                    state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)) {
                helper.succeed();
            } else {
                helper.fail("Lamp did not light after lever powered!");
            }
        });
    }
}
