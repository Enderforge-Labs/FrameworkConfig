package com.snek.frameworkconfig.data;







public abstract class DataEntry {
    private boolean scheduledForSave = false;
    private boolean deleted = false;


    /**
     * Checks if this data entry is scheduled for saving.
     * @return True if the entry is scheduled, false otherwise.
     */
    public final boolean isScheduledForSave() {
        return scheduledForSave;
    }


    /**
     * Checks if this data entry has been deleted.
     * <p>
     * Deleted entries are not present in the cache, have no storage file and are never scheduled for save.
     * <p>
     * Entries cannot be un-deleted. Once deleted, they are gone forever.
     * @return True if the entry has been deleted, false otherwise.
     */
    public final boolean isDeleted() {
        return deleted;
    }


    /**
     * Changes the flag that defines if this entry is currently scheduled for saving.
     * <p>
     * This can effectively cancel saving for a scheduled entry or re-enable it at any time.
     * <p>
     * Deleted entries always set their scheduled flag to false, regardless of the provided value.
     * @param scheduled The new flag value.
     */
    public final void markScheduledForSave(final boolean scheduled) {
        this.scheduledForSave = scheduled && !deleted;
    }


    /**
     * Changes the flag that defines if this entry has been deleted.
     * ! This is intentionally package-private.
     * ! This can only be called by the base DataManager.
     */
    final void markDeleted() {
        this.deleted = true;
    }


    /**
     * Defines if a data entry can be saved to file or must only exist in memory.
     * This method is called right before the entry is saved, and the save is skipped if false is returned.
     * <p>
     * Implementations can override this to prevent special entries such as default or temporary data
     * from being written to file or decide this conditionally in runtime.
     * <p>
     * Entries that can't be stored in a file must be added manually
     * @return True if the entry can be saved to file, false otherwise.
     */
    public boolean canBeSavedToFile() {
        return true;
    }
}
