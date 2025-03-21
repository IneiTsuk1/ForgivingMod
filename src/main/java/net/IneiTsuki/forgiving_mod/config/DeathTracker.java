package net.IneiTsuki.forgiving_mod.config;

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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DeathTracker {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DATA_PATH = Paths.get("config/ForgivingMod/Data/Forgiving_data.json");
    private static final ConcurrentHashMap<UUID, Integer> deathTracker = new ConcurrentHashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(DeathTracker.class);
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    static {
        scheduler.scheduleAtFixedRate(DeathTracker::saveDeathData, 30, 30, TimeUnit.SECONDS);
    }

    public static void loadDeathData() {
        try {
            if (!Files.exists(DATA_PATH)) {
                saveDeathData();
                return;
            }

            try (Reader reader = Files.newBufferedReader(DATA_PATH)) {
                deathTracker.putAll(GSON.fromJson(reader, new TypeToken<ConcurrentHashMap<UUID, Integer>>() {}.getType()));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load death data: {}", e.getMessage(), e);
        }
    }

    public static void saveDeathData() {
        try {
            Files.createDirectories(DATA_PATH.getParent());

            try (BufferedWriter writer = Files.newBufferedWriter(DATA_PATH)) {
                GSON.toJson(deathTracker, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save death data: {}", e.getMessage(), e);
        }
    }

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
}
