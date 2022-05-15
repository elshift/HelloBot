package org.elshift.commands.options;

import net.dv8tion.jda.api.interactions.commands.Command;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;

/**
 * Represents a dynamic multiple choice slash command option.
 *
 * @param <T> The value type
 */
public abstract class MultipleChoiceOption<T> {
    private T value;

    public MultipleChoiceOption() {
    }

    /**
     * Wraps an object value into the specified multiple choice option type.
     */
    @SuppressWarnings("unchecked")
    public static @NotNull MultipleChoiceOption<Object> wrap(@NotNull Class<?> type, Object value)
            throws ReflectiveOperationException {
        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        MultipleChoiceOption<Object> option
                = (MultipleChoiceOption<Object>) constructor.newInstance();
        option.setValue(value);
        return option;
    }

    /**
     * Set the value of the multiple choice option.
     *
     * @param value The new value
     */
    public final void setValue(T value) {
        this.value = value;
    }

    /**
     * @return The type of the chosen value
     */
    public abstract Class<?> getType();

    /**
     * @return The list of possible choices.
     */
    public abstract Command.Choice[] getChoices();

    /**
     * @return The chosen value
     */
    public final T get() {
        return value;
    }
}
