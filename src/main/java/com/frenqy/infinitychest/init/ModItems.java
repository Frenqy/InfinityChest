package com.frenqy.infinitychest.init;

import com.frenqy.infinitychest.Infinitychest;
import com.frenqy.infinitychest.item.ItemInfinityChest;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Infinitychest.MOD_ID);

    public static ItemGroup TAB = new ItemGroup(Infinitychest.MOD_ID) {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(ModBlocks.INFINITY_CHEST.get());
        }
    };

    public static RegistryObject<BlockItem> INFINITY_CHEST_ITEM = ITEMS.register("infinity_chest", ItemInfinityChest::new);
}
