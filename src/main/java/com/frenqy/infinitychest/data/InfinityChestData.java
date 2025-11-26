package com.frenqy.infinitychest.data;

import com.frenqy.infinitychest.Infinitychest;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import java.util.*;

public class InfinityChestData extends WorldSavedData {

    private static final String DATA_NAME = "infinity_chest_data";
    private static final int INITIAL_CAPACITY = 10;
    private static final int EXPAND_SIZE = 10;

    private String chestUUID;
    private final List<ItemStack> items;
    private int capacity;

    public InfinityChestData(String chestUUID) {
        super(DATA_NAME + "_" + chestUUID);
        this.chestUUID = chestUUID;
        this.items = new ArrayList<>();
        this.capacity = INITIAL_CAPACITY;
        initializeSlots();
    }

    private void initializeSlots() {
        while (items.size() < capacity) {
            items.add(ItemStack.EMPTY);
        }
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

    @Nonnull
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
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
            } else if (ItemStack.isSame(existing, stack)) {
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

    @Nonnull
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

    private void expandCapacity() {
        int oldCapacity = capacity;
        capacity += EXPAND_SIZE;

        // 添加新的空槽位
        while (items.size() < capacity) {
            items.add(ItemStack.EMPTY);
        }

        Infinitychest.LOGGER.info("Expanded infinity chest {} capacity from {} to {}",
                chestUUID, oldCapacity, capacity);
        setDirty();
    }

    public int getTotalItemCount() {
        int count = 0;
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                count += stack.getCount();
            }
        }
        return count;
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

    @Override
    public void load(CompoundNBT nbt) {
        if (nbt.contains("ChestUUID")) {
            try {
                this.chestUUID = nbt.getString("ChestUUID");
            } catch (IllegalArgumentException e) {
                Infinitychest.LOGGER.error("Invalid UUID in chest data: {}", nbt.getString("ChestUUID"));
            }
        }

        this.capacity = nbt.getInt("Capacity");
        if (this.capacity <= 0) {
            this.capacity = INITIAL_CAPACITY;
        }

        this.items.clear();
        ListNBT itemList = nbt.getList("Items", Constants.NBT.TAG_COMPOUND);

        for (int i = 0; i < itemList.size(); i++) {
            CompoundNBT itemNBT = itemList.getCompound(i);
            ItemStack stack = ItemStack.of(itemNBT);
            items.add(stack);
        }

        while (items.size() < capacity) {
            items.add(ItemStack.EMPTY);
        }
    }

    @Override
    public CompoundNBT save(CompoundNBT compound) {
        if (chestUUID != null) {
            compound.putString("ChestUUID", chestUUID);
        }

        compound.putInt("Capacity", capacity);

        ListNBT itemList = new ListNBT();
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                CompoundNBT itemNBT = new CompoundNBT();
                stack.save(itemNBT);
                itemList.add(itemNBT);
            }
        }

        compound.put("Items", itemList);

        return compound;
    }
}
