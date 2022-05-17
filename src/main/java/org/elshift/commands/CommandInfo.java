package org.elshift.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Channel;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.CommandInteractionPayload;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.elshift.commands.annotations.Command;
import org.elshift.commands.annotations.CommandGroup;
import org.elshift.commands.annotations.RunMode;
import org.elshift.commands.context.impl.SlashCommandContext;
import org.elshift.commands.context.impl.TextCommandContext;
import org.elshift.commands.options.MultipleChoiceOption;
import org.elshift.modules.Module;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores information about a command.
 */
public final class CommandInfo {
    private static final List<CustomOptionData> EMPTY_OPTIONS_LIST = new ArrayList<>();

    private final @NotNull Command command;
    private final Module module;
    private final @NotNull Method method;
    private final CommandGroup group;
    private final List<CustomOptionData> options;
    private final RunMode.Mode runMode;
    private final Class<?>[] parameterTypes;

    public CommandInfo(
            @NotNull Command command,
            @NotNull Module module,
            @NotNull Method method,
            @Nullable CommandGroup group,
            @NotNull List<CustomOptionData> options,
            @Nullable RunMode runMode
    ) {
        this.command = command;
        this.module = module;
        this.method = method;
        this.group = group;
        this.options = options;
        this.runMode = runMode == null ? RunMode.Mode.Sync : runMode.value();
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
    public boolean matchesEvent(@NotNull CommandInteractionPayload event) {
        if (event.getSubcommandGroup() == null && getGroup() != null)
            return false;
        if (event.getSubcommandGroup() != null && getGroup() == null)
            return false;

        if (getGroup() != null && !isSubcommandOf(event.getSubcommandGroup()))
            return false;

        return command.name().equals(event.getName());
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

    private Object getArgumentValue(Class<?> type, JDA jda, String argument) {
        if(String.class.equals(type))
            return argument;
        else if(Integer.class.equals(type))
            return Integer.parseInt(argument);
        else if(Float.class.equals(type))
            return Float.parseFloat(argument);
        else if(Double.class.equals(type))
            return Double.parseDouble(argument);
        else if(Boolean.class.equals(type))
            return Boolean.parseBoolean(argument);
        else if(User.class.equals(type))
            return jda.getUserById(argument);
        else if(Channel.class.equals(type))
            return jda.getChannelById(Channel.class, argument);
        else if(Role.class.equals(type))
            return jda.getRoleById(argument);
        else if(Member.class.equals(type))
            return jda.getRoleById(argument);
        throw new UnsupportedOperationException("unsupported parameter type: " + type.getName());
    }

    /**
     * Invokes the command with the specified command context & arguments.
     *
     * @param ctx  The context in which the command was executed
     * @param args The arguments
     */
    public void invoke(@NotNull SlashCommandContext ctx,
                       @NotNull List<OptionMapping> args) throws ReflectiveOperationException {
        Object[] arguments = new Object[1 + args.size()];
        arguments[0] = ctx; // ctx is always first

        JDA jda = ctx.getJDA();
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

        method.invoke(module, arguments);
    }

    public void invoke(TextCommandContext ctx, String[] args) {
        Object[] arguments = new Object[parameterTypes.length];
        arguments[0] = ctx;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            // TODO: 5/17/2022 parse arguments and invoke command method
        }
    }

    /**
     * @return The module that holds this command
     */
    public Module getModule() {
        return module;
    }

    /**
     * @return The actual slash command
     */
    public @NotNull Command getCommand() {
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

    /**
     * @return The mode of execution for this command
     */
    public RunMode.Mode getRunMode() {
        return runMode;
    }
}