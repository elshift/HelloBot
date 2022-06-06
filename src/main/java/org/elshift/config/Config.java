package org.elshift.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.Serializable;
import java.util.HashSet;

public class Config implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(Config.class);
    private static final Config instance = load("config.json");

    private String token;
    private String activity;
    private String downloadDir;
    private String textPrefix;
    private HashSet<String> whitelist;

    private static void saveDefault(String path) {
        Config config = new Config();
        config.token = "YOUR_TOKEN";
        config.downloadDir = "downloads/";
        config.activity = "";
        config.textPrefix = "";
        config.whitelist = new HashSet<>();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(path)) {
            writer.write(gson.toJson(config));
        } catch (Exception e) {
            logger.error("Failed to write config file: %s".formatted(path), e);
        }
    }

    public static Config load(String configPath) {
        Config result = null;
        Gson gson = new Gson();

        try (FileReader reader = new FileReader(configPath)) {
            result = gson.fromJson(reader, Config.class);
        } catch (Exception e) {
            logger.info("Failed to load config, creating default. You need to edit config.json to include the bot token.");
            saveDefault(configPath);
            System.exit(0);
        }

        return result;
    }

    public static Config get() {
        return instance;
    }

    public String token() {
        return token;
    }

    public String downloadDir() {
        return downloadDir;
    }

    public String activity() {
        return activity;
    }

    public String getTextPrefix() { return textPrefix; }

    public HashSet<String> getWhitelist() {
        return whitelist;
    }
}
