package com.frenqy.infinitychest.items;

import com.frenqy.infinitychest.init.ModBlocks;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import java.util.UUID;

public class ItemInfinityChest extends ItemBlock {

    public ItemInfinityChest() {
        super(ModBlocks.INFINITY_CHEST_BLOCK);
        setTranslationKey("infinity_chest");
        setRegistryName("infinity_chest");
        setCreativeTab(CreativeTabs.MISC);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String baseName = super.getItemStackDisplayName(stack);

        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("ChestUUID")) {
            try {
                UUID uuid = UUID.fromString(stack.getTagCompound().getString("ChestUUID"));
                String shortUUID = uuid.toString().substring(0, 8);
                return baseName + " [" + shortUUID + "]";
            } catch (IllegalArgumentException e) {
                // 忽略无效UUID
            }
        }

        return baseName;
    }
}
