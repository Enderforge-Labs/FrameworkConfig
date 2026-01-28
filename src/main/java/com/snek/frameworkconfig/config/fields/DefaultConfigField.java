package com.snek.frameworkconfig.config.fields;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;








public class DefaultConfigField<T> {
    private final @NotNull  T        defaultValue;
    private final @Nullable String[] description;

    public @NotNull  T        getDefault    () { return defaultValue; }
    public @Nullable String[] getDescription() { return description; }




    public DefaultConfigField(final @Nullable String[] _description, final @NotNull T _defaul) {
        description = _description;
        defaultValue = _defaul;
    }


    public JsonElement serialize(final @NotNull JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.add("description", context.serialize(description));
        obj.add("default",     context.serialize(defaultValue));
        return obj;
    }


    public static <T> DefaultConfigField<T> deserialize(final @NotNull JsonElement json, final @NotNull Class<T> classType, final @NotNull JsonDeserializationContext context) {
        final JsonObject obj = json.getAsJsonObject();
        final T val = context.deserialize(obj.get("default"), classType);
        final String[] description = obj.has("description") ? context.deserialize(obj.get("description"), String[].class) : null;
        return new DefaultConfigField<>(description, val);
    }
}