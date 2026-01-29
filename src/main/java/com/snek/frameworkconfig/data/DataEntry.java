package com.snek.frameworkconfig.data;







public abstract class DataEntry {
    private boolean scheduledForSave = false;


    /**
     * Checks if this data entry is scheduled for saving.
     * @return True if the entry is scheduled, false otherwise.
     */
    public boolean isScheduledForSave() {
        return scheduledForSave;
    }


    /**
     * Changes the flag that defines if this entry is currently scheduled for saving.
     * <p>
     * This can effectively cancel saving for a scheduled entry or re-enable it at any time.
     * @param scheduled The new flag value.
     */
    public void markScheduledForSave(final boolean scheduled) {
        scheduledForSave = scheduled;
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
