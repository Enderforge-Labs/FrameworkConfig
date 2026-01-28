package com.snek.frameworkconfig.data;

import org.jetbrains.annotations.NotNull;

public interface DataSerializer<T> {
    public @NotNull String serialize(final @NotNull      T data);
    public @NotNull T    deserialize(final @NotNull String data);
}
