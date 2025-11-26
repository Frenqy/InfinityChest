package com.frenqy.infinitychest.item;

import com.frenqy.infinitychest.init.ModBlocks;
import com.frenqy.infinitychest.init.ModItems;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.util.text.StringTextComponent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class ItemInfinityChest extends BlockItem {

    public ItemInfinityChest() {
        super(ModBlocks.INFINITY_CHEST.get(), new Properties().tab(ModItems.TAB));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World worldIn, List<ITextComponent> textComponents, ITooltipFlag tooltipFlag) {
        super.appendHoverText(stack, worldIn, textComponents, tooltipFlag);

        if (stack.hasTag() && stack.getTag().contains("ChestUUID")) {
            try {
                UUID uuid = UUID.fromString(stack.getTag().getString("ChestUUID"));
                String shortUUID = uuid.toString().substring(0, 8);
                textComponents.add(new StringTextComponent("UUID: " + shortUUID));
            } catch (IllegalArgumentException e) {
                // 忽略无效UUID
            }
        }
    }
}
