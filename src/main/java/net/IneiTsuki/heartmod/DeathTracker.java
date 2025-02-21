package net.IneiTsuki.heartmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.UUID;

public class DeathTracker {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DATA_PATH = Paths.get("config/HealthMod_data.json");
    private static final HashMap<UUID, Integer> deathTracker = new HashMap<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(DeathTracker.class);


    public static void loadDeathData() {
        try {
            if (Files.exists(DATA_PATH)) {
                deathTracker.putAll(GSON.fromJson(new FileReader(DATA_PATH.toFile()), new TypeToken<HashMap<UUID, Integer>>() {}.getType()));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load death data: {}", e.getMessage(), e);  // Use logger for error handling
        }
    }

    public static void saveDeathData() {
        try (Writer writer = new FileWriter(DATA_PATH.toFile())) {
            GSON.toJson(deathTracker, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save death data: {}", e.getMessage(), e);  // Use logger for error handling
        }
    }

    public static void incrementDeathCount(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        int deaths = getDeathCount(playerId) + 1;
        deathTracker.put(playerId, deaths);
        saveDeathData(); // Ensure data is saved immediately
    }

    public static void resetDeathCount(UUID playerId) {
        deathTracker.remove(playerId);
        saveDeathData();
    }

    public static int getDeathCount(UUID playerId) {
        return deathTracker.getOrDefault(playerId, 0);
    }
}
