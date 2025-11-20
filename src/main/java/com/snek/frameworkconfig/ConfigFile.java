package com.snek.frameworkconfig;




public abstract interface ConfigFile {

    /**
     * Called once the data is read from the config file, before it's used by any code.
     * <p> This is meant to be used to check whether the data is valid.
     *     The implementation is responsible for throwing exceptions.
     */
    public void validate() throws IllegalStateException;
}
