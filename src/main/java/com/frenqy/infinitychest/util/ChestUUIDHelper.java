package com.frenqy.infinitychest.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import java.util.UUID;

public class ChestUUIDHelper {
    private static final String UUID_TAG = "ChestUUID";

    public static UUID getUUIDFromItem(ItemStack stack) {
        if (stack.isEmpty() || !stack.has(DataComponents.CUSTOM_DATA)) {
            return null;
        }

        CompoundTag tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
        return tag != null && tag.hasUUID(UUID_TAG) ? tag.getUUID(UUID_TAG) : null;
    }

    public static void setUUIDToItem(ItemStack stack, UUID uuid) {
        if (uuid != null && !stack.isEmpty()) {
            CompoundTag tag = stack.has(DataComponents.CUSTOM_DATA)
                    ? stack.get(DataComponents.CUSTOM_DATA).copyTag()
                    : new CompoundTag();
            tag.putUUID(UUID_TAG, uuid);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    public static boolean hasUUID(ItemStack stack) {
        return !stack.isEmpty() && stack.has(DataComponents.CUSTOM_DATA)
                && stack.get(DataComponents.CUSTOM_DATA).copyTag().hasUUID(UUID_TAG);
    }
}