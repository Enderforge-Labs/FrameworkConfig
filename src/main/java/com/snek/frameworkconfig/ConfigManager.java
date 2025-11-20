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
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
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
    public static final @NotNull String MARKER_FILE_NAME = ":3";
    public static final @NotNull String MARKER_FILE_CONTENTS = "🦊";


    // Error messages
    public static final @NotNull String ERROR_MODE_LOAD_DEFAULT = "Loading default configuration.";
    public static final @NotNull String ERROR_MODE_SKIP_WRITE = "Skipping config file creation to preserve the existing data.";


    public static final @NotNull String ERROR_UNREADABLE_FILE  =
        "Config file \"%s\" could not be read. " +
        "Either delete the config file or fix it. FrameworkConfig will NOT do that for you. " +
        ERROR_MODE_LOAD_DEFAULT + " " + ERROR_MODE_SKIP_WRITE
    ;
    public static final @NotNull String ERROR_INVALID_SETTINGS =
        "Config file \"%s\" contains invalid settings. " +
        "Either delete the config file or ensure the values are within the valid ranges. FrameworkConfig will NOT do that for you. " +
        ERROR_MODE_LOAD_DEFAULT + " " + ERROR_MODE_SKIP_WRITE
    ;
    public static final @NotNull String ERROR_UNUSABLE_DIR =
        "Config directory \"%s\" exists, but it was not created by FrameworkConfig for the mod \"%s\". " +
        "This likely indicates that other mods are trying to store config files in the same location. " +
        ERROR_MODE_LOAD_DEFAULT + " " + ERROR_MODE_SKIP_WRITE
    ;
    public static final @NotNull String ERROR_UNWRITABLE_FILE =
        "Could not write config file \"%s\". " +
        ERROR_MODE_LOAD_DEFAULT
    ;




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
     * Prints an error and loads the default configs if the directory exists but wasn't created by FrameworkConfig.
     * <p> This MUST be called on ServerLifecycleEvents.SERVER_STARTING before anything else.
     * @param configName The name of the file.
     * @param configClass The class of the config file data.
     * @param MOD_ID The ID of the mod.This must be unique for each mod on the server.
     * @return The config file instance.
     */
    public static <T extends ConfigFile> @Nullable T loadConfig(final @NotNull String configName, final @NotNull Class<T> configClass, final @NotNull String MOD_ID) {


        // Compute paths
        final Path configDir = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
        final File configPath = configDir.resolve(configName + ".json").toFile();
        final boolean configFilePresent = configPath.exists();


        // Check config directory
        boolean configDirUsable = true;
        try {
            checkConfigDir(configDir);
        } catch(IOException e) {
            FrameworkConfig.LOGGER.error(ERROR_UNUSABLE_DIR.formatted(configDir.toString(), MOD_ID));
            e.printStackTrace();
            configDirUsable = false;
        }


        // Read file if it exists
        if(configDirUsable && configFilePresent) {
            try(JsonReader reader = new JsonReader(new FileReader(configPath))) {
                reader.setLenient(false);
                T r = null;

                // Try to read JSON file
                try {
                    r = gson.fromJson(reader, configClass);
                    if(r == null) throw new JsonSyntaxException("Could not read JSON file.");
                } catch(final JsonIOException | JsonSyntaxException e) {
                    FrameworkConfig.LOGGER.error(ERROR_UNREADABLE_FILE.formatted(configName));
                    e.printStackTrace();
                }

                // Validate read configs if they are not null, return if valid
                if(r != null) {
                    try {
                        r.validate();
                        return r;
                    } catch(final IllegalStateException e) {
                        FrameworkConfig.LOGGER.error(ERROR_INVALID_SETTINGS.formatted(configName));
                        e.printStackTrace();
                    }
                }

            } catch(final IOException e) {
                FrameworkConfig.LOGGER.error(ERROR_UNREADABLE_FILE.formatted(configName));
                e.printStackTrace();
            }
        }



        // If something went wrong or the config file is not readable, create the default config
        // If the config directory is usable and the file doesn't already exist, try to save it to file
        try {
            final T r = configClass.getDeclaredConstructor().newInstance();
            if(configDirUsable && !configFilePresent) {
                saveConfig(configName, r, MOD_ID);
            }
            return r;
        } catch(InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }
    }








    /**
     * Tries to write the configuration file.
     * Prints an error if the directory exists but wasn't created by FrameworkConfig, or other issues arise.
     * @param configName The name of the config file.
     * @param config The config file instance.
     * @param MOD_ID The ID of the mod.This must be unique for each mod on the server.
     */
    public static void saveConfig(final @NotNull String configName, final @NotNull ConfigFile config, final @NotNull String MOD_ID) {

        // Compute paths
        final Path configDir = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
        final File configPath = configDir.resolve(configName + ".json").toFile();


        // Check config directory
        boolean configDirUsable = true;
        try {
            checkConfigDir(configDir);
        } catch(IOException e) {
            FrameworkConfig.LOGGER.error(ERROR_UNUSABLE_DIR.formatted(configDir.toString(), MOD_ID));
            e.printStackTrace();
            configDirUsable = false;
        }


        // Write config file if possible
        if(configDirUsable) {
            try(JsonWriter writer = new JsonWriter(new FileWriter(configPath))) {
                writer.setIndent("    ");
                writer.setLenient(false);
                gson.toJson(config, config.getClass(), writer);
            } catch(IOException e) {
                FrameworkConfig.LOGGER.error(ERROR_UNWRITABLE_FILE.formatted(configName));
                e.printStackTrace();
            }
        }
    }








    /**
     * Checks if the configuration directory exists and was created by FrameworkConfig.
     * Creates the directory if it doesn't exist, then writes the marker file in it.
     * @param dirPath The path of the directory.
     * @throws IOException If the directory exists but was created by another library.
     */
    public static void checkConfigDir(final @NotNull Path dirPath) throws IOException {

        // If the directory doesn't exist, create it and write the marker file
        if(!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
            Path markerFile = dirPath.resolve(MARKER_FILE_NAME);
            Files.writeString(markerFile, MARKER_FILE_CONTENTS);
        }


        // If the directory exists, check for the marker file
        else {
            Path markerFile = dirPath.resolve(MARKER_FILE_NAME);

            // Raise exception if the marker file is not there
            if (!Files.exists(markerFile)) {
                throw new IOException("Marker file not found in config directory");
            }

            // Raise exception if the marker file doesn't contain the right contents
            String content = Files.readString(markerFile);
            if (!MARKER_FILE_CONTENTS.equals(content)) {
                throw new IOException("Marker file contains incorrect content");
            }
        }
    }
}
