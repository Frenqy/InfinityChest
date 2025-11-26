package com.frenqy.infinitychest.tileentity;

import com.frenqy.infinitychest.data.InfinityChestData;
import com.frenqy.infinitychest.init.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TileEntityInfinityChest extends TileEntity implements ITickableTileEntity {
    public static final TileEntityType<TileEntityInfinityChest> TYPE = TileEntityType.Builder
            .of(TileEntityInfinityChest::new, ModBlocks.INFINITY_CHEST.get())
            .build(null);

    private String chestUUID;
    private final InfinityChestItemHandler itemHandler;
    private InfinityChestData chestSavedData;

    public TileEntityInfinityChest() {
        super(TYPE);
        this.itemHandler = new InfinityChestItemHandler(this);
    }

    public String getChestUUID() {
        return chestUUID;
    }

    public void setChestUUID(String uuid) {
        this.chestUUID = uuid;
        this.chestSavedData = null; // Reset cached data, will be lazy-loaded
        setChanged();
    }

    public InfinityChestData getChestData() {
        if (chestSavedData == null && chestUUID != null && this.getLevel() != null && !this.getLevel().isClientSide) {
            chestSavedData = this.getLevel().getServer().overworld().getDataStorage().computeIfAbsent(
                    () -> new InfinityChestData(chestUUID),
                    "infinity_chest_data_" + chestUUID
            );
        }
        return chestSavedData;
    }

    @Override
    public CompoundNBT save(CompoundNBT compound) {
        super.save(compound);
        if (chestUUID != null) {
            compound.putString("ChestUUID", chestUUID);
        }
        return compound;
    }

    @Override
    public void load(BlockState state, CompoundNBT compound) {
        super.load(state, compound);
        if (compound.contains("ChestUUID")) {
            this.chestUUID = compound.getString("ChestUUID");
        }
    }

    @Override
    @Nullable
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction side) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.orEmpty(capability, LazyOptional.of(() -> itemHandler));
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void tick() {
        if (this.getLevel() != null && !this.getLevel().isClientSide && this.getLevel().getGameTime() % 100 == 0) {
            InfinityChestData data = this.getChestData();
            if (data != null) {
                data.setDirty();
            }
        }
    }
}
