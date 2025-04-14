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
        LOGGER.info("Initializing Forgiving Mod... ");

        // Load config and death data
        ConfigManager.loadConfig();
        DeathTracker.loadDeathData();
        RegisterCommands.registerCommands();


        // register player respawn event and server stopping
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> handlePlayerDeath(newPlayer));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> shutDown());
    }

    private void shutDown() {
        LOGGER.info("Saving ForgivingMod data before shutdown...");
        DeathTracker.saveDeathData();
        ConfigManager.saveConfig();
        LOGGER.info("ForgivingMod data saved successfully.");
    }

    private void handlePlayerDeath(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        int deaths = DeathTracker.getDeathCount(playerId);

        if (deaths < (ConfigManager.maxExtraHearts / ConfigManager.healthPerDeath)) {
            // Increase death count and apply health
            DeathTracker.incrementDeathCount(player);
            applyExtraHealth(player, deaths + 1);
            player.sendMessage(Text.literal("You gained extra health! Total extra hearts: " + ((deaths + 1) * ConfigManager.healthPerDeath / 2)), false);
        } else {
            if (ConfigManager.enableBan) {
                banPlayer(player);
            }
        }
    }

    // Apply extra health properly
    private void applyExtraHealth(ServerPlayerEntity player, int deaths) {
        EntityAttributeInstance healthAttribute = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (healthAttribute != null) {
            double newHealth = 20.0 + (deaths * ConfigManager.healthPerDeath);
            healthAttribute.setBaseValue(newHealth);

            // Ensure the player gets full health after respawn
            player.setHealth(player.getMaxHealth());
        }
    }

    private void banPlayer(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        BannedPlayerList banList = server.getPlayerManager().getUserBanList();

        // Check if the player is already banned
        if (banList.contains(player.getGameProfile())) {
            return; // Player is already banned, do nothing
        }

        // If not banned, proceed with banning the player
        BannedPlayerEntry banEntry = new BannedPlayerEntry(
                player.getGameProfile(),
                null,  // Ban date (null means it takes effect immediately)
                "Forgiving",  // Source (who banned them)
                null,  // Expiration date (null means permanent ban)
                ConfigManager.banMessage
        );

        banList.add(banEntry); // Add the player to the ban list
        player.networkHandler.disconnect(Text.literal(ConfigManager.banMessage)); // Kick them from the server
        LOGGER.info("Banned player: {}", player.getGameProfile().getName());
    }
}
