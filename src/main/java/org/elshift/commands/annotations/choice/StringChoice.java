package org.elshift.commands.annotations.choice;

/**
 * Represents one of the possible string choices for a slash command option.
 */
public @interface StringChoice {
    /**
     * @return The name of this choice
     */
    String name();

    /**
     * @return The value of this choice
     */
    String value();
}
