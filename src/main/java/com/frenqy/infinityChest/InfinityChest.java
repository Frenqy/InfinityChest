package com.frenqy.infinityChest;

import com.frenqy.infinityChest.proxy.CommonProxy;
import com.frenqy.infinitychest.Tags;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION)
public class InfinityChest {

    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    @SidedProxy(clientSide = "com.frenqy.infinityChest.proxy.ClientProxy",
                serverSide = "com.frenqy.infinityChest.proxy.CommonProxy")
    public static CommonProxy proxy;

    /**
     * <a href="https://cleanroommc.com/wiki/forge-mod-development/event#overview">
     *     Take a look at how many FMLStateEvents you can listen to via the @Mod.EventHandler annotation here
     * </a>
     */
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("Hello From {}!", Tags.MOD_NAME);
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        LOGGER.info("Initializing {}!", Tags.MOD_NAME);
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        LOGGER.info("Post-initializing {}!", Tags.MOD_NAME);
        proxy.postInit(event);
    }
}
