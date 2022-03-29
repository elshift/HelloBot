package commands;

import commands.annotations.CommandGroup;
import commands.annotations.RunMode;
import commands.annotations.SlashCommand;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Channel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Stores information about a command.
 */
public final class CommandInfo {
    private static final List<OptionData> EMPTY_OPTIONS_LIST = new ArrayList<>();

    private final @NotNull SlashCommand command;
    private final Object classInstance;
    private final @NotNull Method method;
    private final CommandGroup group;
    private final List<OptionData> options;
    private final RunMode.Mode runMode;

    public CommandInfo(
            @NotNull SlashCommand command,
            @NotNull Class<?> klass,
            @NotNull Method method,
            @Nullable CommandGroup group,
            @NotNull List<OptionData> options,
            @Nullable RunMode runMode
    ) {
        this.command = command;
        try {
            this.classInstance = klass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new InvalidParameterException("Cannot instantiate instance of " + klass.getName());
        }
        this.method = method;
        this.group = group;
        this.options = options;
        this.runMode = runMode == null ? RunMode.Mode.Sync : runMode.value();
    }

    private boolean isSubcommandOf(String group) {
        if (getGroup() == null)
            return false;

        return getGroup().name().equals(group);
    }

    /**
     * @return
     *  Whether the interaction event matches this command
     */
    public boolean matchesEvent(@NotNull SlashCommandInteractionEvent event) {
        if (event.getSubcommandGroup() == null && getGroup() != null)
            return false;
        if (event.getSubcommandGroup() != null && getGroup() == null)
            return false;

        if (getGroup() != null && !isSubcommandOf(event.getSubcommandGroup())) {
            return false;
        }

        return command.name().equals(event.getName());
    }

    // I wish JDA let you grab the raw object...
    private Object getOptionValue(@NotNull JDA jna, OptionMapping mapping) {
        if(mapping == null)
            return null;

        switch (mapping.getType()) {
            case STRING -> {
                return mapping.getAsString();
            }
            case INTEGER -> {
                return mapping.getAsInt();
            }
            case BOOLEAN -> {
                return mapping.getAsBoolean();
            }
            case USER -> {
                return mapping.getAsUser();
            }
            case CHANNEL -> {
                return jna.getChannelById(Channel.class, mapping.getAsLong());
            }
            case ROLE -> {
                return mapping.getAsRole();
            }
            case MENTIONABLE -> {
                return mapping.getAsMentionable();
            }
            case NUMBER -> {
                return mapping.getAsDouble();
            }
            case ATTACHMENT -> {
                return mapping.getAsAttachment();
            }
            default -> throw new IllegalStateException("Unexpected value: " + mapping.getType());
        }
    }

    /**
     * Invokes the command with the specified command context & arguments.
     * @param ctx
     *  The context in which the command was executed
     * @param args
     *  The arguments
     */
    public void invoke(CommandContext ctx, @NotNull List<OptionMapping> args) throws InvocationTargetException, IllegalAccessException {
        // ctx needs to be first
        // this is kind of stupid
        List<Object> arguments = args.stream().map(mapping -> getOptionValue(ctx.event().getJDA(), mapping)).collect(Collectors.toList());
        arguments.add(0, ctx);
        method.invoke(classInstance, arguments.toArray());
    }

    /**
     * @return
     *  The actual slash command
     */
    public @NotNull SlashCommand getCommand() {
        return command;
    }

    /**
     * @return
     *  The group this command is a part of
     */
    @Nullable
    public CommandGroup getGroup() {
        return group;
    }

    /**
     * @return
     *  The list of options this command requires
     */
    public List<OptionData> getOptions() {
        return options == null ? EMPTY_OPTIONS_LIST : options;
    }

    /**
     * @return
     *  The mode of execution for this command
     */
    public RunMode.Mode getRunMode() {
        return runMode;
    }
}