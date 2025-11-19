package com.frenqy.infinitychest.blockentity;

import com.frenqy.infinitychest.Infinitychest;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
            .create(Registries.BLOCK_ENTITY_TYPE, Infinitychest.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InfinityChestBlockEntity>> INFINITY_CHEST = BLOCK_ENTITIES
            .register("infinity_chest", () -> BlockEntityType.Builder.of(InfinityChestBlockEntity::new,
                    Infinitychest.INFINITY_CHEST.get()).build(null));
}