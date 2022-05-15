package org.elshift.commands;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.elshift.commands.annotations.*;
import org.elshift.commands.annotations.choice.*;
import org.elshift.commands.autocomplete.AutoCompleteProvider;
import org.elshift.commands.options.MultipleChoiceOption;
import org.elshift.modules.Module;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class to make adding slash commands easier.
 */
@SuppressWarnings("UnusedReturnValue")
public class CommandBuilder {
    private static final Logger logger = LoggerFactory.getLogger(CommandBuilder.class);
    private final List<CommandInfo> commandInfoList = new ArrayList<>();

    private static final Map<Class<?>, OptionType> TYPE_MAP = new HashMap<>() {{
        put(String.class, OptionType.STRING);
        put(Integer.class, OptionType.INTEGER);
        put(Boolean.class, OptionType.BOOLEAN);
        put(User.class, OptionType.USER);
        put(Channel.class, OptionType.CHANNEL);
        put(GuildChannel.class, OptionType.CHANNEL);
        put(VoiceChannel.class, OptionType.CHANNEL);
        put(IMentionable.class, OptionType.MENTIONABLE);
        put(Number.class, OptionType.NUMBER);
        put(Role.class, OptionType.ROLE);
        put(Message.Attachment.class, OptionType.ATTACHMENT);
    }};

    private void populateOptionChoices(OptionData optionData, Parameter parameter) {
        StringChoices stringChoices = parameter.getAnnotation(StringChoices.class);
        if (stringChoices != null) {
            for (StringChoice stringChoice : stringChoices.value())
                optionData.addChoice(stringChoice.name(), stringChoice.value());
            return;
        }

        LongChoices longChoices = parameter.getAnnotation(LongChoices.class);
        if (longChoices != null) {
            for (LongChoice longChoice : longChoices.value())
                optionData.addChoice(longChoice.name(), longChoice.value());
            return;
        }

        DoubleChoices doubleChoices = parameter.getAnnotation(DoubleChoices.class);
        if (doubleChoices != null) {
            for (DoubleChoice doubleChoice : doubleChoices.value())
                optionData.addChoice(doubleChoice.name(), doubleChoice.value());
        }
    }

    private @NotNull CustomOptionData getParameterOptionData(Module module, @NotNull Parameter parameter) {
        Class<?> paramType = parameter.getType();
        OptionType optionType = TYPE_MAP.get(parameter.getType());

        Command.Choice[] choices = null;

        if (paramType.isEnum()) {
            Object[] enumConstants = paramType.getEnumConstants();
            choices = new Command.Choice[enumConstants.length];
            for (int i = 0; i < enumConstants.length; i++)
                choices[i] = new Command.Choice(enumConstants[i].toString(), i);
            optionType = OptionType.INTEGER;
        } else if (MultipleChoiceOption.class.isAssignableFrom(paramType)) {
            try {
                MultipleChoiceOption<?> multipleChoice;

                // Non-static nested classes' constructors require an owning class instance parameter
                if (paramType.isMemberClass() && (paramType.getModifiers() & Modifier.STATIC) == 0) {
                    Constructor<?> constructor = paramType.getDeclaredConstructor(module.getClass());
                    constructor.setAccessible(true);
                    multipleChoice = (MultipleChoiceOption<?>) constructor.newInstance(module);
                } else {
                    Constructor<?> constructor = paramType.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    multipleChoice = (MultipleChoiceOption<?>) constructor.newInstance();
                }

                optionType = TYPE_MAP.get(multipleChoice.getType());
                choices = multipleChoice.getChoices();
            } catch (ReflectiveOperationException e) {
                logger.error("Failed to instantiate multiple choice parameter", e);
            }
        }

        Option option = parameter.getAnnotation(Option.class);

        String name = option.name();
        String description = option.description().isEmpty() ? name : option.description();
        boolean required = option.required();

        CustomOptionData optionData = new CustomOptionData(optionType, name, description, required);
        populateOptionChoices(optionData, parameter);

        if (choices != null)
            optionData.addChoices(choices);

        AutoComplete autoComplete = parameter.getAnnotation(AutoComplete.class);
        if (autoComplete != null) {
            try {
                Constructor<?> constructor = autoComplete.value().getDeclaredConstructor();
                constructor.setAccessible(true);
                AutoCompleteProvider autoCompleteProvider = (AutoCompleteProvider) constructor.newInstance();

                optionData.setAutoComplete(true);
                optionData.setAutoCompleteProvider(autoCompleteProvider);
            } catch (ReflectiveOperationException e) {
                logger.error("Failed to create auto-complete provider", e);
            }
        }

        return optionData;
    }

    /**
     * Adds a slash command method.
     *
     * @param method The method to add
     * @param group  Optional group
     * @return Self
     */
    public CommandBuilder addMethod(@NotNull Module module, @NotNull Method method, @Nullable CommandGroup group) {
        SlashCommand slashCommand = method.getAnnotation(SlashCommand.class);

        // Method isn't a command
        if (slashCommand == null)
            return this;

        List<CustomOptionData> options = new ArrayList<>();

        boolean doesHaveCommandContext = false;
        for (int i = 0; i < method.getParameterCount(); i++) {
            Parameter parameter = method.getParameters()[i];

            if (parameter.isVarArgs())
                throw new InvalidParameterException("Parameter cannot be varargs");

            if (parameter.getType().isPrimitive())
                throw new InvalidParameterException("Parameter cannot be a primitive type");

            boolean isCommandContext = CommandContext.class.equals(parameter.getType());

            if (i == 0 && !isCommandContext)
                throw new InvalidParameterException("First parameter must be %s".formatted(CommandContext.class.getName()));
            else if (isCommandContext) {
                doesHaveCommandContext = true;
                continue;
            }

            options.add(getParameterOptionData(module, parameter));
        }

        if (!doesHaveCommandContext)
            throw new InvalidParameterException("Method provided must have %s as first parameter".formatted(CommandContext.class.getName()));

        RunMode runMode = method.getAnnotation(RunMode.class);

        commandInfoList.add(
                new CommandInfo(
                        slashCommand,
                        module,
                        method,
                        group,
                        options,
                        runMode
                )
        );
        return this;
    }

    /**
     * Adds all eligible slash commands in a class.
     *
     * @param module The module instance
     * @return Self
     */
    public CommandBuilder addModule(@NotNull Module module) {
        CommandGroup commandGroup = module.getClass().getAnnotation(CommandGroup.class);

        for (Method method : module.getClass().getDeclaredMethods())
            addMethod(module, method, commandGroup);

        return this;
    }

    /**
     * @return The command info list.
     */
    public List<CommandInfo> build() {
        return commandInfoList;
    }
}
