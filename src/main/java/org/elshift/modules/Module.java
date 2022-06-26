package org.elshift.modules;

/**
 * Base module interface.
 */
public interface Module {
    /**
     * @return The name of this module
     */
    String getName();

    /**
     * @return The help message of this module
     */
    String getHelpMessage();
}
