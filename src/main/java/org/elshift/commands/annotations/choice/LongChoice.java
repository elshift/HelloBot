package org.elshift.commands.annotations.choice;

/**
 * Represents one of the possible long choices for a slash command option.
 */
public @interface LongChoice {
    /**
     * @return The name of this choice
     */
    String name();

    /**
     * @return The value of this choice
     */
    long value();
}
