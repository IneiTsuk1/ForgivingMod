package net.IneiTsuki.forgivingmod;

import net.IneiTsuki.forgivingmod.command.RegisterCommands;
import net.IneiTsuki.forgivingmod.config.ConfigManager;
import net.IneiTsuki.forgivingmod.config.DeathTracker;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.BannedPlayerEntry;
import net.minecraft.server.BannedPlayerList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class Forgivingmod implements ModInitializer {

    private static final String MOD_ID = "forgiving_mod";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Forgiving Mod...");

        // Load config and data
        ConfigManager.loadConfig();
        DeathTracker.loadDeathData();
        RegisterCommands.registerCommands();

        // Register player respawn and shutdown events
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> handlePlayerRespawn(newPlayer));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> shutDown());
    }

    private void shutDown() {
        LOGGER.info("Saving ForgivingMod data before shutdown...");
        DeathTracker.saveDeathData();
        ConfigManager.saveConfig();
        LOGGER.info("ForgivingMod data saved successfully.");
    }

    private void handlePlayerRespawn(ServerPlayerEntity player) {
        var cfg = ConfigManager.getConfig();
        UUID playerId = player.getUuid();
        int deaths = DeathTracker.getDeathCount(playerId);
        int maxDeaths = DeathTracker.getMaxDeaths(playerId);
        int remainingLives = Math.max(0, maxDeaths - deaths + DeathTracker.getBonusLives(playerId));

        if (remainingLives > 0) {
            DeathTracker.incrementDeathCount(player);
            applyExtraHealth(player, deaths + 1);

            int totalExtraHearts = ((deaths + 1) * cfg.health_per_death) / 2;

            if (remainingLives == 1) {
                player.sendMessage(Text.literal("The world is giving you one last chance! Hearts: " + totalExtraHearts), false);
            } else {
                player.sendMessage(Text.literal("The world is giving you another chance! Hearts: " + totalExtraHearts), false);
            }
        } else {
            if (cfg.enable_ban) {
                banPlayer(player);
            }
        }
    }

    private void applyExtraHealth(ServerPlayerEntity player, int deaths) {
        var cfg = ConfigManager.getConfig();
        EntityAttributeInstance healthAttr = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (healthAttr != null) {
            double newHealth = 20.0 + (deaths * cfg.health_per_death);
            healthAttr.setBaseValue(newHealth);
            player.setHealth(player.getMaxHealth()); // Fully heal on respawn
        }
    }

    private void banPlayer(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        BannedPlayerList banList = server.getPlayerManager().getUserBanList();

        // Avoid duplicate bans
        if (banList.contains(player.getGameProfile())) return;

        var cfg = ConfigManager.getConfig();

        // Calculate expiration time (if any)
        java.util.Date expirationDate = null;
        if (cfg.ban_duration > 0) {
            // You can change this multiplier to match your design:
            // * 60_000L = minutes
            // * 3_600_000L = hours
            // * 86_400_000L = days
            long durationMillis = cfg.ban_duration * 60_000L; // banDuration in minutes
            expirationDate = new java.util.Date(System.currentTimeMillis() + durationMillis);
        }

        // Create ban entry
        BannedPlayerEntry banEntry = new BannedPlayerEntry(
                player.getGameProfile(),
                new java.util.Date(),   // Ban date
                "ForgivingMod",         // Source
                expirationDate,         // Expiration (null = permanent)
                cfg.ban_message          // Reason
        );

        banList.add(banEntry);
        player.networkHandler.disconnect(Text.literal(cfg.ban_message));

        if (expirationDate == null) {
            LOGGER.info("Permanently banned player: {}", player.getGameProfile().getName());
        } else {
            LOGGER.info("Temporarily banned player: {} for {} minute(s)", player.getGameProfile().getName(), cfg.ban_duration);
        }
    }
}
