package org.elshift.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Channel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.elshift.commands.annotations.CommandGroup;
import org.elshift.commands.annotations.RunMode;
import org.elshift.commands.annotations.SlashCommand;
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
    private static final List<OptionData> EMPTY_OPTIONS_LIST = new ArrayList<>();

    private final @NotNull SlashCommand command;
    private final Module module;
    private final @NotNull Method method;
    private final CommandGroup group;
    private final List<OptionData> options;
    private final RunMode.Mode runMode;

    public CommandInfo(
            @NotNull SlashCommand command,
            @NotNull Module module,
            @NotNull Method method,
            @Nullable CommandGroup group,
            @NotNull List<OptionData> options,
            @Nullable RunMode runMode
    ) {
        this.command = command;
        this.module = module;
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
     * @return Whether the interaction event matches this command
     */
    public boolean matchesEvent(@NotNull SlashCommandInteractionEvent event) {
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

    /**
     * Invokes the command with the specified command context & arguments.
     *
     * @param ctx  The context in which the command was executed
     * @param args The arguments
     */
    public void invoke(CommandContext ctx, @NotNull List<OptionMapping> args) throws ReflectiveOperationException {
        // TODO: 5/13/2022 invalid parameter types will throw ReflectiveOperationException, check the types before
        // TODO: 5/13/2022 invocation to provide a better error message
        Object[] arguments = new Object[1 + args.size()];
        arguments[0] = ctx; // ctx is always first
        JDA jda = ctx.event().getJDA();
        for (int i = 0; i < args.size(); i++)
            arguments[i + 1] = getOptionValue(jda, args.get(i));
        method.invoke(module, arguments);
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
    public List<OptionData> getOptions() {
        return options == null ? EMPTY_OPTIONS_LIST : options;
    }

    /**
     * @return The mode of execution for this command
     */
    public RunMode.Mode getRunMode() {
        return runMode;
    }
}