package org.elshift;

import com.google.common.reflect.ClassPath;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.elshift.commands.CommandBuilder;
import org.elshift.commands.CommandHandler;
import org.elshift.config.Config;
import org.elshift.modules.Module;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;


public class HelloBot extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(HelloBot.class);
    private JDA jda;
    private CommandHandler commandHandler;

    public void createBot(String[] args, String token) {
        try {
            CommandBuilder cmdBuilder = new CommandBuilder();
            JDABuilder jdaBuilder = JDABuilder.createDefault(token);

            loadModules(jdaBuilder, cmdBuilder);

            commandHandler = new CommandHandler(cmdBuilder.build());
            jdaBuilder.addEventListeners(this, commandHandler);

            String activity = Config.get().activity();
            if (activity != null && !activity.isEmpty())
                jdaBuilder.setActivity(Activity.playing(activity));

            jda = jdaBuilder.build();
        } catch (Exception e) {
            logger.error("Failed to create bot", e);
        }
    }

    private void loadModules(JDABuilder jdaBuilder, CommandBuilder cmdBuilder) throws Exception {
        List<ClassPath.ClassInfo> moduleClasses = ClassPath.from(ClassLoader.getSystemClassLoader())
                .getTopLevelClassesRecursive("org.elshift.modules")
                .stream().toList();

        Set<String> whitelist = Config.get().getWhitelist();
        boolean hasWhitelist = whitelist != null && !whitelist.isEmpty();

        for (ClassPath.ClassInfo classInfo : moduleClasses) {
            Class<?> klass = classInfo.load();

            // Check if it implements Module
            if (!Module.class.isAssignableFrom(klass))
                continue;

            String klassName = klass.getSimpleName();

            if (hasWhitelist && !whitelist.contains(klassName))
                continue;

            Module moduleInstance = (Module) klass.getDeclaredConstructor().newInstance();

            if (moduleInstance.usesSlashCommands()) {
                cmdBuilder.addModule(moduleInstance);
                logger.debug("Added slash command module: {}", klassName);
            }

            if (ListenerAdapter.class.isAssignableFrom(klass)) {
                jdaBuilder.addEventListeners(moduleInstance);
                logger.debug("Added event listener module: {}", klassName);
            }
        }
    }

    public void shutdownBot() {
        jda.shutdown();
        logger.info("Shutting down");
    }

    public CommandHandler getCommandHandler() {
        return commandHandler;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        logger.info("Logged in as {}", event.getJDA().getSelfUser().getName());
    }
}
