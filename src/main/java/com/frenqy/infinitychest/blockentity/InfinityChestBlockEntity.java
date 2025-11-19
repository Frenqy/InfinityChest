package com.frenqy.infinitychest.blockentity;

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
    private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
    private UUID chestUUID;

    public InfinityChestBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.INFINITY_CHEST.get(), pos, blockState);
        this.chestUUID = UUID.randomUUID(); // 初始化时生成UUID
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
        for (ItemStack item : this.items) {
            if (!item.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ContainerHelper.removeItem(this.items, slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        if (stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }
        this.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items, registries);

        // 加载UUID
        if (tag.hasUUID("ChestUUID")) {
            this.chestUUID = tag.getUUID("ChestUUID");
        } else {
            this.chestUUID = UUID.randomUUID(); // 如果没有UUID，生成一个新的
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);

        // 保存UUID
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