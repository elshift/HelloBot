package org.elshift.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;

public class Config implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(Config.class);
    private static final Config instance = load("config.json");

    private String token;
    private String activity;
    private String downloadDir;
    private HashSet<String> whitelist;
    private ArrayList<String> textPrefixes;

    private static void saveDefault(String path) {
        Config config = new Config();
        config.token = "YOUR_TOKEN";
        config.downloadDir = "downloads/";
        config.activity = "";
        config.whitelist = new HashSet<>();
        config.textPrefixes = new ArrayList<>() {{
            add("/");
        }};

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

        if (result.textPrefixes == null || result.textPrefixes.isEmpty())
            logger.warn("You have not configured any text command prefixes! Text commands will not work.");
        else  // Longest prefixes come first, so that prefixes like { "x", "xy" } do not read "xy test" as "y test"
            result.textPrefixes.sort((String s1, String s2) -> s2.length() - s1.length());

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

    public HashSet<String> whitelist() {
        return whitelist;
    }

    public Collection<String> textPrefixes() {
        return textPrefixes;
    }
}
