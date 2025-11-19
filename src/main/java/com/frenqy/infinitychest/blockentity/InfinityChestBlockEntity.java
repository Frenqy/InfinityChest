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
        return manager != null ? manager.getChestItems(chestUUID) : NonNullList.withSize(10, ItemStack.EMPTY);
    }

    // 智能添加物品的方法，支持自动扩容
    public ItemStack insertItem(ItemStack stack) {
        if (stack.isEmpty())
            return ItemStack.EMPTY;

        NonNullList<ItemStack> items = getItems();

        // 首先尝试合并到现有物品堆
        for (int i = 0; i < items.size(); i++) {
            ItemStack existing = items.get(i);
            if (ItemStack.isSameItemSameComponents(existing, stack)
                    && existing.getCount() < existing.getMaxStackSize()) {
                int space = existing.getMaxStackSize() - existing.getCount();
                int toAdd = Math.min(space, stack.getCount());
                existing.grow(toAdd);
                stack.shrink(toAdd);

                if (getDataManager() != null) {
                    getDataManager().setChestItems(chestUUID, items);
                }

                if (stack.isEmpty())
                    return ItemStack.EMPTY;
            }
        }

        // 寻找空位置
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, stack.copy());

                if (getDataManager() != null) {
                    getDataManager().setChestItems(chestUUID, items);
                }

                return ItemStack.EMPTY;
            }
        }

        // 如果没有空位，尝试扩容
        if (getDataManager() != null) {
            items = getDataManager().expandChestIfNeeded(chestUUID);

            // 在新扩容的位置放置物品
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).isEmpty()) {
                    items.set(i, stack.copy());
                    getDataManager().setChestItems(chestUUID, items);
                    return ItemStack.EMPTY;
                }
            }
        }

        return stack; // 无法插入
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
        InfinityChestDataManager manager = getDataManager();
        return manager != null ? manager.getChestSize(chestUUID) : 10;
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
        NonNullList<ItemStack> items = getItems();
        if (slot >= items.size()) {
            return ItemStack.EMPTY;
        }
        return items.get(slot);
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

        // 检查是否需要扩容
        if (slot >= items.size()) {
            return; // 不允许超出当前容量
        }

        items.set(slot, stack);
        if (stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }

        if (getDataManager() != null) {
            // 尝试扩容（如果需要）
            items = getDataManager().expandChestIfNeeded(chestUUID);
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
        int size = getContainerSize();
        int[] slots = new int[size];
        for (int i = 0; i < size; i++) {
            slots[i] = i;
        }
        return slots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack itemStack, Direction direction) {
        // 对于漏斗等自动化设备，我们使用智能插入而不是固定索引
        return !itemStack.isEmpty();
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return index < getContainerSize(); // 只允许从有效范围内取出物品
    }

    // 重写以支持智能插入
    public boolean insertItemFromHopper(ItemStack stack) {
        if (stack.isEmpty())
            return false;

        ItemStack remaining = insertItem(stack.copy());
        int inserted = stack.getCount() - remaining.getCount();

        if (inserted > 0) {
            stack.shrink(inserted);
            return true;
        }

        return false;
    }
}