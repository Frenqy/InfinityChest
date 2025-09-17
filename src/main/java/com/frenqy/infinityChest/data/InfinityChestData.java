package com.frenqy.infinityChest.data;

import com.frenqy.infinityChest.InfinityChest;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class InfinityChestData extends WorldSavedData {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String DATA_NAME = "infinity_chest_data";
    private static final int INITIAL_CAPACITY = 10;
    private static final int EXPAND_SIZE = 10;

    private UUID chestUUID;
    private List<ItemStack> items;
    private int capacity;

    public InfinityChestData() {
        super(DATA_NAME);
        this.items = new ArrayList<>();
        this.capacity = INITIAL_CAPACITY;
        initializeSlots();
    }

    public InfinityChestData(String name) {
        super(name);
        this.items = new ArrayList<>();
        this.capacity = INITIAL_CAPACITY;
        initializeSlots();
    }

    public InfinityChestData(UUID uuid) {
        super(DATA_NAME + "_" + uuid.toString());
        this.chestUUID = uuid;
        this.items = new ArrayList<>();
        this.capacity = INITIAL_CAPACITY;
        initializeSlots();
    }

    private void initializeSlots() {
        while (items.size() < capacity) {
            items.add(ItemStack.EMPTY);
        }
    }

    public static InfinityChestData getOrCreate(World world, UUID uuid) {
        if (world.isRemote) return null;

        String dataName = DATA_NAME + "_" + uuid.toString();
        InfinityChestData data = (InfinityChestData) world.getMapStorage().getOrLoadData(InfinityChestData.class, dataName);

        if (data == null) {
            data = new InfinityChestData(uuid);
            world.getMapStorage().setData(dataName, data);
        }

        return data;
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
        if (stack.isEmpty()) return ItemStack.EMPTY;

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
            } else if (existing.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(existing, stack)) {
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
            markDirty();
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

        InfinityChest.LOGGER.info("Expanded infinity chest {} capacity from {} to {}",
                                 chestUUID, oldCapacity, capacity);
        markDirty();
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
    public void readFromNBT(NBTTagCompound nbt) {
        if (nbt.hasKey("ChestUUID")) {
            try {
                this.chestUUID = UUID.fromString(nbt.getString("ChestUUID"));
            } catch (IllegalArgumentException e) {
                InfinityChest.LOGGER.error("Invalid UUID in chest data: {}", nbt.getString("ChestUUID"));
            }
        }

        this.capacity = nbt.getInteger("Capacity");
        if (this.capacity <= 0) {
            this.capacity = INITIAL_CAPACITY;
        }

        this.items.clear();
        NBTTagList itemList = nbt.getTagList("Items", Constants.NBT.TAG_COMPOUND);

        // 确保有足够的槽位
        while (items.size() < capacity) {
            items.add(ItemStack.EMPTY);
        }

        for (int i = 0; i < itemList.tagCount(); i++) {
            NBTTagCompound itemNBT = itemList.getCompoundTagAt(i);
            int slot = itemNBT.getInteger("Slot");

            if (slot >= 0 && slot < capacity) {
                items.set(slot, new ItemStack(itemNBT));
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        if (chestUUID != null) {
            compound.setString("ChestUUID", chestUUID.toString());
        }

        compound.setInteger("Capacity", capacity);

        NBTTagList itemList = new NBTTagList();
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                NBTTagCompound itemNBT = new NBTTagCompound();
                itemNBT.setInteger("Slot", i);
                stack.writeToNBT(itemNBT);
                itemList.appendTag(itemNBT);
            }
        }
        compound.setTag("Items", itemList);

        return compound;
    }
}
