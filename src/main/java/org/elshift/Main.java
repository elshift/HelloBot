package org.elshift;

import org.elshift.config.Config;

public class Main {
    private static final HelloBot bot = new HelloBot();

    public static void main(String[] args) {
        bot.createBot(args, Config.get().token());

        Runtime.getRuntime().addShutdownHook(new Thread(bot::shutdownBot));
    }
}
