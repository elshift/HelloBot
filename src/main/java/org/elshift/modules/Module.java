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

    /**
     * @return Whether this module uses slash commands
     */
    default boolean usesSlashCommands() {
        return false;
    }
}
