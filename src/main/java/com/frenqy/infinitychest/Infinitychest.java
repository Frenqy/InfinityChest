package com.frenqy.infinitychest;

import com.frenqy.infinitychest.block.InfinityChestBlock;
import com.frenqy.infinitychest.blockentity.ModBlockEntities;
import com.frenqy.infinitychest.item.InfinityChestBlockItem;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Infinitychest.MODID)
public class Infinitychest {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "infinitychest";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under
    // the "infinitychest" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold Items which will all be registered under
    // the "infinitychest" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be
    // registered under the "infinitychest" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister
            .create(Registries.CREATIVE_MODE_TAB, MODID);

    // Register the InfinityChest block
    public static final DeferredBlock<Block> INFINITY_CHEST = BLOCKS.register("infinity_chest",
            () -> new InfinityChestBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5f)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()));

    // Register the InfinityChest block item
    public static final DeferredItem<BlockItem> INFINITY_CHEST_ITEM = ITEMS.register("infinity_chest",
            () -> new InfinityChestBlockItem(INFINITY_CHEST.get(), new Item.Properties()));

    // Create a creative mode tab for InfinityChest
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> INFINITY_CHEST_TAB = CREATIVE_MODE_TABS
            .register("infinity_chest_tab",
                    () -> CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.infinitychest"))
                            .withTabsBefore(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                            .icon(() -> INFINITY_CHEST_ITEM.get().getDefaultInstance())
                            .displayItems((parameters, output) -> {
                                output.accept(INFINITY_CHEST_ITEM.get());
                            }).build());

    // The constructor for the mod class is the first code that is run when your mod
    // is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and
    // pass them in automatically.
    public Infinitychest(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        // Register block entities
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class
        // (Infinitychest) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in
        // this class, like onServerStarting() below.
        // NeoForge.EVENT_BUS.register(this);

        // Register our mod's ModConfigSpec so that FML can create and load the config
        // file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // Register the creative mode tab event
        modEventBus.addListener(this::addCreative);

        // Register capabilities
        modEventBus.addListener(InfinityChestBlock::registerCapabilities);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
    }

    // Add the InfinityChest to the functional blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(INFINITY_CHEST_ITEM);
        }
    }

    // You can use EventBusSubscriber to automatically register all static methods
    // in the class annotated with @SubscribeEvent
    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
        }
    }
}
