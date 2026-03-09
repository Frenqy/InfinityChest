package com.frenqy.infinitychest.tileentity;

import com.frenqy.infinitychest.data.InfinityChestData;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.UUID;

public class TileEntityInfinityChest extends TileEntity implements ITickable {

    private UUID chestUUID;
    private InfinityChestItemHandler itemHandler;

    public TileEntityInfinityChest() {
        this.itemHandler = new InfinityChestItemHandler(this);
    }

    public UUID getChestUUID() {
        return chestUUID;
    }

    public void setChestUUID(UUID uuid) {
        this.chestUUID = uuid;
        this.itemHandler.setChestUUID(uuid);
        markDirty();
    }

    public InfinityChestData getChestData() {
        if (chestUUID == null)
            return null;
        return InfinityChestData.getOrCreate(world, chestUUID);
    }

    @Override
    public void update() {
        // 定期保存数据
        if (!world.isRemote && world.getTotalWorldTime() % 100 == 0) {
            InfinityChestData data = getChestData();
            if (data != null) {
                data.markDirty();
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (chestUUID != null) {
            compound.setString("ChestUUID", chestUUID.toString());
        }
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("ChestUUID")) {
            try {
                this.chestUUID = UUID.fromString(compound.getString("ChestUUID"));
                this.itemHandler.setChestUUID(this.chestUUID);
            } catch (IllegalArgumentException e) {
                this.chestUUID = null;
            }
        }
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Override
    @Nullable
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(itemHandler);
        }
        return super.getCapability(capability, facing);
    }

    public int getComparatorOutput() {
        InfinityChestData data = getChestData();
        if (data == null)
            return 0;

        int totalItems = data.getTotalItemCount();
        int capacity = data.getCapacity();

        if (totalItems == 0)
            return 0;
        if (capacity == 0)
            return 15;

        return Math.min(15, 1 + (totalItems * 14) / capacity);
    }
}
