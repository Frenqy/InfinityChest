package com.frenqy.infinitychest.data;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InfinityChestDataManager {
    private static final String DATA_PREFIX = "InfinityChest_";
    private static final String DATA_SUFFIX = ".dat";
    private static final Logger LOGGER = LoggerFactory.getLogger(InfinityChestDataManager.class);
    private static InfinityChestDataManager instance;
    private Map<UUID, NonNullList<ItemStack>> chestData = new HashMap<>();
    private Level level;

    public static InfinityChestDataManager getInstance(Level level) {
        if (instance == null) {
            instance = new InfinityChestDataManager();
        }
        instance.level = level;
        return instance;
    }

    public NonNullList<ItemStack> getChestItems(UUID chestUUID) {
        if (!chestData.containsKey(chestUUID)) {
            loadChestData(chestUUID);
        }
        return chestData.computeIfAbsent(chestUUID, k -> NonNullList.withSize(27, ItemStack.EMPTY));
    }

    public void setChestItems(UUID chestUUID, NonNullList<ItemStack> items) {
        chestData.put(chestUUID, items);
        saveChestData(chestUUID);
    }

    public void removeChestData(UUID chestUUID) {
        chestData.remove(chestUUID);
        deleteChestData(chestUUID);
    }

    public void loadData() {
        // 不再需要预加载所有数据，数据将在需要时动态加载
        if (level == null || level.isClientSide())
            return;

        File worldDir = level.getServer().getServerDirectory().toFile();
        File dataDir = new File(worldDir, "data");
        dataDir.mkdirs();

        LOGGER.info("InfinityChest data directory initialized: " + dataDir.getAbsolutePath());
    }

    public void saveData() {
        if (level == null || level.isClientSide())
            return;

        // 保存所有在内存中的数据
        for (UUID chestUUID : chestData.keySet()) {
            saveChestData(chestUUID);
        }
    }

    private void loadChestData(UUID chestUUID) {
        if (level == null || level.isClientSide())
            return;

        try {
            File worldDir = level.getServer().getServerDirectory().toFile();
            File dataDir = new File(worldDir, "data");
            File dataFile = new File(dataDir, DATA_PREFIX + chestUUID.toString() + DATA_SUFFIX);

            if (dataFile.exists()) {
                CompoundTag chestTag = NbtIo.readCompressed(dataFile.toPath(),
                        net.minecraft.nbt.NbtAccounter.unlimitedHeap());
                NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
                ContainerHelper.loadAllItems(chestTag, items, level.registryAccess());
                chestData.put(chestUUID, items);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load chest data for UUID: " + chestUUID, e);
        }
    }

    private void saveChestData(UUID chestUUID) {
        if (level == null || level.isClientSide())
            return;

        NonNullList<ItemStack> items = chestData.get(chestUUID);
        if (items == null)
            return;

        try {
            File worldDir = level.getServer().getServerDirectory().toFile();
            File dataDir = new File(worldDir, "data");
            dataDir.mkdirs();
            File dataFile = new File(dataDir, DATA_PREFIX + chestUUID.toString() + DATA_SUFFIX);

            CompoundTag chestTag = new CompoundTag();
            ContainerHelper.saveAllItems(chestTag, items, level.registryAccess());
            NbtIo.writeCompressed(chestTag, dataFile.toPath());
        } catch (IOException e) {
            LOGGER.error("Failed to save chest data for UUID: " + chestUUID, e);
        }
    }

    private void deleteChestData(UUID chestUUID) {
        if (level == null || level.isClientSide())
            return;

        try {
            File worldDir = level.getServer().getServerDirectory().toFile();
            File dataDir = new File(worldDir, "data");
            File dataFile = new File(dataDir, DATA_PREFIX + chestUUID.toString() + DATA_SUFFIX);

            if (dataFile.exists() && !dataFile.delete()) {
                LOGGER.warn("Failed to delete chest data file: " + dataFile.getAbsolutePath());
            }
        } catch (Exception e) {
            LOGGER.error("Error deleting chest data for UUID: " + chestUUID, e);
        }
    }
}