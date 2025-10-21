package net.IneiTsuki.forgivingmod.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.IneiTsuki.forgivingmod.config.ConfigManager;
import net.IneiTsuki.forgivingmod.config.DeathTracker;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.BannedPlayerEntry;
import net.minecraft.server.BannedPlayerList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Objects;
import java.util.UUID;

public class RegisterCommands {

    @SuppressWarnings("unused")
    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("forgivingmod")
                        .requires(source -> source.hasPermissionLevel(3)) // Admin-only

                        // Reset player deaths & unban
                        .then(CommandManager.literal("reset")
                                .then(CommandManager.argument("player", StringArgumentType.word())
                                        .executes(context ->
                                                resetPlayer(context.getSource(), StringArgumentType.getString(context, "player")))))

                        // Reload config
                        .then(CommandManager.literal("reload")
                                .executes(context -> reloadConfig(context.getSource())))

                        // Add bonus lives
                        .then(CommandManager.literal("addlife")
                                .then(CommandManager.argument("player", StringArgumentType.word())
                                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(context -> addLife(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "player"),
                                                        IntegerArgumentType.getInteger(context, "amount"))))))

                        // Set max deaths per player
                        .then(CommandManager.literal("setmaxdeaths")
                                .then(CommandManager.argument("player", StringArgumentType.word())
                                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(context -> setMaxDeaths(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "player"),
                                                        IntegerArgumentType.getInteger(context, "amount"))))))

                        // Show server config
                        .then(CommandManager.literal("info")
                                .executes(context -> showConfigInfo(context.getSource())))

                        // Show player death count / stats
                        .then(CommandManager.literal("deathcount")
                                .then(CommandManager.argument("player", StringArgumentType.word())
                                        .executes(context -> showDeathCount(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "player"))))))
        );
    }

    // -----------------------------
    // Command Implementations
    // -----------------------------

    private static int resetPlayer(ServerCommandSource source, String playerName) {
        MinecraftServer server = source.getServer();
        GameProfile profile = Objects.requireNonNull(server.getUserCache())
                .findByName(playerName)
                .orElse(null);

        if (profile == null) {
            source.sendError(Text.literal("Player '" + playerName + "' not found or has never joined this server.")
                    .formatted(Formatting.RED));
            return 0;
        }

        UUID playerID = profile.getId();
        DeathTracker.resetDeathCount(playerID);
        DeathTracker.resetBonusLives(playerID);
        DeathTracker.setMaxDeaths(playerID, ConfigManager.getConfig().max_deaths);
        source.sendFeedback(() -> Text.literal("Reset death data for " + playerName).formatted(Formatting.GREEN), false);

        BannedPlayerList banList = server.getPlayerManager().getUserBanList();
        if (banList.contains(profile)) {
            banList.remove(profile);
            source.sendFeedback(() -> Text.literal("Unbanned " + playerName).formatted(Formatting.YELLOW), false);
        }

        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerName);
        if (player != null) {
            resetPlayerHealth(player);
        }

        return 1;
    }

    private static int reloadConfig(ServerCommandSource source) {
        ConfigManager.reloadConfig();
        source.sendFeedback(() -> Text.literal("ForgivingMod configuration reloaded successfully.")
                .formatted(Formatting.GREEN), false);
        return 1;
    }

    private static int addLife(ServerCommandSource source, String playerName, int amount) {
        MinecraftServer server = source.getServer();
        GameProfile profile = Objects.requireNonNull(server.getUserCache())
                .findByName(playerName)
                .orElse(null);

        if (profile == null) {
            source.sendError(Text.literal("Player '" + playerName + "' not found.").formatted(Formatting.RED));
            return 0;
        }

        DeathTracker.addBonusLives(profile.getId(), amount);
        source.sendFeedback(() -> Text.literal("Added " + amount + " bonus life(s) to " + playerName)
                .formatted(Formatting.GREEN), false);
        return 1;
    }

    private static int setMaxDeaths(ServerCommandSource source, String playerName, int maxDeaths) {
        MinecraftServer server = source.getServer();
        GameProfile profile = Objects.requireNonNull(server.getUserCache())
                .findByName(playerName)
                .orElse(null);

        if (profile == null) {
            source.sendError(Text.literal("Player '" + playerName + "' not found.").formatted(Formatting.RED));
            return 0;
        }

        DeathTracker.setMaxDeaths(profile.getId(), maxDeaths);
        source.sendFeedback(() -> Text.literal(playerName + "'s max deaths set to " + maxDeaths)
                .formatted(Formatting.GREEN), false);
        return 1;
    }

    private static int showConfigInfo(ServerCommandSource source) {
        var cfg = ConfigManager.getConfig();
        source.sendFeedback(() -> Text.literal(String.format(
                "=== ForgivingMod Config ===\n" +
                        "max_deaths: %d\n" +
                        "health_per_death: %d\n" +
                        "max_extra_hearts: %d\n" +
                        "enable_ban: %s\n" +
                        "ban_duration: %d\n" +
                        "auto_save_interval: %d\n" +
                        "ban_message: %s",
                cfg.max_deaths,
                cfg.health_per_death,
                cfg.max_extra_hearts,
                cfg.enable_ban,
                cfg.ban_duration,
                cfg.auto_save_interval,
                cfg.ban_message)).formatted(Formatting.AQUA), false);
        return 1;
    }

    private static int showDeathCount(ServerCommandSource source, String playerName) {
        MinecraftServer server = source.getServer();
        GameProfile profile = Objects.requireNonNull(server.getUserCache())
                .findByName(playerName)
                .orElse(null);

        if (profile == null) {
            source.sendError(Text.literal("Player '" + playerName + "' not found.").formatted(Formatting.RED));
            return 0;
        }

        final UUID playerID = profile.getId();
        final int deaths = DeathTracker.getDeathCount(playerID);
        final int remainingLives = Math.max(0,
                DeathTracker.getMaxDeaths(playerID) - deaths + DeathTracker.getBonusLives(playerID));

        final BannedPlayerList banList = server.getPlayerManager().getUserBanList();
        final BannedPlayerEntry banEntry = banList.get(profile);
        final boolean isBanned = banEntry != null;
        final String banInfo = isBanned
                ? "BANNED (" + (banEntry.getReason() != null ? banEntry.getReason() : "No reason provided") + ")"
                : "Not banned";

        final ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerName);
        final double currentHealth;
        final double maxHealth;

        if (player != null) {
            EntityAttributeInstance healthAttr = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
            if (healthAttr != null) {
                maxHealth = healthAttr.getBaseValue();
                currentHealth = player.getHealth();
            } else {
                maxHealth = 0;
                currentHealth = 0;
            }
        } else {
            maxHealth = 0;
            currentHealth = 0;
        }

        source.sendFeedback(() -> {
            StringBuilder sb = new StringBuilder()
                    .append("📊 ").append(playerName).append(" Stats:\n")
                    .append(" - Deaths: ").append(deaths).append("\n")
                    .append(" - Remaining Lives: ").append(remainingLives).append("\n")
                    .append(" - Bonus Lives: ").append(DeathTracker.getBonusLives(playerID)).append("\n")
                    .append(" - Max Deaths: ").append(DeathTracker.getMaxDeaths(playerID)).append("\n")
                    .append(" - Ban Status: ").append(banInfo).append("\n");

            if (player != null) {
                sb.append(String.format(" - Health: %.1f / %.1f\n", currentHealth, maxHealth));
                sb.append(" - Status: Online");
            } else {
                sb.append(" - Status: Offline");
            }

            return Text.literal(sb.toString()).formatted(Formatting.AQUA);
        }, false);

        return 1;
    }

    private static void resetPlayerHealth(ServerPlayerEntity player) {
        EntityAttributeInstance healthAttribute = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (healthAttribute != null) {
            healthAttribute.setBaseValue(20.0);
            player.setHealth(player.getMaxHealth());
        }
    }
}
