package org.elshift;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.elshift.commands.CommandBuilder;
import org.elshift.commands.CommandHandler;
import org.elshift.modules.BloonsCodeModule;
import org.elshift.modules.DownloadModule;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HelloBot extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(HelloBot.class);
    private JDA jda;

    public void createBot(String[] args, String token) {
        try {
            CommandHandler commandHandler = new CommandHandler(
                    new CommandBuilder()
                            .addModule(DownloadModule.class)
                            .build()
            );

            jda = JDABuilder.createDefault(token)
                    .addEventListeners(this, commandHandler)
                    .addEventListeners(new BloonsCodeModule())
                    .build();

            jda.awaitReady();
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
