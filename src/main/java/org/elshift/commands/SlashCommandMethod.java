package org.elshift.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Channel;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.CommandInteractionPayload;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.elshift.commands.annotations.CommandGroup;
import org.elshift.commands.annotations.RunMode;
import org.elshift.commands.annotations.SlashCommand;
import org.elshift.commands.options.MultipleChoiceOption;
import org.elshift.modules.Module;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class SlashCommandMethod extends CommandMethod {
    private static final List<CustomOptionData> EMPTY_OPTIONS_LIST = new ArrayList<>();

    private final @NotNull SlashCommand command;
    private final CommandGroup group;
    private final List<CustomOptionData> options;
    private final Class<?>[] parameterTypes;

    public SlashCommandMethod(
            @NotNull SlashCommand command,
            @NotNull Module module,
            @NotNull Method method,
            @Nullable CommandGroup group,
            @NotNull List<CustomOptionData> options,
            @Nullable RunMode runMode
    ) {
        super(command.name(), module, method, group, runMode);
        this.command = command;
        this.group = group;
        this.options = options;
        this.parameterTypes = method.getParameterTypes();
    }

    private boolean isSubcommandOf(String group) {
        if (getGroup() == null)
            return false;

        return getGroup().name().equals(group);
    }

    /**
     * @return Whether the interaction event matches this command
     */
    @Override
    public boolean matchesEvent(@NotNull Object event) {
        if (!(event instanceof CommandInteractionPayload interaction))
            return false;

        if (interaction.getSubcommandGroup() == null && getGroup() != null)
            return false;
        if (interaction.getSubcommandGroup() != null && getGroup() == null)
            return false;

        if (getGroup() != null && !isSubcommandOf(interaction.getSubcommandGroup()))
            return false;

        return command.name().equals(interaction.getName());
    }

    // I wish JDA let you grab the raw object...
    private Object getOptionValue(@NotNull JDA jda, OptionMapping mapping) {
        if (mapping == null)
            return null;

        return switch (mapping.getType()) {
            case STRING -> mapping.getAsString();
            case INTEGER -> mapping.getAsInt();
            case BOOLEAN -> mapping.getAsBoolean();
            case USER -> mapping.getAsUser();
            case CHANNEL -> jda.getChannelById(Channel.class, mapping.getAsLong());
            case ROLE -> mapping.getAsRole();
            case MENTIONABLE -> mapping.getAsMentionable();
            case NUMBER -> mapping.getAsDouble();
            case ATTACHMENT -> mapping.getAsAttachment();
            default -> throw new IllegalStateException("Unexpected value: " + mapping.getType());
        };
    }

    public void invoke(@NotNull Event ctx) throws ReflectiveOperationException {
        // TODO: 5/13/2022 invalid parameter types will throw ReflectiveOperationException, check the types before
        // TODO: 5/13/2022 invocation to provide a better error message

        if (!(ctx instanceof SlashCommandInteractionEvent slashEvent))
            throw new IllegalArgumentException("Expected SlashCommandInteractionEvent");

        List<OptionMapping> args = getOptions().stream().map(option -> slashEvent.getOption(option.getName())).collect(Collectors.toList());

        Object[] arguments = new Object[1 + args.size()];
        arguments[0] = slashEvent; // ctx is always first

        JDA jda = slashEvent.getJDA();
        for (int i = 0; i < args.size(); i++) {
            Object rawValue = getOptionValue(jda, args.get(i));
            // Add 1 to skip over ctx
            Class<?> parameterType = parameterTypes[i + 1];

            if (MultipleChoiceOption.class.isAssignableFrom(parameterType))
                arguments[i + 1] = MultipleChoiceOption.wrap(parameterType, rawValue);
            else if (parameterType.isEnum()) {
                Object[] constants = parameterType.getEnumConstants();
                arguments[i + 1] = constants[(int) rawValue];
            } else
                arguments[i + 1] = rawValue;
        }

        getMethod().invoke(getModule(), arguments);
    }

    /**
     * @return The actual slash command
     */
    public @NotNull SlashCommand getCommand() {
        return command;
    }

    /**
     * @return The group this command is a part of
     */
    @Nullable
    public CommandGroup getGroup() {
        return group;
    }

    /**
     * @return The list of options this command requires
     */
    public List<CustomOptionData> getOptions() {
        return options == null ? EMPTY_OPTIONS_LIST : options;
    }
}