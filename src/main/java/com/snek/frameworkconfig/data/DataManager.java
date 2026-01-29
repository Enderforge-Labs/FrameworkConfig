package com.snek.frameworkconfig.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import com.snek.frameworkconfig.FrameworkConfig;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.util.Tuple;








/**
 * The base class of data managers.
 * <p>
 * Data managers handle lazy reading and interval-based writing of data for entities
 * (players, mobs, or any UUID-identifiable object).
 * <p>
 * Each UUID maps to a single file, with all files stored in a directory based on the manager's dataId.
 */
public abstract class DataManager<T extends DataEntry> {

    // File path data
    private final @NotNull String modId;
    private final @NotNull String dataId;
    private final @NotNull String fileExtension;

    // Data cache and serializer
    private final @NotNull Map<UUID, T> cache = new HashMap<>();
    private final @NotNull List<Tuple<UUID, T>> scheduledForSaving = new LinkedList<>();
    private final @NotNull DataEntrySerializer<T> serializer;




    /**
     * Calculates the path to the directory where entity data files are saved.
     */
    public @NotNull Path calcDirPath() {
        return FrameworkConfig.getStorageDir(modId).resolve(dataId);
    }

    /**
     * Calculates the path to the save file of the specified entity.
     * @param uuid The uuid that identifies the entity.
     * @return The path to the save file of the entity.
     */
    public @NotNull Path calcFilePath(final @NotNull UUID uuid) {
        return calcDirPath().resolve(uuid.toString() + fileExtension);
    }




    /**
     * Creates a new DataManager.
     * @param modId The ID of the mod. This defines where the data is saved in the world files.
     * @param dataId The ID of the data manager. This identifies the data manager and contributes to the final path of the data files.
     * @param serializer An instance of the serializer class. This must be able to convert between {@link T} and a plain {@link String}.
     * @param fileExtension The extension to use for data files (including the dot).
     *     Extensions are purely cosmetic and don't affect functionality.
     *     Useful for syntax highlighting in text editors or integration with other tools.
     *     Defaults to ".json"
     */
    protected DataManager(final @NotNull String modId, final @NotNull String dataId, final @NotNull DataEntrySerializer<T> serializer, final @NotNull String fileExtension) {
        this.modId = modId;
        this.dataId = dataId;
        this.serializer = serializer;
        this.fileExtension = fileExtension;
    }


    /**
     * Creates a new DataManager.
     * @param serializer The serializer used to convert data instances from and to a string.
     * @param modId The ID of the mod. This defines where the data is saved in the world files.
     * @param dataId The ID of the data manager. This identifies the data manager and contributes to the final path of the data files.
     * @param serializer An instance of the serializer class. This must be able to convert between {@link T} and a plain {@link String}.
     */
    protected DataManager(final @NotNull String modId, final @NotNull String dataId, final @NotNull DataEntrySerializer<T> serializer) {
        this(modId, dataId, serializer, ".json");
    }




    /**
     * Retrieves the data associated with the specified UUID.
     * The data is read from the file during the first call, from a cache during subsequent calls.
     * @param uuid The UUID the data is associated with.
     * @return The data, or null if the data couldn't be found.
     *     Requesting data that doesn't exist is considered an issue and makes this method log a warning.
     */
    public T get(final @NotNull UUID uuid) {

        // Try to retrieve from the cache
        final T cachedData = cache.get(uuid);
        if(cachedData != null) return cachedData;


        // If the UUID is not cached, read from file. Return null if absent
        final Path filePath = calcFilePath(uuid);
        if(!Files.exists(filePath)) return null;
        try {

            // Load the data into the runtime map
            final String rawData = Files.readString(filePath);
            final T data = serializer.deserialize(rawData);
            cache.put(uuid, data);
            return data;
        }


        // If the file exists but cannot be read, return null
        catch(final IOException e) {
            FrameworkConfig.LOGGER.warn("Couldn't read the persistent data storage file {}. Treating it as non-existent.", filePath);
            return null;
        }
    }




