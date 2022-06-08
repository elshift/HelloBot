package org.elshift.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.elshift.db.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class Config implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(Config.class);
    private static final Config instance = load("config.json");

    private String token;
    private String activity;
    private String downloadDir;
    private String sqlServer;
    private String sqlUser;
    private String sqlPass;
    private HashSet<String> whitelist;
    private ArrayList<String> textPrefixes;

    private Database sqlDatabase = new Database();

    private static void saveDefault(String path) {
        Config config = new Config();
        config.token = "YOUR_TOKEN";
        config.downloadDir = "downloads/";
        config.activity = "";
        config.sqlServer = "jdbc:sqlite:HelloBotDB.db";
        config.sqlUser = "";
        config.sqlPass = "";
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

        if (isNullOrEmpty(result.textPrefixes))
            logger.warn("You have not configured any text command prefixes! Text commands will not work.");
        else  // Longest prefixes come first, so that prefixes like { "x", "xy" } do not read "xy test" as "y test"
            result.textPrefixes.sort((String s1, String s2) -> s2.length() - s1.length());

        if (isNullOrEmpty(result.sqlUser) || isNullOrEmpty(result.sqlPass))
            result.sqlUser = result.sqlPass = null;

        // Dispose of any previous SQL connection, and (re)connect if an SQL server is configured
        try {
            if (result.sqlServer == null || result.sqlServer.isEmpty()) {
                logger.warn("You have not configured an SQL server connection! Database storage will be unavailable.");
                result.sqlDatabase.closeIfConnected();
            }
            else
                result.sqlDatabase.reconnect(result.sqlServer, result.sqlUser, result.sqlPass);
        } catch (SQLException e) {
            logger.error("Failed to open/close SQL server connection", e);
            result.sqlDatabase = new Database();
        }

        return result;
    }

    private static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    private static boolean isNullOrEmpty(List<?> list) {
        return list == null || list.isEmpty();
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

    public String sqlServer() {
        return sqlServer;
    }

    public String sqlUser() {
        return sqlUser;
    }

    public String sqlPass() {
        return sqlPass;
    }

    public HashSet<String> whitelist() {
        return whitelist;
    }

    public Collection<String> textPrefixes() {
        return textPrefixes;
    }

    public Database sqlDatabase() {
        return sqlDatabase;
    }
}
