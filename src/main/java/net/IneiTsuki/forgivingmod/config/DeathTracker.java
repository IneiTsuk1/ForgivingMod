package net.IneiTsuki.forgivingmod.config;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class DeathTracker {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DATA_PATH = Paths.get("config/ForgivingMod/Data/Forgiving_data.json");
    private static final ConcurrentHashMap<UUID, Integer> deathTracker = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Integer> bonusLives = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Integer> maxDeathOverrides = new ConcurrentHashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(DeathTracker.class);
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    static {
        scheduler.scheduleAtFixedRate(DeathTracker::saveDeathData, 30, 30, TimeUnit.SECONDS);
    }

    // Bonus lives
    public static void addBonusLives(UUID playerId, int amount) {
        bonusLives.merge(playerId, amount, Integer::sum);
    }

    public static int getBonusLives(UUID playerId) {
        return bonusLives.getOrDefault(playerId, 0);
    }

    public static void resetBonusLives(UUID playerId) {
        bonusLives.remove(playerId);
    }

    // Max death overrides
    public static void setMaxDeaths(UUID playerId, int maxDeaths) {
        maxDeathOverrides.put(playerId, maxDeaths);
    }

    public static int getMaxDeaths(UUID playerId) {
        return maxDeathOverrides.getOrDefault(playerId, ConfigManager.getConfig().max_deaths);
    }

    // Death count
    public static void incrementDeathCount(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        deathTracker.merge(playerId, 1, Integer::sum);
    }

    public static void resetDeathCount(UUID playerId) {
        deathTracker.remove(playerId);
    }

    public static int getDeathCount(UUID playerId) {
        return deathTracker.getOrDefault(playerId, 0);
    }

    // Load all data (deaths + bonus lives + max overrides)
    public static void loadDeathData() {
        if (!Files.exists(DATA_PATH)) {
            saveDeathData();
            return;
        }

        try (Reader reader = Files.newBufferedReader(DATA_PATH)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            Type type = new TypeToken<ConcurrentHashMap<UUID, Integer>>() {}.getType();

            if (json.has("deaths"))
                deathTracker.putAll(GSON.fromJson(json.get("deaths"), type));

            if (json.has("bonusLives"))
                bonusLives.putAll(GSON.fromJson(json.get("bonusLives"), type));

            if (json.has("maxDeaths"))
                maxDeathOverrides.putAll(GSON.fromJson(json.get("maxDeaths"), type));

            LOGGER.info("DeathTracker data loaded successfully.");
        } catch (Exception e) {
            LOGGER.error("Failed to load death data: {}", e.getMessage(), e);
        }
    }

    // Save all data
    public static void saveDeathData() {
        try {
            Files.createDirectories(DATA_PATH.getParent());

            CompletableFuture.runAsync(() -> {
                try (BufferedWriter writer = Files.newBufferedWriter(DATA_PATH)) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("deaths", deathTracker);
                    data.put("bonusLives", bonusLives);
                    data.put("maxDeaths", maxDeathOverrides);
                    GSON.toJson(data, writer);
                } catch (IOException e) {
                    LOGGER.error("Failed to save death data: {}", e.getMessage(), e);
                }
            });
        } catch (IOException e) {
            LOGGER.error("Failed to save death data: {}", e.getMessage(), e);
        }
    }
}