    /**
     * Associates a new data entry to the provided UUID and saves it in the cache.
     * <p>
     * Providing a UUID that already exists will replace the associated data entry.
     * <p>
     * This method automatically schedules the entry for save.
     * @param uuid The UUID to associate the data with.
     * @param data The data entry to add and save.
     */
    public void put(final @NotNull UUID uuid, final @NotNull T data) {
        cache.put(uuid, data);
        schedule(uuid, data);
    }




    /**
     * Removes the data entry associated with the provided UUID from the runtime cache and removes its file.
     * <p>
     * Providing a UUID that doesn't exist in the cache has no effect.
     * @param uuid The UUID the data to remove is associated with.
     */
    @SuppressWarnings({ "java:S899", "java:S4042" }) //! Return value of file.delete() ignored
    public void remove(final @NotNull UUID uuid) {
        final T prev = cache.remove(uuid);
        if(prev == null) return;
        calcFilePath(uuid).toFile().delete();
    }




    /**
     * Schedules a data entry for save.
     * This marks the entry as scheduled but doesn't immediately save it to file.
     * <p>
     * Only cached entries can be scheduled. To cache a new entry, call {@link #add(UUID, T)}.
     * <p>
     * At the end of each server tick, all data entries scheduled during that tick are written to file and marked as not scheduled.
     * @param uuid The UUID this data entry is associated with.
     * @param data The data entry associated with the provided UUID. This can be omitted if not known.
     */
    public void schedule(final @NotNull UUID uuid, final @NotNull T data) {
        if(!data.isScheduledForSave()) {
            scheduledForSaving.add(new Tuple<UUID, T>(uuid, data));
            data.markScheduledForSave(true);
        }
    }

    /**
     * Schedules a data entry for save.
     * This marks the entry as scheduled but doesn't immediately save it to file.
     * <p>
     * Only cached entries can be scheduled. To cache a new entry, call {@link #add(UUID, T)}.
     * <p>
     * At the end of each server tick, all data entries scheduled during that tick are written to file and marked as not scheduled.
     * @param uuid The UUID this data entry is associated with.
     * @return True if the entry was successfully scheduled, false if it couldn't be found in the runtime cache.
     */
    public boolean schedule(final @NotNull UUID uuid) {
        final T data = cache.get(uuid);
        if(data == null) return false;
        schedule(uuid, data);
        return true;
    }




    /**
     * Saves all scheduled data entries.
     * This must be called at the end of each server tick.
     */
    private void saveScheduled() {

        // Create directory for this manager's persistent data
        try {
            Files.createDirectories(calcDirPath());
        } catch(final IOException e) {
            FrameworkConfig.LOGGER.error("Couldn't create persistent data storage directory {}" + calcDirPath(), e);
            return;
        }


        // Iterate scheduled entries
        for(final var scheduledPair : scheduledForSaving) {
            final UUID uuid = scheduledPair.getA();
            final T entry = scheduledPair.getB();

            // If the entry can be saved to file and it's still scheduled for save
            if(entry.canBeSavedToFile() && entry.isScheduledForSave()) {

                // Serialize the data and write it to file
                try {
                    final String string = serializer.serialize(entry);
                    Files.writeString(calcFilePath(uuid), string);
                }

                // Print error if there was an issue writing while the file
                catch(final IOException e) {
                    FrameworkConfig.LOGGER.error("Couldn't create persistent data storage file {}", calcFilePath(uuid), e);
                }
            }

            // Flag the entry as not scheduled
            entry.markScheduledForSave(false);
        }


        // Clear scheduled entries list
        scheduledForSaving.clear();
    }




    /**
     * Registers this data manager.
     * <p>
     * Each data manager instance must be registered during server initialization.
     * This is required in order for them to work properly.
     */
    public void registerDataManager() {
        ServerTickEvents.END_SERVER_TICK.register(server -> { saveScheduled(); });
    }
}
