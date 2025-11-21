package com.frenqy.infinitychest.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.frenqy.infinitychest.data.InfinityChestData;

public class InfinityChestBlockEntity extends BlockEntity {

    private UUID chestUUID;
    private InfinityChestData chestSavedData;

    private final static SavedData.Factory<InfinityChestData> factory = new SavedData.Factory<>(
            InfinityChestData::create, InfinityChestData::load);

    private IItemHandler itemHandler; // Capability 处理器

    public InfinityChestBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.INFINITY_CHEST.get(), pos, blockState);
        this.chestUUID = UUID.randomUUID(); // 初始化时生成UUID
        this.itemHandler = new InfinityChestItemHandler();
    }

    public UUID getChestUUID() {
        return this.chestUUID;
    }

    public void setChestUUID(UUID uuid) {
        this.chestUUID = uuid;
        this.setChanged();

        // 只在服务端初始化SavedData，确保 level 不为空
        if (this.getLevel() != null && !this.getLevel().isClientSide && this.getLevel().getServer() != null) {
            this.chestSavedData = this.getLevel().getServer().overworld().getDataStorage().computeIfAbsent(
                    factory,
                    "infinity_chest_data_" + chestUUID.toString());
            chestSavedData.EnsureUUID(chestUUID.toString());

            // 通知客户端数据更新
            this.getLevel().sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        // 只加载UUID，物品数据由外部管理
        if (tag.hasUUID("ChestUUID")) {
            this.chestUUID = tag.getUUID("ChestUUID");
        } else {
            this.chestUUID = UUID.randomUUID();
        }

        // 只在服务端初始化SavedData，确保 level 不为空
        if (this.getLevel() != null && !this.getLevel().isClientSide && this.getLevel().getServer() != null) {
            this.chestSavedData = this.getLevel().getServer().overworld().getDataStorage().computeIfAbsent(
                    factory,
                    "infinity_chest_data_" + chestUUID.toString());
            chestSavedData.EnsureUUID(chestUUID.toString());

            // 通知客户端数据更新
            this.getLevel().sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        // 只保存UUID，物品数据由外部管理
        if (this.chestUUID != null) {
            tag.putUUID("ChestUUID", this.chestUUID);
        }

        // 保存时确保数据被写入
        if (chestSavedData != null) {
            chestSavedData.setDirty();
        }
    }

    // 客户端同步方法
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        // 同步UUID到客户端
        if (this.chestUUID != null) {
            tag.putUUID("ChestUUID", this.chestUUID);
        }
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        // 客户端接收UUID同步
        if (tag.hasUUID("ChestUUID")) {
            this.chestUUID = tag.getUUID("ChestUUID");
        }
    }

    // 延迟初始化SavedData，当level可用时调用
    private void initializeSavedData() {
        if (this.getLevel() != null && !this.getLevel().isClientSide && this.getLevel().getServer() != null
                && this.chestUUID != null && this.chestSavedData == null) {
            this.chestSavedData = this.getLevel().getServer().overworld().getDataStorage().computeIfAbsent(
                    factory,
                    "infinity_chest_data_" + chestUUID.toString());
            chestSavedData.EnsureUUID(chestUUID.toString());
        }
    }

    public @Nullable IItemHandler getItemHandler(@Nullable net.minecraft.core.Direction side) {
        // 确保SavedData已初始化
        initializeSavedData();
        return itemHandler;
    }

    // IItemHandler 实现类
    private class InfinityChestItemHandler implements IItemHandler {

        @Override
        public int getSlots() {
            return chestSavedData != null ? chestSavedData.getCapacity() : 0;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return chestSavedData != null ? chestSavedData.getStackInSlot(slot) : ItemStack.EMPTY;
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return chestSavedData != null ? chestSavedData.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return chestSavedData != null ? chestSavedData.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return true;
        }
    }
}