package org.elshift.modules.impl;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.elshift.Main;
import org.elshift.commands.annotations.Option;
import org.elshift.commands.annotations.SlashCommand;
import org.elshift.commands.annotations.TextCommand;
import org.elshift.commands.options.MultipleChoiceOption;
import org.elshift.config.Config;
import org.elshift.modules.Module;
import org.elshift.util.ParsedTextCommand;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class HelpMenuModule implements Module {
    public HelpMenuModule() {
    }

    @SlashCommand(name = "help", description = "Learn how to use HelloBot")
    public void help(SlashCommandInteractionEvent event, @Option(name = "topic", required = false) ModuleNameOption moduleName) {
        String msg;
        if (moduleName == null)
            msg = formatActiveModules();
        else
            msg = formatRequestedModule(moduleName.get());

        event.reply(msg).setEphemeral(true).queue();
    }

    // Raw text-command, due to slash-command unavailability
    // (Works in DMs and for clients that don't update when the bot first joins)
    @TextCommand(name = "help", description = "Learn how to use HelloBot")
    public void onMessageReceived(@NotNull MessageReceivedEvent event, ParsedTextCommand parsedText) {
        String args = parsedText.getCmdArgs();
        if (args != null && args.startsWith("topic:"))
            args = args.substring("topic:".length()).trim();

        String msg = (args == null || args.isEmpty()) ? formatActiveModules()
                : formatRequestedModule(args);
        event.getMessage().reply(msg).queue();
    }

    private String formatRequestedModule(String moduleName) {
        Optional<Module> optionalModule = Main.getBot().getActiveModules().stream()
                .filter(module -> module.getName().equals(moduleName))
                .findFirst();

        if (optionalModule.isEmpty())
            return "That module does not exist.\n\n" + formatActiveModules();

        Module module = optionalModule.get();
        String helpMessage = module.getHelpMessage();

        if (helpMessage == null || helpMessage.isEmpty())
            return "That module does not have a description.\n\n" + formatActiveModules();

        return helpMessage;
    }

    private String formatActiveModules() {
        List<String> moduleNames = Main.getBot().getActiveModules().stream().map(Module::getName).toList();

        StringBuilder s = new StringBuilder("Learn about any enabled features:\n```");

        moduleNames.forEach(name -> s.append("\n/help %s".formatted(name)));

        if (moduleNames.isEmpty())
            s.append("No features are enabled!");

        s.append("```");

        Collection<String> prefixes = Config.get().textPrefixes();
        if (!prefixes.isEmpty()) {
            s.append("\n(You can also use:");
            prefixes.forEach(pf -> s.append(" `%shelp`".formatted(pf)));
            s.append(')');
        }
        return s.toString();
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getHelpMessage() {
        return "Provides help for enabled features";
    }

    private static class ModuleNameOption extends MultipleChoiceOption<String> {
        @Override
        public Class<?> getType() {
            return String.class;
        }

        @Override
        public @NotNull Command.Choice[] getChoices() {
            return Main.getBot().getActiveModules().stream()
                    .map(Module::getName)
                    .map(name -> new Command.Choice(name, name))
                    .toArray(Command.Choice[]::new);
        }
    }
}
