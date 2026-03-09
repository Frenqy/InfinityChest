package com.frenqy.infinitychest.tileentity;

import com.frenqy.infinitychest.data.InfinityChestData;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import java.util.UUID;

public class InfinityChestItemHandler implements IItemHandler {

    private final TileEntityInfinityChest tileEntity;
    private UUID chestUUID;

    public InfinityChestItemHandler(TileEntityInfinityChest tileEntity) {
        this.tileEntity = tileEntity;
    }

    public void setChestUUID(UUID uuid) {
        this.chestUUID = uuid;
    }

    private InfinityChestData getChestData() {
        if (chestUUID == null || tileEntity.getWorld() == null)
            return null;
        return InfinityChestData.getOrCreate(tileEntity.getWorld(), chestUUID);
    }

    @Override
    public int getSlots() {
        InfinityChestData data = getChestData();
        return data != null ? data.getCapacity() : 10;
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        InfinityChestData data = getChestData();
        if (data == null || slot < 0 || slot >= data.getCapacity()) {
            return ItemStack.EMPTY;
        }
        return data.getStackInSlot(slot);
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (stack.isEmpty())
            return ItemStack.EMPTY;

        InfinityChestData data = getChestData();
        if (data == null)
            return stack;

        // 尝试插入物品，如果容量不足则自动扩容
        ItemStack result = data.insertItem(slot, stack, simulate);

        if (!simulate) {
            tileEntity.markDirty();
        }

        return result;
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        InfinityChestData data = getChestData();
        if (data == null || slot < 0 || slot >= data.getCapacity()) {
            return ItemStack.EMPTY;
        }

        ItemStack result = data.extractItem(slot, amount, simulate);

        if (!simulate && !result.isEmpty()) {
            tileEntity.markDirty();
        }

        return result;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }
}
