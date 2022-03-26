import commands.CommandBuilder;
import commands.CommandHandler;
import config.Config;
import modules.DownloadModule;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HelloBot extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(HelloBot.class);

    public void createBot(String[] args) {
        try {
            CommandHandler commandHandler = new CommandHandler(
                    new CommandBuilder()
                            .addModule(DownloadModule.class)
                            .build()
            );

            JDA jda = JDABuilder.createDefault(Config.get().token())
                    .addEventListeners(this, commandHandler)
                    .build();

            jda.awaitReady();
        } catch (Exception e) {
            logger.error("Failed to create bot", e);
        }
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        logger.info("Logged in as {}", event.getJDA().getSelfUser().getName());
    }
}
