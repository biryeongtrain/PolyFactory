package eu.pb4.polyfactory.item.block;

import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.mechanical.AxleBlock;
import eu.pb4.polyfactory.block.mechanical.source.WindmillBlock;
import eu.pb4.polyfactory.block.mechanical.source.WindmillBlockEntity;
import eu.pb4.factorytools.api.item.FireworkStarColoredItem;
import eu.pb4.factorytools.api.item.ModeledItem;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import net.minecraft.item.DyeableItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Direction;

public class WindmillSailItem extends ModeledItem implements DyeableItem, FireworkStarColoredItem {
    public WindmillSailItem(Settings settings) {
        super(Items.FIREWORK_STAR, settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        var oldState = context.getWorld().getBlockState(context.getBlockPos());
        if (oldState.isOf(FactoryBlocks.AXLE)) {
            var axis = oldState.get(AxleBlock.AXIS);
            if (axis == Direction.Axis.Y) {
                return ActionResult.FAIL;
            }

            var val = Direction.AxisDirection.POSITIVE;

            if ((axis == Direction.Axis.X && context.getPlayerYaw() > 0 && context.getPlayerYaw() < 180)
                    || (axis == Direction.Axis.Z && (context.getPlayerYaw() < -90 || context.getPlayerYaw() > 90))) {
                val = Direction.AxisDirection.NEGATIVE;
            }

            context.getWorld()
                    .setBlockState(context.getBlockPos(), FactoryBlocks.WINDMILL.getDefaultState()
                            .with(WindmillBlock.WATERLOGGED, oldState.get(AxleBlock.WATERLOGGED))
                            .with(WindmillBlock.FACING, Direction.from(axis, val)).with(WindmillBlock.SAIL_COUNT, 1));

            if (context.getWorld().getBlockEntity(context.getBlockPos()) instanceof WindmillBlockEntity be) {
                be.addSail(0, context.getStack());
            }

            return ActionResult.SUCCESS;
        } else if (oldState.isOf(FactoryBlocks.WINDMILL)) {
            var count = oldState.get(WindmillBlock.SAIL_COUNT) + 1;
            if (count > WindmillBlock.MAX_SAILS) {
                return ActionResult.FAIL;
            } else {
                var state =  oldState.with(WindmillBlock.SAIL_COUNT, count);
                context.getWorld().setBlockState(context.getBlockPos(), state);
                if (context.getWorld().getBlockEntity(context.getBlockPos()) instanceof WindmillBlockEntity be) {
                    be.addSail(count, context.getStack());

                    if (context.getPlayer() instanceof ServerPlayerEntity player) {
                        be.updateRotationalData(RotationData.State.SPECIAL, state, player.getServerWorld(), context.getBlockPos());
                        if (RotationData.State.SPECIAL.finalSpeed() > 0) {
                            TriggerCriterion.trigger(player, FactoryTriggers.CONSTRUCT_WORKING_WINDMILL);
                        }

                        RotationData.State.SPECIAL.clear();
                    }
                }
                return ActionResult.SUCCESS;
            }
        }

        return super.useOnBlock(context);
    }
    @Override
    public int getItemColor(ItemStack itemStack) {
        if (itemStack.hasNbt() && itemStack.getNbt().contains("display", NbtElement.COMPOUND_TYPE)) {
            var d = itemStack.getNbt().getCompound("display");

            if (d.contains("color", NbtElement.NUMBER_TYPE)) {
                return d.getInt("color");
            }
        }

        return 0xFFFFFF;
    }
}
