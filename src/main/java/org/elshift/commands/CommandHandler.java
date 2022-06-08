package org.elshift.commands;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.elshift.commands.annotations.RunMode;
import org.elshift.config.Config;
import org.elshift.util.ParsedTextCommand;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Listens for and handles execution of interactions.
 */
public class CommandHandler extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(CommandHandler.class);
    private final List<CommandMethod> commands;

    private final List<SlashCommandData> slashCommands = new ArrayList<>();
    private final ExecutorService synchronousExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService asynchronousExecutor = Executors.newCachedThreadPool();

    public CommandHandler(List<CommandMethod> commands) {
        this.commands = commands;
    }

    private void registerCommandsForGuild(Guild guild) {
        if (slashCommands.isEmpty()) {
            for (CommandMethod baseCmd : commands) {
                if (!(baseCmd instanceof SlashCommandMethod slashMethod))
                    continue;

                SlashCommandData command = Commands.slash(
                        slashMethod.getCommand().name(),
                        slashMethod.getCommand().description()
                );

                boolean hasOptions = slashMethod.getOptions() != null && !slashMethod.getOptions().isEmpty();
                if (hasOptions)
                    command.addOptions(slashMethod.getOptions());

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
                try {
                    command.delete().queue();
                } catch (Exception ignored) {
                }
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
    private void handleCommand(Event event, @NotNull CommandMethod cmdMethod) {
        Runnable invoke = () -> {
            try {
                cmdMethod.invoke(event);
            } catch (Exception e) {
                logger.error("Failed to execute command {}", cmdMethod.getName(), e);
                String response = "Failed to execute command! " + e;

                if (event instanceof SlashCommandInteractionEvent slashEvent)
                    slashEvent.reply(response).setEphemeral(true).queue();
                else if (event instanceof MessageReceivedEvent msgEvent)
                    msgEvent.getMessage().reply(response).queue();
            }
        };

        if (cmdMethod.getRunMode() == RunMode.Mode.Async)
            asynchronousExecutor.execute(invoke);
        else
            synchronousExecutor.execute(invoke);
    }

    private CommandMethod getCommandForEvent(Object event) {
        List<CommandMethod> list = commands.stream().filter(info -> info.matchesEvent(event)).toList();
        if (list.isEmpty())
            return null;

        return list.get(0);
    }

    public List<CommandMethod> getCommands() {
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
        CommandMethod info = getCommandForEvent(event);

        if (info == null)
            event.reply("Command does not exist.").setEphemeral(true).queue();
        else
            handleCommand(event, info);
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        CommandMethod method = getCommandForEvent(event);

        if (!(method instanceof SlashCommandMethod slashMethod))
            return;

        List<CustomOptionData> options = slashMethod.getOptions();
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

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        Collection<String> prefixes = Config.get().textPrefixes();
        if (prefixes == null || prefixes.isEmpty())
            return;

        ParsedTextCommand parsed = new ParsedTextCommand(event.getMessage().getContentRaw());
        if (!parsed.hasPrefix())
            return;

        CommandMethod cmd = getCommandForEvent(event);
        if (cmd != null)
            handleCommand(event, cmd);
    }
}
