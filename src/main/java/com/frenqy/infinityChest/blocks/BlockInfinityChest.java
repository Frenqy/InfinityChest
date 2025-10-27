package com.frenqy.infinityChest.blocks;

import com.frenqy.infinityChest.InfinityChest;
import com.frenqy.infinityChest.init.ModBlocks;
import com.frenqy.infinityChest.tileentity.TileEntityInfinityChest;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.UUID;

public class BlockInfinityChest extends Block implements ITileEntityProvider {

    public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);

    public BlockInfinityChest() {
        super(Material.ROCK);
        setTranslationKey("infinity_chest");
        setRegistryName("infinity_chest");
        setCreativeTab(CreativeTabs.MISC);
        setHardness(-1.0F);
        setResistance(6000000.0F);
        setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
                                   EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote && playerIn.getHeldItem(hand).isEmpty()) {
            if(playerIn.isSneaking()){
                TileEntity te = worldIn.getTileEntity(pos);
                if (te instanceof TileEntityInfinityChest) {
                    TileEntityInfinityChest chest = (TileEntityInfinityChest) te;
                    ItemStack chestItem = new ItemStack(ModBlocks.INFINITY_CHEST_ITEM);

                    UUID chestUUID = chest.getChestUUID();
                    if (chestUUID != null) {
                        NBTTagCompound nbt = new NBTTagCompound();
                        nbt.setString("ChestUUID", chestUUID.toString());
                        chestItem.setTagCompound(nbt);
                    }

                    worldIn.setBlockToAir(pos);
                    playerIn.addItemStackToInventory(chestItem);
                    return true;
                }
            } else {
                TileEntity te = worldIn.getTileEntity(pos);
                if (te instanceof TileEntityInfinityChest) {
                    // try push item to down block
                    TileEntity blockEntityBelow = worldIn.getTileEntity(pos.down());
                    if (blockEntityBelow != null){
                        IItemHandler downHandler = blockEntityBelow.getCapability(net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP);
                        if (downHandler != null){
                            TileEntityInfinityChest chest = (TileEntityInfinityChest) te;
                            IItemHandler chestHandler = chest.getCapability(net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
                            // find first non-empty slot
                            for (int i = 0; i < chestHandler.getSlots(); i++) {
                                ItemStack stack = chestHandler.getStackInSlot(i);
                                if (!stack.isEmpty()) {
                                    ItemStack extracted = chestHandler.extractItem(i, stack.getCount(), false);
                                    if (!extracted.isEmpty()) {
                                        downHandler.insertItem(0, extracted, false);
                                        chest.markDirty();
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);

        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityInfinityChest && !worldIn.isRemote) {
            TileEntityInfinityChest chest = (TileEntityInfinityChest) te;

            UUID chestUUID = null;
            if (stack.hasTagCompound() && stack.getTagCompound().hasKey("ChestUUID")) {
                try {
                    chestUUID = UUID.fromString(stack.getTagCompound().getString("ChestUUID"));
                } catch (IllegalArgumentException e) {
                    InfinityChest.LOGGER.warn("Invalid UUID in chest item, generating new one");
                }
            }

            if (chestUUID == null) {
                chestUUID = UUID.randomUUID();
            }

            chest.setChestUUID(chestUUID);
        }
    }

    @Override
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing[] facings = {EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST};
        return this.getDefaultState().withProperty(FACING, facings[meta & 3]);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityInfinityChest();
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState blockState, World worldIn, BlockPos pos) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityInfinityChest) {
            TileEntityInfinityChest chest = (TileEntityInfinityChest) te;
            return chest.getComparatorOutput();
        }
        return 0;
    }
}
