package org.elshift.commands;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.CommandInteractionPayload;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.elshift.commands.annotations.RunMode;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Listens for and handles execution of interactions.
 */
public class CommandHandler extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(CommandHandler.class);
    private final List<CommandInfo> commands;

    private final List<SlashCommandData> slashCommands = new ArrayList<>();
    private final ExecutorService synchronousExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService asynchronousExecutor = Executors.newCachedThreadPool();

    public CommandHandler(List<CommandInfo> commands) {
        this.commands = commands;
    }

    private void registerCommandsForGuild(Guild guild) {
        if (slashCommands.isEmpty()) {
            for (CommandInfo commandInfo : commands) {
                SlashCommandData command = Commands.slash(
                        commandInfo.getCommand().name(),
                        commandInfo.getCommand().description()
                );

                boolean hasOptions = commandInfo.getOptions() != null && !commandInfo.getOptions().isEmpty();


                if (hasOptions)
                    command.addOptions(commandInfo.getOptions());

                slashCommands.add(command);
            }
        }

        List<Command> registeredCommands = guild.retrieveCommands().complete();

        for (Command command : registeredCommands) {
            // TODO: support more types of commands
            if (command.getType() != Command.Type.SLASH)
                continue;

            if (slashCommands.stream().noneMatch(info -> info.getName().equals(command.getName()))) {
                logger.info("Removing old command: " + command.getName());
                command.delete().queue();
            }
        }

        guild.updateCommands().addCommands(slashCommands).queue();
        logger.info("Registered {} command(s) for guild: {}", slashCommands.size(), guild.getName());
    }

    private void tryRegisterCommandsForGuild(Guild guild) {
        try {
            registerCommandsForGuild(guild);
        } catch (ErrorResponseException e) {
            logger.warn("Failed to create commands for guild: %s".formatted(guild.getName()));
        }
    }

    /**
     * Attempt to execute a command.
     */
    private void handleCommand(SlashCommandInteractionEvent event, @NotNull CommandInfo commandInfo) {
        CommandContext context = new CommandContext(event);
        List<OptionMapping> args = commandInfo.getOptions().stream().map(option -> event.getOption(option.getName())).collect(Collectors.toList());

        Runnable invoke = () -> {
            try {
                commandInfo.invoke(context, args);
            } catch (Exception e) {
                logger.error("Failed to execute command {}", commandInfo.getCommand().name(), e);
                event.reply("Failed to execute command!").setEphemeral(true).queue();
            }
        };

        if (commandInfo.getRunMode() == RunMode.Mode.Async)
            asynchronousExecutor.execute(invoke);
        else
            synchronousExecutor.execute(invoke);
    }

    private CommandInfo getCommandForEvent(CommandInteractionPayload event) {
        List<CommandInfo> list = commands.stream().filter(info -> info.matchesEvent(event)).toList();
        if (list.isEmpty())
            return null;

        return list.get(0);
    }

    public List<CommandInfo> getCommands() {
        return commands;
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        tryRegisterCommandsForGuild(event.getGuild());
    }

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        tryRegisterCommandsForGuild(event.getGuild());
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        CommandInfo info = getCommandForEvent(event);

        if (info == null) {
            event.reply("Command does not exist.").setEphemeral(true).queue();
            return;
        }

        handleCommand(event, info);
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        CommandInfo info = getCommandForEvent(event);

        if (info == null)
            return;

        List<CustomOptionData> options = info.getOptions();
        if (options.isEmpty())
            return;

        AutoCompleteQuery query = event.getFocusedOption();

        Optional<CustomOptionData> first = options.stream()
                .filter(data -> data.getName().equals(query.getName()))
                .findFirst();

        if (first.isEmpty())
            return;

        CustomOptionData currentOption = first.get();

        if (!currentOption.isAutoComplete())
            return;

        if (currentOption.getAutoCompleteProvider() == null)
            return;

        Command.Choice[] candidates = currentOption.getAutoCompleteProvider().getCandidates(query);
        if (candidates == null)
            return;

        event.replyChoices(candidates).queue();
    }
}
