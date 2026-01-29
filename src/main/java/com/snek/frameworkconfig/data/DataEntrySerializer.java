package com.snek.frameworkconfig.data;

import org.jetbrains.annotations.NotNull;




public abstract class DataEntrySerializer<T extends DataEntry> {

    /**
     * Serializes a DataEntry.
     * @param data The data to serialize.
     * @return The serialized data as a string.
     */
    public abstract @NotNull String serialize(final @NotNull T data);


    /**
     * Creates a new DataEntry by deserializing a string.
     * @param string The string to deserialize.
     *     This is expected to be a serialized representation of an instance of the data, created using {@link #serialize(T)}.
     */
    public abstract @NotNull T deserialize(final @NotNull String string);
}
