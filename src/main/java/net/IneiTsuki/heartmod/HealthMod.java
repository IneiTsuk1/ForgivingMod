package net.IneiTsuki.heartmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.BannedPlayerEntry;
import net.minecraft.server.BannedPlayerList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

public class HealthMod implements ModInitializer {

    @Override
    public void onInitialize() {
        // Load config and death data
        ConfigManager.loadConfig();
        DeathTracker.loadDeathData();
        RegisterCommands.registerCommands();

        // Register player respawn event
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> handlePlayerDeath(newPlayer));
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
                "HealthMod",  // Source (who banned them)
                null,  // Expiration date (null means permanent ban)
                ConfigManager.banMessage
        );

        banList.add(banEntry); // Add the player to the ban list
        player.networkHandler.disconnect(Text.literal(ConfigManager.banMessage)); // Kick them from the server
    }
}
