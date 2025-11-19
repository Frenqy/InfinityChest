package com.frenqy.infinitychest;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
@EventBusSubscriber(modid = Infinitychest.MODID)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Add your config options here

    static final ModConfigSpec SPEC = BUILDER.build();

    // Add your config fields here

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // Load config values here
    }
}
