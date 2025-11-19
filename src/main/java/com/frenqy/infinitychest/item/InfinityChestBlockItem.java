package com.frenqy.infinitychest.item;

import com.frenqy.infinitychest.util.ChestUUIDHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import java.util.List;
import java.util.UUID;

public class InfinityChestBlockItem extends BlockItem {

    public InfinityChestBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
            TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        UUID uuid = ChestUUIDHelper.getUUIDFromItem(stack);
        if (uuid != null) {
            tooltipComponents.add(Component.literal("UUID: " + uuid.toString().substring(0, 8) + "..."));
        } else {
            tooltipComponents.add(Component.literal("UUID: Will be generated on placement"));
        }
    }
}