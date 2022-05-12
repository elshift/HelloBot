package org.elshift;

import com.google.common.reflect.ClassPath;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.elshift.commands.CommandBuilder;
import org.elshift.commands.CommandHandler;
import org.elshift.config.Config;
import org.elshift.modules.annotations.CommandModule;
import org.elshift.modules.annotations.ListenerModule;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;


public class HelloBot extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(HelloBot.class);
    private JDA jda;



    public void createBot(String[] args, String token) {
        try {
            CommandBuilder cmdBuilder = new CommandBuilder();
            JDABuilder jdaBuilder = JDABuilder.createDefault(token);

            List<ClassPath.ClassInfo> collect = ClassPath.from(ClassLoader.getSystemClassLoader())
                    .getTopLevelClassesRecursive("org.elshift.modules")
                    .stream().collect(Collectors.toList());

            boolean hasWhitelist = Config.get().getWhitelist() != null && !Config.get().getWhitelist().isEmpty();

            for (ClassPath.ClassInfo classInfo : collect) {
                Class<?> c = classInfo.load();
                if (c.isAnnotation())
                    continue;

                if (hasWhitelist && !Config.get().getWhitelist().contains(c.getSimpleName()))
                    continue;

                Object o = c.getDeclaredConstructor().newInstance();
                if (c.isAnnotationPresent(CommandModule.class))
                    cmdBuilder.addModule(o);
                if (c.isAnnotationPresent(ListenerModule.class))
                    jdaBuilder.addEventListeners(o);
            }

            jdaBuilder.addEventListeners(this, new CommandHandler(cmdBuilder.build()));
            jda = jdaBuilder.build();
        } catch (Exception e) {
            logger.error("Failed to create bot", e);
        }
    }

    public void shutdownBot() {
        jda.shutdown();
        logger.info("Shutting down");
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        logger.info("Logged in as {}", event.getJDA().getSelfUser().getName());
    }
}
