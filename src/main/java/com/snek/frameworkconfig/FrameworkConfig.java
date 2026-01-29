package com.snek.frameworkconfig;

import java.nio.file.Path;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;




public class FrameworkConfig implements ModInitializer {
    public static final String LIB_ID = "frameworkconfig";
    public static final @NotNull Logger LOGGER = LoggerFactory.getLogger(LIB_ID);
    public static final ResourceLocation PHASE_ID = new ResourceLocation(LIB_ID, "phase_id");



    private static @Nullable MinecraftServer serverInstance = null;
    public  static @NotNull  MinecraftServer getServer() {
        return serverInstance;
    }

    /**
     * Computes the path to the data storage directory for the specified mod.
     * @param modId The ID of the mod.
     * @return The path to the storage directory.
     */
    public static @NotNull Path getStorageDir(final @NotNull String modId) {
        return FrameworkConfig.getServer().getWorldPath(LevelResource.ROOT).resolve("data/" + modId);
    }

    /**
     * Computes the path to the config directory for the specified mod.
     * @param modId The ID of the mod.
     * @return The path to the config directory.
     */
    public static @NotNull Path getConfigDir(final @NotNull String modId) {
        return FabricLoader.getInstance().getConfigDir().resolve(modId);
    }



    @SuppressWarnings("java:S2696") //! Assigning static values from a non-static method
    @Override
    public void onInitialize() {
        LOGGER.info("FrameworkConfig loaded :3");


        // Register initialization
        ServerLifecycleEvents.SERVER_STARTING.register(PHASE_ID, server -> {
            serverInstance = server;
        });
    }
}
