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
    private final Map<UUID, NonNullList<ItemStack>> chestData = new HashMap<>();
    private final Map<UUID, Integer> chestSizes = new HashMap<>(); // 存储每个箱子的容量
    private final Map<UUID, Boolean> dirtyChests = new HashMap<>(); // 跟踪需要保存的箱子
    private Level level;
    private File dataDir; // 缓存数据目录

    public static InfinityChestDataManager getInstance(Level level) {
        if (instance == null) {
            instance = new InfinityChestDataManager();
        }
        instance.level = level;
        return instance;
    }

    public NonNullList<ItemStack> getChestItems(UUID chestUUID) {
        NonNullList<ItemStack> items = chestData.get(chestUUID);
        if (items == null) {
            loadChestData(chestUUID);
            items = chestData.get(chestUUID);

            if (items == null) {
                // 创建新的箱子数据
                items = NonNullList.withSize(INITIAL_SIZE, ItemStack.EMPTY);
                chestData.put(chestUUID, items);
                chestSizes.put(chestUUID, INITIAL_SIZE);
                markDirty(chestUUID);
            }
        }
        return items;
    }

    public void setChestItems(UUID chestUUID, NonNullList<ItemStack> items) {
        chestData.put(chestUUID, items);
        markDirty(chestUUID);
    }

    private void markDirty(UUID chestUUID) {
        dirtyChests.put(chestUUID, true);
    }

    public int getChestSize(UUID chestUUID) {
        return chestSizes.getOrDefault(chestUUID, INITIAL_SIZE);
    }

    public NonNullList<ItemStack> expandChestIfNeeded(UUID chestUUID) {
        NonNullList<ItemStack> items = getChestItems(chestUUID);
        int currentSize = getChestSize(chestUUID);

        // 检查是否需要扩容（当最后一个位置被占用时）
        if (currentSize > 0 && currentSize <= items.size() && !items.get(currentSize - 1).isEmpty()) {
            int newSize = currentSize + EXPAND_SIZE;

            // 原地扩容：直接添加新的空槽位
            for (int i = currentSize; i < newSize; i++) {
                items.add(ItemStack.EMPTY);
            }

            chestSizes.put(chestUUID, newSize);
            markDirty(chestUUID);

            LOGGER.debug("Expanded chest {} from {} to {} slots", chestUUID.toString().substring(0, 8), currentSize,
                    newSize);
            return items;
        }

        return items;
    }

    public void removeChestData(UUID chestUUID) {
        chestData.remove(chestUUID);
        chestSizes.remove(chestUUID);
        dirtyChests.remove(chestUUID);
        deleteChestData(chestUUID);
    }

    public void loadData() {
        if (level == null || level.isClientSide())
            return;

        if (dataDir == null) {
            File worldDir = level.getServer().getServerDirectory().toFile();
            dataDir = new File(worldDir, "data");
            dataDir.mkdirs();
            LOGGER.info("InfinityChest data directory initialized: {}", dataDir.getAbsolutePath());
        }
    }

    public void saveData() {
        if (level == null || level.isClientSide())
            return;

        // 只保存被标记为脏的数据
        dirtyChests.entrySet().removeIf(entry -> {
            if (entry.getValue()) {
                saveChestData(entry.getKey());
            }
            return entry.getValue();
        });
    }

    // 即时保存单个箱子数据
    public void saveChestDataImmediately(UUID chestUUID) {
        saveChestData(chestUUID);
        dirtyChests.remove(chestUUID);
    }

    private void loadChestData(UUID chestUUID) {
        if (level == null || level.isClientSide())
            return;

        try {
            if (dataDir == null) {
                loadData(); // 初始化dataDir
            }
            File dataFile = new File(dataDir, DATA_PREFIX + chestUUID + DATA_SUFFIX);

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
            if (dataDir == null) {
                loadData(); // 初始化dataDir
            }
            File dataFile = new File(dataDir, DATA_PREFIX + chestUUID + DATA_SUFFIX);

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
            if (dataDir == null) {
                loadData(); // 初始化dataDir
            }
            File dataFile = new File(dataDir, DATA_PREFIX + chestUUID + DATA_SUFFIX);

            if (dataFile.exists() && !dataFile.delete()) {
                LOGGER.warn("Failed to delete chest data file: " + dataFile.getAbsolutePath());
            }
        } catch (Exception e) {
            LOGGER.error("Error deleting chest data for UUID: " + chestUUID, e);
        }
    }
}