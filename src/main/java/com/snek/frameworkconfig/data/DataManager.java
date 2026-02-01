package com.snek.frameworkconfig.data;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    // Getters
    public @NotNull String                 getModId        () { return modId;         }
    public @NotNull String                 getDataId       () { return dataId;        }
    public @NotNull String                 getFileExtension() { return fileExtension; }
    public @NotNull Map<UUID, T>           getCache        () { return cache;         }
    public @NotNull DataEntrySerializer<T> getSerializer   () { return serializer;    }




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

        // Register save operations
        ServerTickEvents.END_SERVER_TICK.register(server -> { saveScheduled(); });
        //FIXME use a separate thread to save stuff to file. This improves server response times
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
    public @Nullable T get(final @NotNull UUID uuid) {

        // Try to retrieve from the cache
        final T cachedData = cache.get(uuid);
        if(cachedData != null) return cachedData;


        // If the UUID is not cached, read from file. Return null if absent
        return loadFromFile(uuid);
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
    public final void put(final @NotNull UUID uuid, final @NotNull T data) {
        cache.put(uuid, data);
        schedule(uuid, data);
        afterPut(uuid, data);
    }
    /**
     * Callback method for subclasses.
     * <p>
     * This is called each time a new data entry is added to the cache (which includes the time it's first loaded from disk).
     * <p>
     * At this stage, the entry is already present in the cache and could currently be scheduled for save.
     * @param uuid The UUID the data is associated with.
     * @param data The newly added data entry.
     */
    protected void afterPut(final @NotNull UUID uuid, final @NotNull T data) {}




    /**
     * Removes the data entry associated with the provided UUID from the runtime cache and removes its file.
     * <p>
     * This also marks the data as not scheduled for save.
     * <p>
     * Providing a UUID that doesn't exist in the cache has no effect.
     * @param uuid The UUID the data to remove is associated with.
     */
    @SuppressWarnings({ "java:S899", "java:S4042" }) //! Return value of file.delete() ignored
    public final void remove(final @NotNull UUID uuid) {
        final T prev = cache.remove(uuid);
        if(prev == null) return;
        prev.markScheduledForSave(false);
        calcFilePath(uuid).toFile().delete();
        afterRemove(uuid, prev);
    }
    /**
     * Callback method for subclasses.
     * <p>
     * This is called each time a data entry is removed from the cache.
     * <p>
     * At this stage, the entry is not in the cache anymore and its file has been removed.
     * It is also not scheduled for save anymore (if it ever was before).
     * @param uuid The UUID the data was associated with.
     * @param data The data entry that was just removed from the cache.
     */
    protected void afterRemove(final @NotNull UUID uuid, final @NotNull T data) {}




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
            scheduledForSaving.add(new Tuple<>(uuid, data));
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




    //FIXME make this async from a specialized thread
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
     * Reloads all data entries into the runtime cache, reading from their save files, after saving all existing modified entries.
     * <p>
     * This effectively merges the entries in the existing cache with the entries read from the files, prioritizing changes made through Minecraft.
     * <p>
     * Mods should call this when they want to reload the data after manual changes to their files,
     * or if having all data entries loaded is required for the code to work correctly.
     * <p>
     * Notice:
     * This is done on the caller's thread. If async loading is an option, you should call this from a separate thread.
     * This method skips lazy loading and can sometimes require a very high number of disk operations, creating lag spikes.
     * Don't call this in a loop.
     */
    public void forceLoadAll() {

        // Forcefully write modified entries to file
        saveScheduled(); //FIXME this is gonna be async. which is bad. call it not async and wait for the data to get flushed to file before reading


        // For each file in the storage directory
        for(final File file : calcDirPath().toFile().listFiles()) {

            // Skip directories if for some reason any are present. They shouldn't be, though
            if(file.isDirectory()) continue;

            // Compute the UUID from the file's name, then load its data
            final String fileName = file.getName();
            try {
                final UUID uuid = UUID.fromString(fileName.substring(0, fileName.length() - fileExtension.length()));
                loadFromFile(uuid);
            }

            // Print a warning if the file is not recognized
            catch(final IllegalArgumentException | IndexOutOfBoundsException e) {
                try {
                    //! IllegalArgumentException: Bad UUID / not a UUID
                    //! IndexOutOfBoundsException: Bad extension / bad file name. Anything that makes substring fail
                    FrameworkConfig.LOGGER.warn("Unexpected file in storage directory: {}. This is not a FrameworkConfig storage file", file.getCanonicalPath());
                    // continue
                }
                catch(IOException e2) {
                    //! This IOException can be caused by getCanonicalPath.
                    //! Just do nothing in this case, we have no idea what's going on anymore and the file isn't readable anyway.
                    // let's continue...
                }
            }
        }
    }




    /**
     * Retrieves the data associated with the specified UUID by reading it from the storage file.
     * <p>
     * This method doesn't use the cache. It only reads from file. To use the cache, call {@link #get(UUID)}.
     * <p>
     * Notice:
     * This is done on the caller's thread. If async loading is an option, you should call this from a separate thread.
     * @param uuid The UUID the data is associated with.
     * @return The data, or null if the data couldn't be found.
     *     Requesting data that doesn't exist is considered an issue and makes this method log a warning.
     */
    public @Nullable T loadFromFile(final @NotNull UUID uuid) {

        // Calculate file path. If the file doesn't exist, return null
        final Path filePath = calcFilePath(uuid);
        if(!Files.exists(filePath)) return null;
        try {

            // Load the data into the runtime map
            final String rawData = Files.readString(filePath);
            final T data = serializer.deserialize(rawData);
            cache.put(uuid, data);
            afterPut(uuid, data);
            return data;
        }


        // If the file exists but cannot be read, print a warning and return null
        catch(final IOException e) {
            FrameworkConfig.LOGGER.warn("Couldn't read the persistent data storage file {}. Treating it as non-existent.", filePath);
            return null;
        }
    }
}
