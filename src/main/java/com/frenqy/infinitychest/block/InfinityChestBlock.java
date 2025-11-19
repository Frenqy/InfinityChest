package com.frenqy.infinitychest.block;

import com.frenqy.infinitychest.blockentity.InfinityChestBlockEntity;
import com.frenqy.infinitychest.util.ChestUUIDHelper;
import net.minecraft.core.BlockPos;

import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.UUID;

public class InfinityChestBlock extends BaseEntityBlock {
    public static final MapCodec<InfinityChestBlock> CODEC = simpleCodec(InfinityChestBlock::new);

    public InfinityChestBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InfinityChestBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof InfinityChestBlockEntity chestEntity) {
            // 检查物品是否有UUID
            UUID existingUUID = ChestUUIDHelper.getUUIDFromItem(stack);
            if (existingUUID != null) {
                chestEntity.setChestUUID(existingUUID);
            }
            // 如果没有UUID，使用BlockEntity初始化时生成的UUID
        }
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);

        // 获取BlockEntity并将UUID添加到掉落物品
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof InfinityChestBlockEntity chestEntity) {
            UUID chestUUID = chestEntity.getChestUUID();

            // 为每个掉落的箱子物品添加UUID
            for (ItemStack drop : drops) {
                if (drop.getItem() == this.asItem() && chestUUID != null) {
                    ChestUUIDHelper.setUUIDToItem(drop, chestUUID);
                }
            }

            // 如果没有通过战利品表掉落箱子，手动创建一个
            boolean hasChestDrop = drops.stream().anyMatch(drop -> drop.getItem() == this.asItem());
            if (!hasChestDrop) {
                ItemStack chestDrop = new ItemStack(this.asItem());
                if (chestUUID != null) {
                    ChestUUIDHelper.setUUIDToItem(chestDrop, chestUUID);
                }
                drops.add(chestDrop);
            }
        }

        return drops;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof InfinityChestBlockEntity) {
                // 不掉落箱子内物品，保持数据在外部文件中
                // 但箱子本身需要通过战利品表正常掉落
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // 静态方法用于注册 capability
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                com.frenqy.infinitychest.blockentity.ModBlockEntities.INFINITY_CHEST.get(),
                (blockEntity, side) -> ((InfinityChestBlockEntity) blockEntity).getItemHandler(side));
    }
}