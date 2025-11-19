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
    private static final int INITIAL_SIZE = 10; // 初始容量
    private static final int EXPAND_SIZE = 10; // 每次扩容大小

    private static InfinityChestDataManager instance;
    private Map<UUID, NonNullList<ItemStack>> chestData = new HashMap<>();
    private Map<UUID, Integer> chestSizes = new HashMap<>(); // 存储每个箱子的容量
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

        if (!chestData.containsKey(chestUUID)) {
            // 创建新的箱子数据
            chestSizes.put(chestUUID, INITIAL_SIZE);
            NonNullList<ItemStack> newItems = NonNullList.withSize(INITIAL_SIZE, ItemStack.EMPTY);
            chestData.put(chestUUID, newItems);
            saveChestData(chestUUID); // 立即保存新箱子
        }

        return chestData.get(chestUUID);
    }

    public void setChestItems(UUID chestUUID, NonNullList<ItemStack> items) {
        chestData.put(chestUUID, items);
        saveChestData(chestUUID);
    }

    public int getChestSize(UUID chestUUID) {
        return chestSizes.getOrDefault(chestUUID, INITIAL_SIZE);
    }

    public NonNullList<ItemStack> expandChestIfNeeded(UUID chestUUID) {
        NonNullList<ItemStack> items = getChestItems(chestUUID);
        int currentSize = getChestSize(chestUUID);

        // 检查是否需要扩容（当最后一个位置被占用时）
        if (currentSize > 0 && !items.get(currentSize - 1).isEmpty()) {
            int newSize = currentSize + EXPAND_SIZE;
            NonNullList<ItemStack> newItems = NonNullList.withSize(newSize, ItemStack.EMPTY);

            // 复制旧数据
            for (int i = 0; i < currentSize; i++) {
                newItems.set(i, items.get(i));
            }

            chestData.put(chestUUID, newItems);
            chestSizes.put(chestUUID, newSize);
            saveChestData(chestUUID);

            LOGGER.info("Expanded chest {} from {} to {} slots", chestUUID.toString().substring(0, 8), currentSize,
                    newSize);
            return newItems;
        }

        return items;
    }

    // 强制扩容方法，用于确保有足够空间
    public NonNullList<ItemStack> ensureCapacity(UUID chestUUID, int neededSlots) {
        NonNullList<ItemStack> items = getChestItems(chestUUID);
        int currentSize = getChestSize(chestUUID);

        if (neededSlots > currentSize) {
            int newSize = ((neededSlots - 1) / EXPAND_SIZE + 1) * EXPAND_SIZE; // 向上取整到最近的扩容单位
            NonNullList<ItemStack> newItems = NonNullList.withSize(newSize, ItemStack.EMPTY);

            // 复制旧数据
            for (int i = 0; i < Math.min(currentSize, items.size()); i++) {
                newItems.set(i, items.get(i));
            }

            chestData.put(chestUUID, newItems);
            chestSizes.put(chestUUID, newSize);
            saveChestData(chestUUID);

            LOGGER.info("Ensured capacity for chest {} from {} to {} slots", chestUUID.toString().substring(0, 8),
                    currentSize, newSize);
            return newItems;
        }

        return items;
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

                // 加载容量信息
                int size = chestTag.getInt("Size");
                if (size <= 0)
                    size = INITIAL_SIZE; // 兼容旧数据

                NonNullList<ItemStack> items = NonNullList.withSize(size, ItemStack.EMPTY);
                ContainerHelper.loadAllItems(chestTag, items, level.registryAccess());

                chestData.put(chestUUID, items);
                chestSizes.put(chestUUID, size);
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
            // 保存容量信息
            chestTag.putInt("Size", getChestSize(chestUUID));
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