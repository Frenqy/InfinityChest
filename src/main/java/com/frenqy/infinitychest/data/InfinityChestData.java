package com.frenqy.infinitychest.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

public class InfinityChestData extends SavedData {

    private static final int INITIAL_CAPACITY = 10;
    private static final int EXPAND_SIZE = 10;

    private String uuid;
    private List<ItemStack> items;
    private int capacity;

    public static InfinityChestData create() {
        InfinityChestData savedData = new InfinityChestData();
        savedData.capacity = INITIAL_CAPACITY;
        savedData.items = new ArrayList<>();
        for (int i = 0; i < INITIAL_CAPACITY; i++) {
            savedData.items.add(ItemStack.EMPTY);
        }
        return savedData;
    }

    @Override
    public CompoundTag save(CompoundTag compoundTag, HolderLookup.Provider provider) {
        compoundTag.putInt("capacity", capacity);
        compoundTag.putString("uuid", uuid);

        ListTag itemList = new ListTag(Tag.TAG_COMPOUND);
        for (ItemStack itemStack : items) {
            if (!itemStack.isEmpty()) {
                Tag itemTag = itemStack.save(provider);
                itemList.add(itemTag);
            }
        }
        compoundTag.put("items", itemList);

        return compoundTag;
    }

    public static InfinityChestData load(CompoundTag compoundTag, HolderLookup.Provider provider) {
        String uuid = null;
        if (compoundTag.contains("uuid")) {
            uuid = compoundTag.getString("uuid");
        }
        int capacity = INITIAL_CAPACITY;
        if (compoundTag.contains("capacity")) {
            capacity = compoundTag.getInt("capacity");
        }

        InfinityChestData savedData = new InfinityChestData();
        savedData.uuid = uuid;
        savedData.capacity = capacity;

        if (compoundTag.contains("items")) {
            ListTag itemList = compoundTag.getList("items", Tag.TAG_COMPOUND);
            savedData.items = new ArrayList<>();
            for (int i = 0; i < itemList.size(); i++) {
                CompoundTag itemTag = itemList.getCompound(i);
                ItemStack itemStack = ItemStack.parseOptional(provider, itemTag);
                savedData.items.add(itemStack);
            }
        }

        while (savedData.items.size() < capacity) {
            savedData.items.add(ItemStack.EMPTY);
        }

        return savedData;
    }

    public void EnsureUUID(String uuid) {
        this.uuid = uuid;
    }

    private void expandCapacity() {
        capacity += EXPAND_SIZE;

        // 添加新的空槽位
        while (items.size() < capacity) {
            items.add(ItemStack.EMPTY);
        }

        setDirty();
    }

    public int getCapacity() {
        return capacity;
    }

    public ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= items.size()) {
            return ItemStack.EMPTY;
        }
        return items.get(slot);
    }

    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty())
            return ItemStack.EMPTY;

        // 如果插入一个物品后槽位超出当前容量，先扩容
        if (getTotalUsedSlots() + 1 >= capacity) {
            expandCapacity();
        }

        // 尝试插入到任何可用槽位
        for (int i = 0; i < capacity; i++) {
            ItemStack existing = items.get(i);
            if (existing.isEmpty()) {
                if (!simulate) {
                    items.set(i, stack.copy());
                }
                return ItemStack.EMPTY;
            } else if (ItemStack.isSameItemSameComponents(existing, stack)) {
                int maxSize = Math.min(stack.getMaxStackSize(), 64);
                int canInsert = maxSize - existing.getCount();
                if (canInsert > 0) {
                    int toInsert = Math.min(canInsert, stack.getCount());
                    if (!simulate) {
                        existing.setCount(existing.getCount() + toInsert);
                    }
                    ItemStack remainder = stack.copy();
                    remainder.setCount(stack.getCount() - toInsert);
                    return remainder.isEmpty() ? ItemStack.EMPTY : remainder;
                }
            }
        }

        return stack;
    }

    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= items.size() || amount <= 0) {
            return ItemStack.EMPTY;
        }

        ItemStack existing = items.get(slot);
        if (existing.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int toExtract = Math.min(amount, existing.getCount());
        ItemStack result = existing.copy();
        result.setCount(toExtract);

        if (!simulate) {
            if (toExtract >= existing.getCount()) {
                items.set(slot, ItemStack.EMPTY);
            } else {
                existing.setCount(existing.getCount() - toExtract);
            }
            setDirty();
        }

        return result;
    }

    public int getTotalUsedSlots() {
        int used = 0;
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                used++;
            }
        }
        return used;
    }
}
