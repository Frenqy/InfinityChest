package com.frenqy.infinitychest.blockentity;

import com.frenqy.infinitychest.data.InfinityChestDataManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InfinityChestBlockEntity extends BlockEntity implements Container {
    private UUID chestUUID;
    private InfinityChestDataManager dataManager;
    private List<ItemStack> cachedItems; // 缓存物品列表
    private int cachedSize = -1; // 缓存容器大小
    private IItemHandler itemHandler; // Capability 处理器

    public InfinityChestBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.INFINITY_CHEST.get(), pos, blockState);
        this.chestUUID = UUID.randomUUID(); // 初始化时生成UUID
        this.itemHandler = new InfinityChestItemHandler();
    }

    private InfinityChestDataManager getDataManager() {
        if (dataManager == null && level != null) {
            dataManager = InfinityChestDataManager.getInstance(level);
        }
        return dataManager;
    }

    private List<ItemStack> getItems() {
        if (cachedItems == null) {
            InfinityChestDataManager manager = getDataManager();
            if (manager != null) {
                cachedItems = manager.getChestItems(chestUUID);
            } else {
                cachedItems = new ArrayList<>();
                for (int i = 0; i < 10; i++) {
                    cachedItems.add(ItemStack.EMPTY);
                }
            }
        }
        return cachedItems;
    }

    private void invalidateCache() {
        cachedItems = null;
        cachedSize = -1;
    }

    // 检查是否需要扩容
    private void checkAndExpand() {
        InfinityChestDataManager manager = getDataManager();
        if (manager == null)
            return;

        List<ItemStack> items = getItems();
        int currentSize = manager.getChestSize(chestUUID);

        // 检查最后一个槽位是否被占用
        if (currentSize > 0 && currentSize <= items.size() && !items.get(currentSize - 1).isEmpty()) {
            items = manager.expandChestIfNeeded(chestUUID);
            invalidateCache(); // 清除缓存以反映新的容器大小
        }
    }

    // 智能添加物品的方法，支持自动扩容
    public ItemStack insertItem(ItemStack stack) {
        if (stack.isEmpty())
            return ItemStack.EMPTY;

        List<ItemStack> items = getItems();
        InfinityChestDataManager manager = getDataManager();
        boolean needsUpdate = false;

        // 首先尝试合并到现有物品堆
        for (int i = 0; i < items.size() && !stack.isEmpty(); i++) {
            ItemStack existing = items.get(i);
            if (ItemStack.isSameItemSameComponents(existing, stack)
                    && existing.getCount() < existing.getMaxStackSize()) {
                int space = existing.getMaxStackSize() - existing.getCount();
                int toAdd = Math.min(space, stack.getCount());
                existing.grow(toAdd);
                stack.shrink(toAdd);
                needsUpdate = true;
            }
        }

        // 寻找空位置
        for (int i = 0; i < items.size() && !stack.isEmpty(); i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, stack.copy());
                stack = ItemStack.EMPTY;
                needsUpdate = true;
                break;
            }
        }

        // 如果仍有物品未插入，尝试扩容
        if (!stack.isEmpty() && manager != null) {
            items = manager.expandChestIfNeeded(chestUUID);
            invalidateCache(); // 清除缓存

            // 在新扩容的位置放置物品
            for (int i = 0; i < items.size() && !stack.isEmpty(); i++) {
                if (items.get(i).isEmpty()) {
                    items.set(i, stack.copy());
                    stack = ItemStack.EMPTY;
                    needsUpdate = true;
                    break;
                }
            }
        }

        if (needsUpdate && manager != null) {
            manager.setChestItems(chestUUID, items);
        }

        return stack;
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
        if (cachedSize == -1) {
            InfinityChestDataManager manager = getDataManager();
            cachedSize = manager != null ? manager.getChestSize(chestUUID) : 10;
        }
        return cachedSize;
    }

    @Override
    public boolean isEmpty() {
        List<ItemStack> items = getItems();
        for (ItemStack item : items) {
            if (!item.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        List<ItemStack> items = getItems();
        if (slot >= items.size()) {
            return ItemStack.EMPTY;
        }
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        List<ItemStack> items = getItems();
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            InfinityChestDataManager manager = getDataManager();
            if (manager != null) {
                manager.setChestItems(chestUUID, items);
            }
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        List<ItemStack> items = getItems();
        // 为ContainerHelper创建临时NonNullList
        NonNullList<ItemStack> tempList = NonNullList.create();
        tempList.addAll(items);
        ItemStack result = ContainerHelper.takeItem(tempList, slot);

        // 更新原列表但不保存
        for (int i = 0; i < tempList.size() && i < items.size(); i++) {
            items.set(i, tempList.get(i));
        }

        return result; // No update版本不需要立即保存
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        List<ItemStack> items = getItems();

        // 检查是否需要扩容
        if (slot >= items.size()) {
            return; // 不允许超出当前容量
        }

        if (stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }

        items.set(slot, stack);

        InfinityChestDataManager manager = getDataManager();
        if (manager != null) {
            manager.setChestItems(chestUUID, items);
            // 检查并执行扩容（当第10个槽位被填满时）
            checkAndExpand();
        }
        this.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        List<ItemStack> items = getItems();
        items.replaceAll(ignored -> ItemStack.EMPTY); // 更高效的清空方式

        InfinityChestDataManager manager = getDataManager();
        if (manager != null) {
            manager.setChestItems(chestUUID, items);
        }
        invalidateCache();
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

        // 清除缓存，强制重新加载
        invalidateCache();

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

        // 保存时确保数据被写入
        InfinityChestDataManager manager = getDataManager();
        if (manager != null && cachedItems != null) {
            manager.saveChestDataImmediately(chestUUID);
        }
    }

    public @Nullable IItemHandler getItemHandler(@Nullable net.minecraft.core.Direction side) {
        return itemHandler;
    }

    // IItemHandler 实现类
    private class InfinityChestItemHandler implements IItemHandler {

        @Override
        public int getSlots() {
            return getContainerSize();
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            if (slot < 0 || slot >= getSlots()) {
                return ItemStack.EMPTY;
            }
            return getItem(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            if (slot < 0 || slot >= getSlots()) {
                return stack;
            }

            ItemStack existing = getItem(slot);

            if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, stack)) {
                return stack;
            }

            int maxStackSize = Math.min(getSlotLimit(slot), stack.getMaxStackSize());
            int currentCount = existing.getCount();
            int insertCount = Math.min(stack.getCount(), maxStackSize - currentCount);

            if (insertCount <= 0) {
                return stack;
            }

            if (!simulate) {
                if (existing.isEmpty()) {
                    ItemStack newStack = stack.copy();
                    newStack.setCount(insertCount);
                    setItem(slot, newStack);
                } else {
                    existing.grow(insertCount);
                    setItem(slot, existing);
                }
            }

            ItemStack remainder = stack.copy();
            remainder.shrink(insertCount);
            return remainder;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (amount <= 0 || slot < 0 || slot >= getSlots()) {
                return ItemStack.EMPTY;
            }

            ItemStack existing = getItem(slot);
            if (existing.isEmpty()) {
                return ItemStack.EMPTY;
            }

            int extractCount = Math.min(amount, existing.getCount());
            ItemStack extracted = existing.copy();
            extracted.setCount(extractCount);

            if (!simulate) {
                if (extractCount >= existing.getCount()) {
                    setItem(slot, ItemStack.EMPTY);
                } else {
                    existing.shrink(extractCount);
                    setItem(slot, existing);
                }
            }

            return extracted;
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