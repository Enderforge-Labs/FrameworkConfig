package com.snek.frameworkconfig.config.fields;

import java.lang.reflect.Type;

import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;








public class __DefaultConfigFieldAdapter<T> implements JsonSerializer<DefaultConfigField<T>>, JsonDeserializer<DefaultConfigField<T>> {
    private final Class<T> classType;


    public __DefaultConfigFieldAdapter(final @NotNull Class<T> _classType) {
        classType = _classType;
    }


    @Override
    public JsonElement serialize(final @NotNull DefaultConfigField<T> src, Type typeOfSrc, final @NotNull JsonSerializationContext context) {
        return src.serialize(context);
    }


    @Override
    public DefaultConfigField<T> deserialize(final @NotNull JsonElement json, final @NotNull Type typeOfT, final @NotNull JsonDeserializationContext context) throws JsonParseException {
        return DefaultConfigField.deserialize(json, classType, context);
    }
}
