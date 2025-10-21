package net.IneiTsuki.forgivingmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Handles loading, saving, and validating the ForgivingMod configuration.
 * Generates a user-friendly JSON config with inline comments for clarity.
 */
public class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("ForgivingMod");
    private static final Path CONFIG_PATH = Paths.get("config/ForgivingMod/Forgiving.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Config config = new Config();

    // -----------------------
    // Public accessors
    // -----------------------
    public static Config getConfig() {
        return config;
    }

    // -----------------------
    // Config operations
    // -----------------------
    public static void loadConfig() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                LOGGER.info("Config file not found. Creating default configuration...");
                saveConfig();
                return;
            }

            try (BufferedReader reader = Files.newBufferedReader(CONFIG_PATH)) {
                config = GSON.fromJson(reader, Config.class);
            }

            validateConfig();
            LOGGER.info("Configuration loaded successfully:\n{}", GSON.toJson(config));

        } catch (Exception e) {
            LOGGER.error("Failed to load configuration! Restoring defaults.", e);
            config = new Config();
            saveConfig();
        }
    }

    public static void saveConfig() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            // Write JSON with helpful comments
            String json = GSON.toJson(config);
            String commentedJson = addComments(json);

            try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_PATH)) {
                writer.write(commentedJson);
            }

            LOGGER.info("Configuration saved successfully (with comments).");
        } catch (IOException e) {
            LOGGER.error("Failed to save configuration.", e);
        }
    }

    public static void reloadConfig() {
        LOGGER.info("Reloading configuration...");
        loadConfig();
        LOGGER.info("Configuration reloaded successfully.");
    }

    // -----------------------
    // Validation
    // -----------------------
    private static void validateConfig() {
        boolean changed = false;

        if (config.max_deaths < 1) {
            LOGGER.warn("Invalid max_deaths value '{}', resetting to 1.", config.max_deaths);
            config.max_deaths = 1;
            changed = true;
        }
        if (config.health_per_death < 1) {
            LOGGER.warn("Invalid health_per_death value '{}', resetting to 1.", config.health_per_death);
            config.health_per_death = 1;
            changed = true;
        }
        if (config.max_extra_hearts < 0) {
            LOGGER.warn("Invalid max_extra_hearts value '{}', resetting to 0.", config.max_extra_hearts);
            config.max_extra_hearts = 0;
            changed = true;
        }
        if (config.auto_save_interval < 5) {
            LOGGER.warn("Invalid auto_save_interval '{}', resetting to 5 seconds minimum.", config.auto_save_interval);
            config.auto_save_interval = 5;
            changed = true;
        }

        if (changed) {
            saveConfig();
        }
    }

    // -----------------------
    // JSON comment helper
    // -----------------------
    private static String addComments(String json) {
        // Prepend helpful comments above the JSON
        return """
            // =============================================================
            // ForgivingMod Configuration File
            // -------------------------------------------------------------
            // Edit these settings to adjust mod behavior.
            // Comments are ignored by Minecraft / Gson, but be careful not
            // to break JSON syntax (commas, quotes, braces).
            //
            // max_deaths         : Maximum deaths allowed before punishment.
            // health_per_death   : Hearts lost per death.
            // max_extra_hearts   : Maximum bonus hearts that can be earned.
            // enable_ban         : If true, bans players who exceed max deaths.
            // ban_duration       : Ban length in seconds (0 = permanent).
            // auto_save_interval : How often player data is autosaved (seconds).
            // ban_message        : Message shown to banned players.
            // =============================================================

            """ + json;
    }

    // -----------------------
    // Inner Config class
    // -----------------------
    public static class Config {
        public int max_deaths = 5;
        public int health_per_death = 2;
        public int max_extra_hearts = 10;
        public boolean enable_ban = true;
        public int ban_duration = 0;
        public int auto_save_interval = 30;
        public String ban_message = "You died too many times and have been banned!";
    }
}
