package com.snek.frameworkconfig;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.ResourceLocation;




public class FrameworkConfig implements ModInitializer {
    public static final String LIB_ID = "frameworkconfig";
    public static final @NotNull Logger LOGGER = LoggerFactory.getLogger(LIB_ID);
    public static final ResourceLocation PHASE_ID = new ResourceLocation(LIB_ID, "phase_id");


    @Override
    public void onInitialize() {
        System.out.println("FrameworkConfig loaded :3");


        // Register initialization
        ServerLifecycleEvents.SERVER_STARTING.register(PHASE_ID, server -> {
            // Empty
        });
    }
}
