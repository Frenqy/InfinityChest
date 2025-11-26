package com.frenqy.infinitychest.init;

import com.frenqy.infinitychest.Infinitychest;
import com.frenqy.infinitychest.block.BlockInfinityChest;
import com.frenqy.infinitychest.item.ItemInfinityChest;
import com.frenqy.infinitychest.tileentity.TileEntityInfinityChest;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Infinitychest.MOD_ID);
    public static final DeferredRegister<TileEntityType<?>> TILE_ENTITIES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, Infinitychest.MOD_ID);

    public static RegistryObject<Block> INFINITY_CHEST = BLOCKS.register("infinity_chest", BlockInfinityChest::new);

    public static RegistryObject<TileEntityType<TileEntityInfinityChest>> INFINITY_CHEST_TE = TILE_ENTITIES.register("infinity_chest", () -> TileEntityInfinityChest.TYPE);
}
