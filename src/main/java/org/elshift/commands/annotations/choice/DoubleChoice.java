package org.elshift.commands.annotations.choice;

/**
 * Represents one of the possible double choices for a slash command option.
 */
public @interface DoubleChoice {
    /**
     * @return The name of this choice
     */
    String name();

    /**
     * @return The value of this choice
     */
    double value();
}
