package com.snek.frameworkconfig;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.ResourceLocation;




public class FrameworkConfig implements ModInitializer {
    public static final String LIB_ID = "frameworkconfig";
    public static final ResourceLocation INIT_PHASE_ID = new ResourceLocation(LIB_ID, "init");


    @Override
    public void onInitialize() {
        System.out.println("FrameworkConfig loaded :3");


        // Register initialization
        ServerLifecycleEvents.SERVER_STARTING.register(INIT_PHASE_ID, server -> {
            // Empty
        });
    }
}
