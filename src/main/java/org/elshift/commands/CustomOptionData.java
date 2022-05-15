package org.elshift.commands;

import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.elshift.commands.autocomplete.AutoCompleteProvider;
import org.jetbrains.annotations.NotNull;

public class CustomOptionData extends OptionData {
    private AutoCompleteProvider autoCompleteProvider;

    public CustomOptionData(@NotNull OptionType type, @NotNull String name, @NotNull String description) {
        super(type, name, description);
    }

    public CustomOptionData(@NotNull OptionType type, @NotNull String name, @NotNull String description, boolean isRequired) {
        super(type, name, description, isRequired);
    }

    public CustomOptionData(@NotNull OptionType type, @NotNull String name, @NotNull String description, boolean isRequired, boolean isAutoComplete) {
        super(type, name, description, isRequired, isAutoComplete);
    }

    public AutoCompleteProvider getAutoCompleteProvider() {
        return autoCompleteProvider;
    }

    public void setAutoCompleteProvider(AutoCompleteProvider autoCompleteProvider) {
        this.autoCompleteProvider = autoCompleteProvider;
    }
}
