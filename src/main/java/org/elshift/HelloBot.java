package org.elshift;

import com.google.common.reflect.ClassPath;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.elshift.commands.CommandBuilder;
import org.elshift.commands.CommandHandler;
import org.elshift.config.Config;
import org.elshift.modules.HelpMenuModule;
import org.elshift.modules.annotations.ModuleProperties;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class HelloBot extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(HelloBot.class);
    private JDA jda;
    Map<String, Object> activeModules = new HashMap<>();

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
                if (c.isAnnotation()
                        || c.isInterface()
                        || !c.isAnnotationPresent(ModuleProperties.class))
                    continue;


                if (hasWhitelist && !Config.get().getWhitelist().contains(c.getSimpleName())) {
                    continue;
                }

                ModuleProperties properties = c.getAnnotation(ModuleProperties.class);
                if (activeModules.containsKey(properties.name())) {
                    logger.error("Module \"%s\" and \"%s\" both have the same display name. Check for duplicates in annotation."
                            .formatted(c.getSimpleName(), c.getSimpleName()));
                    throw new Exception("Duplicate module display names");
                }

                Object inst = c.getDeclaredConstructor().newInstance();
                activeModules.put(properties.name(), inst);

                if (properties.useSlashCommands())
                    cmdBuilder.addModule(inst);
                if (properties.listenAllMessages())
                    jdaBuilder.addEventListeners(inst);
            }

            // FIXME: Big fat hack, making this static when it shouldn't have to be.
            // FIXME: See FIXME in CommandBuilder::addModule for reason
            HelpMenuModule.setActiveModules(activeModules);
            /*for (Object m : activeModules.values()) {
                if (m.getClass() == HelpMenuModule.class) {
                    ((HelpMenuModule) m).setActiveModules(activeModules);
                    break;
                }
            }*/

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
