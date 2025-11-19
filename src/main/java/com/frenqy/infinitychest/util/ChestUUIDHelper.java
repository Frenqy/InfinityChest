package com.frenqy.infinitychest.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import java.util.UUID;

public class ChestUUIDHelper {
    private static final String UUID_TAG = "ChestUUID";

    public static UUID getUUIDFromItem(ItemStack stack) {
        if (stack.has(DataComponents.CUSTOM_DATA)) {
            CompoundTag tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
            if (tag != null && tag.hasUUID(UUID_TAG)) {
                return tag.getUUID(UUID_TAG);
            }
        }
        return null;
    }

    public static void setUUIDToItem(ItemStack stack, UUID uuid) {
        if (uuid != null) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID(UUID_TAG, uuid);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    public static boolean hasUUID(ItemStack stack) {
        return getUUIDFromItem(stack) != null;
    }
}