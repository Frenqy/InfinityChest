package com.frenqy.infinitychest.block;

import com.frenqy.infinitychest.Infinitychest;
import com.frenqy.infinitychest.init.ModBlocks;
import com.frenqy.infinitychest.init.ModItems;
import com.frenqy.infinitychest.tileentity.TileEntityInfinityChest;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.UUID;

public class BlockInfinityChest extends Block {

    public BlockInfinityChest() {
        super(Properties.of(Material.STONE).strength(-1.0F, 6000000.0F).noOcclusion());
        //this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    @Override
    public ActionResultType use(BlockState state, World worldIn, BlockPos pos, PlayerEntity playerIn, Hand hand, BlockRayTraceResult hit) {
        if (!worldIn.isClientSide && playerIn.getMainHandItem().isEmpty()){
            if (playerIn.isCrouching()){
                TileEntity te = worldIn.getBlockEntity(pos);
                if (te instanceof TileEntityInfinityChest) {
                    TileEntityInfinityChest chest = (TileEntityInfinityChest) te;
                    ItemStack chestItem = new ItemStack(ModItems.INFINITY_CHEST_ITEM.get());

                    String chestUUID = chest.getChestUUID();
                    if (chestUUID != null) {
                        CompoundNBT nbt = new CompoundNBT();
                        nbt.putString("ChestUUID", chestUUID);
                        chestItem.setTag(nbt);
                    }

                    worldIn.removeBlock(pos, false);
                    playerIn.addItem(chestItem);
                    return ActionResultType.SUCCESS;
                }
            } else {
                TileEntity te = worldIn.getBlockEntity(pos);
                if (te instanceof TileEntityInfinityChest) {
                    // try push item to down block
                    TileEntity blockEntityBelow = worldIn.getBlockEntity(pos.below());
                    if (blockEntityBelow != null) {
                        IItemHandler downHandler = blockEntityBelow.getCapability(
                                net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, Direction.UP).orElse(null);
                        if (downHandler != null) {
                            TileEntityInfinityChest chest = (TileEntityInfinityChest) te;
                            IItemHandler chestHandler = chest.getCapability(
                                    net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, Direction.DOWN).orElse(null);
                            // find first non-empty slot
                            for (int i = 0; i < chestHandler.getSlots(); i++) {
                                ItemStack stack = chestHandler.getStackInSlot(i);
                                if (!stack.isEmpty()) {
                                    ItemStack extracted = chestHandler.extractItem(i, stack.getCount(), false);
                                    if (!extracted.isEmpty()) {
                                        downHandler.insertItem(0, extracted, false);
                                        chest.setChanged();
                                        return ActionResultType.SUCCESS;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return ActionResultType.PASS;
    }

    @Override
    public void setPlacedBy(World worldIn, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(worldIn, pos, state, placer, stack);

        TileEntity te = worldIn.getBlockEntity(pos);
        if (te instanceof TileEntityInfinityChest && !worldIn.isClientSide) {
            TileEntityInfinityChest chest = (TileEntityInfinityChest) te;

            String chestUUID = null;
            if (stack.hasTag() && stack.getTag().contains("ChestUUID")) {
                try {
                    chestUUID = stack.getTag().getString("ChestUUID");
                } catch (IllegalArgumentException e) {
                    Infinitychest.LOGGER.warn("Invalid UUID in chest item, generating new one");
                }
            }

            if (chestUUID == null) {
                chestUUID = UUID.randomUUID().toString();
            }

            chest.setChestUUID(chestUUID);
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        Direction direction = context.getHorizontalDirection().getOpposite();
        return this.defaultBlockState();//.setValue(FACING, direction);
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new TileEntityInfinityChest();
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return false;
    }
}
