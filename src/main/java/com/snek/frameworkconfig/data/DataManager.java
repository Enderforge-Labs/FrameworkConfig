package com.snek.frameworkconfig.data;

import org.jetbrains.annotations.NotNull;




public abstract class DataManager {
    private final DataSerializer<?> serializer;
    private final String MOD_ID;


    protected DataManager(final @NotNull DataSerializer<?> serializer, final @NotNull String MOD_ID) {
        this.serializer = serializer;
        this.MOD_ID = MOD_ID;
    }

}
