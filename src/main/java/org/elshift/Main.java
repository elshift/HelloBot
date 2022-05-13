package org.elshift;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.elshift.config.Config;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Main {
    private static final HelloBot bot = new HelloBot();

    public static void main(String[] args) {
        Set<String> argsSet = new HashSet<>();
        Collections.addAll(argsSet, args);

        if (argsSet.contains("--verbose")) {
            Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            root.setLevel(Level.DEBUG);
            System.out.println("Set log level to DEBUG");
        }

        bot.createBot(args, Config.get().token());

        Runtime.getRuntime().addShutdownHook(new Thread(bot::shutdownBot));
    }

    public static HelloBot getBot() {
        return bot;
    }
}
