package com.frenqy.infinitychest.blockentity;

import com.frenqy.infinitychest.data.InfinityChestDataManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import java.util.UUID;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class InfinityChestBlockEntity extends BlockEntity implements WorldlyContainer {
    private UUID chestUUID;
    private InfinityChestDataManager dataManager;

    public InfinityChestBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.INFINITY_CHEST.get(), pos, blockState);
        this.chestUUID = UUID.randomUUID(); // 初始化时生成UUID
    }

    private InfinityChestDataManager getDataManager() {
        if (dataManager == null && level != null) {
            dataManager = InfinityChestDataManager.getInstance(level);
        }
        return dataManager;
    }

    private NonNullList<ItemStack> getItems() {
        InfinityChestDataManager manager = getDataManager();
        return manager != null ? manager.getChestItems(chestUUID) : NonNullList.withSize(27, ItemStack.EMPTY);
    }

    public UUID getChestUUID() {
        return this.chestUUID;
    }

    public void setChestUUID(UUID uuid) {
        this.chestUUID = uuid;
        this.setChanged();
    }

    @Override
    public int getContainerSize() {
        return 27;
    }

    @Override
    public boolean isEmpty() {
        NonNullList<ItemStack> items = getItems();
        for (ItemStack item : items) {
            if (!item.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return getItems().get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        NonNullList<ItemStack> items = getItems();
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (getDataManager() != null) {
            getDataManager().setChestItems(chestUUID, items);
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        NonNullList<ItemStack> items = getItems();
        ItemStack result = ContainerHelper.takeItem(items, slot);
        if (getDataManager() != null) {
            getDataManager().setChestItems(chestUUID, items);
        }
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        NonNullList<ItemStack> items = getItems();
        items.set(slot, stack);
        if (stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }
        if (getDataManager() != null) {
            getDataManager().setChestItems(chestUUID, items);
        }
        this.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        NonNullList<ItemStack> items = getItems();
        items.clear();
        if (getDataManager() != null) {
            getDataManager().setChestItems(chestUUID, items);
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

        // 加载外部数据
        if (level != null && !level.isClientSide()) {
            InfinityChestDataManager.getInstance(level).loadData();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        // 只保存UUID，物品数据由外部管理
        if (this.chestUUID != null) {
            tag.putUUID("ChestUUID", this.chestUUID);
        }
    }

    // WorldlyContainer methods for hopper interaction
    @Override
    public int[] getSlotsForFace(Direction side) {
        // Allow access to all slots from all sides
        int[] slots = new int[27];
        for (int i = 0; i < 27; i++) {
            slots[i] = i;
        }
        return slots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack itemStack, Direction direction) {
        return true; // Allow placing items from any direction
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return true; // Allow taking items from any direction
    }
}