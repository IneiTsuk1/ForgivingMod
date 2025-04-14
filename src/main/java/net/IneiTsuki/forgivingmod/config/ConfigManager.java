package net.IneiTsuki.forgivingmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("ForgivingMod");
    private static final Path CONFIG_PATH = Paths.get("config/ForgivingMod/Forgiving.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Default values
    public static int maxDeaths = 5;
    public static int healthPerDeath = 2;
    public static int maxExtraHearts = 10;
    public static boolean enableBan = true;
    public static int banDuration = 0; // 0 means permanent ban
    public static int autoSaveInterval = 30; // Default to 30 seconds
    public static String banMessage = "You died too many times and have been banned!";

    public static void loadConfig() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                saveDefaultConfig();
                return;
            }

            try (BufferedReader reader = Files.newBufferedReader(CONFIG_PATH)) {
                JsonObject config = JsonParser.parseReader(reader).getAsJsonObject();

                maxDeaths = config.has("max_deaths") ? config.get("max_deaths").getAsInt() : maxDeaths;
                healthPerDeath = config.has("health_per_death") ? config.get("health_per_death").getAsInt() : healthPerDeath;
                maxExtraHearts = config.has("max_extra_hearts") ? config.get("max_extra_hearts").getAsInt() : maxExtraHearts;
                enableBan = config.has("enable_ban") ? config.get("enable_ban").getAsBoolean() : enableBan;
                banDuration = config.has("ban_duration") ? config.get("ban_duration").getAsInt() : banDuration;
                autoSaveInterval = config.has("auto_save_interval") ? config.get("auto_save_interval").getAsInt() : autoSaveInterval;
                banMessage = config.has("ban_message") ? config.get("ban_message").getAsString() : banMessage;
            }

            LOGGER.info("Config loaded successfully:");
            LOGGER.info(" maxDeaths={}, healthPerDeath={}, maxExtraHearts={}, enableBan={}, banDuration={}, autoSaveInterval={}, banMessage={}",
                    maxDeaths, healthPerDeath, maxExtraHearts, enableBan, banDuration, autoSaveInterval, banMessage);
        } catch (Exception e) {
            LOGGER.error("Failed to load config! Regenerating default config.", e);
            saveDefaultConfig();
        }
    }

    public static void saveConfig() {
        try {
            JsonObject config = new JsonObject();
            config.addProperty("max_deaths", maxDeaths);
            config.addProperty("health_per_death", healthPerDeath);
            config.addProperty("max_extra_hearts", maxExtraHearts);
            config.addProperty("enable_ban", enableBan);
            config.addProperty("ban_duration", banDuration);
            config.addProperty("auto_save_interval", autoSaveInterval);
            config.addProperty("ban_message", banMessage);

            Files.createDirectories(CONFIG_PATH.getParent());

            try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(config, writer);
            }

            LOGGER.info("Config saved successfully.");
        } catch (IOException e) {
            LOGGER.error("Failed to save config.", e);
        }
    }

    private static void saveDefaultConfig() {
        LOGGER.info("Creating default config file...");
        saveConfig();
    }

    public static void reloadConfig() {
        LOGGER.info("Reloading configuration...");
        loadConfig();
        LOGGER.info("Configuration reloaded successfully.");
    }
}
