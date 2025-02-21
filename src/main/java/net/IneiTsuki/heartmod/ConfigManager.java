package net.IneiTsuki.heartmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("HealthMod");
    private static final Path CONFIG_PATH = Paths.get("config/HealthMod.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static int maxExtraHearts = 10;
    public static int healthPerDeath = 2;
    public static boolean enableBan = true;
    public static String banMessage = "You died too many times and have been banned!";

    public static void loadConfig() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                saveDefaultConfig();
            }

            String json = new String(Files.readAllBytes(CONFIG_PATH));
            JsonObject config = JsonParser.parseString(json).getAsJsonObject();

            maxExtraHearts = config.has("max_extra_hearts") ? config.get("max_extra_hearts").getAsInt() : maxExtraHearts;
            healthPerDeath = config.has("health_per_death") ? config.get("health_per_death").getAsInt() : healthPerDeath;
            enableBan = config.has("enable_ban") ? config.get("enable_ban").getAsBoolean() : enableBan;
            banMessage = config.has("ban_message") ? config.get("ban_message").getAsString() : banMessage;

            LOGGER.info("Loaded config: maxExtraHearts={}, healthPerDeath={}, enableBan={}, banMessage={}", maxExtraHearts, healthPerDeath, enableBan, banMessage);
        } catch (Exception e) {
            LOGGER.error("Failed to load config! Using defaults.", e);
        }
    }

    private static void saveDefaultConfig() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            JsonObject config = new JsonObject();
            config.addProperty("max_extra_hearts", maxExtraHearts);
            config.addProperty("health_per_death", healthPerDeath);
            config.addProperty("enable_ban", enableBan);
            config.addProperty("ban_message", banMessage);

            try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
                GSON.toJson(config, writer);
            }

            LOGGER.info("Created default config file.");
        } catch (IOException e) {
            LOGGER.error("Failed to save default config.", e);
        }
    }
}
