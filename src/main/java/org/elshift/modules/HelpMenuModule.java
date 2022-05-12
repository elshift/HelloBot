package org.elshift.modules;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.elshift.commands.CommandContext;
import org.elshift.commands.annotations.Option;
import org.elshift.commands.annotations.RunMode;
import org.elshift.commands.annotations.SlashCommand;
import org.elshift.modules.annotations.ModuleHelp;
import org.elshift.modules.annotations.ModuleProperties;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@ModuleProperties(name = "help", useSlashCommands = true, listenAllMessages = true)
public class HelpMenuModule extends ListenerAdapter {
    private static Map<String, Object> activeModules = new HashMap<>();

    public HelpMenuModule() {
        System.out.println("Helper instantiated");
    }

    public static void setActiveModules(Map<String, Object> activeModules) {
        HelpMenuModule.activeModules = activeModules;
    }

    @SlashCommand(name = "help", description = "Learn how to use HelloBot")
    @RunMode(RunMode.Mode.Async)
    public void helpSelect(CommandContext context, @Option(name = "topic", required = false) String moduleName) {
        String msg;
        if (moduleName == null)
            msg = formatActiveModules();
        else
            msg = formatRequestedModule(moduleName);

        context.replyEphemeral(msg);
    }

    // Raw text-command, due to slash-command unavailability
    // (Works in DMs and for clients that don't update when the bot first joins)
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        String contents = event.getMessage().getContentDisplay();

        // TODO: Make this a common function. See SakugabooruModule's onMessageReceived, too.
        final String[] prefixes = {"$help"};
        String foundPrefix = null;
        for (String prefix : prefixes) {
            if (contents.startsWith(prefix)) {
                foundPrefix = prefix;
                break;
            }
        }

        if (foundPrefix != null)
            contents = contents.substring(foundPrefix.length()).trim();
        else
            return;

        String msg = contents.isEmpty() ? formatActiveModules() : formatRequestedModule(contents);
        event.getMessage().reply(msg).queue();
    }

    private String formatRequestedModule(String moduleName) {
        Object module = activeModules.get(moduleName);

        if (module instanceof  ModuleHelp help) {
            return help.getHelpMessage();
        } else {
            String msg = (module == null) ?
                    "That module does not exist.\n\n" :
                    "That module does not have a description (Try asking the developer?).\n\n";
            msg += formatActiveModules();
            return msg;
        }
    }

    private String formatActiveModules() {
        if (activeModules.isEmpty())
            return "All features are disabled :(";

        StringBuilder msg = new StringBuilder("Learn about any enabled features:\n```");
        int totalInfo = 0;
        for (String m : activeModules.keySet()) {
            if (activeModules.get(m) instanceof ModuleHelp) {
                msg.append("\n/help %s".formatted(m));
                ++totalInfo;
            }
        }

        if (totalInfo == 0)
            return "No features have descriptions :/";

        msg.append("```\n(`$help` also works)");
        return msg.toString();
    }
}
