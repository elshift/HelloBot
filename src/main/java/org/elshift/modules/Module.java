package org.elshift.modules;

public interface Module {
    String getName();

    String getHelpMessage();

    default boolean usesSlashCommands() {
        return false;
    }
}
