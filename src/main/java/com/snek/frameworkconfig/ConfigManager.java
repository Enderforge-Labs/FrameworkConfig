package com.snek.frameworkconfig;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.snek.frameworkconfig.fields.ConstrainedConfigField;
import com.snek.frameworkconfig.fields.DefaultConfigField;
import com.snek.frameworkconfig.fields.ValueConfigField;
import com.snek.frameworkconfig.fields.__ValueConfigFieldAdapter;
import com.snek.frameworkconfig.fields.__ConstrainedConfigFieldAdapter;
import com.snek.frameworkconfig.fields.__DefaultConfigFieldAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import net.fabricmc.loader.api.FabricLoader;








public abstract class ConfigManager {
    private ConfigManager() {}


    // Define a custom Gson to handle ConfigField logic
    private static final @NotNull Gson gson = new GsonBuilder()

        // Configure gson
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .generateNonExecutableJson()


        // Register defaulted value adapters
        .registerTypeAdapter( new TypeToken<DefaultConfigField<Long   >>() {}.getType(), new __DefaultConfigFieldAdapter<>(Long   .class))
        .registerTypeAdapter( new TypeToken<DefaultConfigField<Integer>>() {}.getType(), new __DefaultConfigFieldAdapter<>(Integer.class))
        .registerTypeAdapter( new TypeToken<DefaultConfigField<Double >>() {}.getType(), new __DefaultConfigFieldAdapter<>(Double .class))
        .registerTypeAdapter( new TypeToken<DefaultConfigField<Float  >>() {}.getType(), new __DefaultConfigFieldAdapter<>(Float  .class))
        .registerTypeAdapter( new TypeToken<DefaultConfigField<Boolean>>() {}.getType(), new __DefaultConfigFieldAdapter<>(Boolean.class))
        .registerTypeAdapter( new TypeToken<DefaultConfigField<String >>() {}.getType(), new __DefaultConfigFieldAdapter<>(String .class))


        // Register defaulted value array adapters
        .registerTypeAdapter( new TypeToken<DefaultConfigField<Long   []>>() {}.getType(), new __DefaultConfigFieldAdapter<>(Long   [].class))
        .registerTypeAdapter( new TypeToken<DefaultConfigField<Integer[]>>() {}.getType(), new __DefaultConfigFieldAdapter<>(Integer[].class))
        .registerTypeAdapter( new TypeToken<DefaultConfigField<Double []>>() {}.getType(), new __DefaultConfigFieldAdapter<>(Double [].class))
        .registerTypeAdapter( new TypeToken<DefaultConfigField<Float  []>>() {}.getType(), new __DefaultConfigFieldAdapter<>(Float  [].class))
        .registerTypeAdapter( new TypeToken<DefaultConfigField<Boolean[]>>() {}.getType(), new __DefaultConfigFieldAdapter<>(Boolean[].class))
        .registerTypeAdapter( new TypeToken<DefaultConfigField<String []>>() {}.getType(), new __DefaultConfigFieldAdapter<>(String [].class))


        // Register value adapters
        .registerTypeAdapter( new TypeToken<ValueConfigField<Long   >>() {}.getType(), new __ValueConfigFieldAdapter<>(Long   .class))
        .registerTypeAdapter( new TypeToken<ValueConfigField<Integer>>() {}.getType(), new __ValueConfigFieldAdapter<>(Integer.class))
        .registerTypeAdapter( new TypeToken<ValueConfigField<Double >>() {}.getType(), new __ValueConfigFieldAdapter<>(Double .class))
        .registerTypeAdapter( new TypeToken<ValueConfigField<Float  >>() {}.getType(), new __ValueConfigFieldAdapter<>(Float  .class))
        .registerTypeAdapter( new TypeToken<ValueConfigField<Boolean>>() {}.getType(), new __ValueConfigFieldAdapter<>(Boolean.class))
        .registerTypeAdapter( new TypeToken<ValueConfigField<String >>() {}.getType(), new __ValueConfigFieldAdapter<>(String .class))


        // Register value array adapters
        .registerTypeAdapter( new TypeToken<ValueConfigField<Long   []>>() {}.getType(), new __ValueConfigFieldAdapter<>(Long   [].class))
        .registerTypeAdapter( new TypeToken<ValueConfigField<Integer[]>>() {}.getType(), new __ValueConfigFieldAdapter<>(Integer[].class))
        .registerTypeAdapter( new TypeToken<ValueConfigField<Double []>>() {}.getType(), new __ValueConfigFieldAdapter<>(Double [].class))
        .registerTypeAdapter( new TypeToken<ValueConfigField<Float  []>>() {}.getType(), new __ValueConfigFieldAdapter<>(Float  [].class))
        .registerTypeAdapter( new TypeToken<ValueConfigField<Boolean[]>>() {}.getType(), new __ValueConfigFieldAdapter<>(Boolean[].class))
        .registerTypeAdapter( new TypeToken<ValueConfigField<String []>>() {}.getType(), new __ValueConfigFieldAdapter<>(String [].class))


        // Register constrained adapters (Arrays, bools and defaulted values cannot be constrained)
        .registerTypeAdapter(new TypeToken<ConstrainedConfigField<Long   >>() {}.getType(),new __ConstrainedConfigFieldAdapter<>(Long.   class))
        .registerTypeAdapter(new TypeToken<ConstrainedConfigField<Integer>>() {}.getType(),new __ConstrainedConfigFieldAdapter<>(Integer.class))
        .registerTypeAdapter(new TypeToken<ConstrainedConfigField<Double >>() {}.getType(),new __ConstrainedConfigFieldAdapter<>(Double. class))
        .registerTypeAdapter(new TypeToken<ConstrainedConfigField<Float  >>() {}.getType(),new __ConstrainedConfigFieldAdapter<>(Float.  class))
    .create();








    /**
     * Loads a configuration file from the mod's config directory.
     * <p> This MUST be called on ServerLifecycleEvents.SERVER_STARTING before anything else.
     * <p> Any errors during loading will cause the mod to not start. //TODO update this
     * @param configName The name of the file.
     * @param configClass The class of the config file data.
     * @return The config file instance.
     */
    public static <T extends ConfigFile> @Nullable T loadConfig(final @NotNull String configName, final @NotNull Class<T> configClass, final @NotNull String MOD_ID) {


        // Read file if it exists
        final Path configDir = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
        final File configPath = configDir.resolve(configName + ".json").toFile();
        if(configPath.exists()) {
            try(JsonReader reader = new JsonReader(new FileReader(configPath))) {
                reader.setLenient(false);
                final T r = gson.fromJson(reader, configClass);
                r.validate();
                return r;
            } catch (final IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        // Create default config and save it if the config file is missing
        else {
            try {
                final T r = configClass.getDeclaredConstructor().newInstance();
                if(!saveConfig(configName, r, MOD_ID)) return null;
                return r;
            } catch(InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                e.printStackTrace();
                return null;
            }
        }
    }




    public static boolean saveConfig(final @NotNull String configName, final @NotNull ConfigFile config, final @NotNull String MOD_ID) {

        final Path configDir = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
        final File configPath = configDir.resolve(configName + ".json").toFile();

        try {
            Files.createDirectories(configDir);
            try(JsonWriter writer = new JsonWriter(new FileWriter(configPath))) {
                writer.setIndent("    ");
                writer.setLenient(false);
                gson.toJson(config, config.getClass(), writer);
            }
            return true;
        } catch(IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
