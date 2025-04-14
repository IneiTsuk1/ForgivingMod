package net.IneiTsuki.forgivingmod.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.IneiTsuki.forgivingmod.config.ConfigManager;
import net.IneiTsuki.forgivingmod.config.DeathTracker;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.BannedPlayerList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Objects;
import java.util.UUID;

public class RegisterCommands {

    @SuppressWarnings("unused")
    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                CommandManager.literal("ForgivingMod")
                        .requires(source -> source.hasPermissionLevel(3))
                        .then(CommandManager.literal("Reset")
                                .then(CommandManager.argument("Player", StringArgumentType.word())
                                        .executes(context -> resetPlayer(context.getSource(), StringArgumentType.getString(context, "Player")))))
                        .then(CommandManager.literal("reload")
                                .executes(context -> reloadConfig(context.getSource())))));
    }

    public static int resetPlayer(ServerCommandSource source, String playerName) {
        MinecraftServer server = source.getServer();
        GameProfile profile = Objects.requireNonNull(server.getUserCache()).findByName(playerName).orElse(null);

        if (profile == null) {
            source.sendError(Text.literal("Player not found."));
            return 0;
        }

        UUID playerID = profile.getId();
        DeathTracker.resetDeathCount(playerID);
        source.sendFeedback(() -> Text.literal("Reset deaths for " + playerName), false);

        BannedPlayerList banList = server.getPlayerManager().getUserBanList();
        if (banList.contains(profile)) {
            banList.remove(profile);
            source.sendFeedback(() -> Text.literal("Unbanned " + playerName), false);
        }

        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerName);
        if (player != null) {
            resetPlayerHealth(player);
        }

        return 1;
    }

    public static int reloadConfig(ServerCommandSource source) {
        ConfigManager.reloadConfig();
        source.sendFeedback(() -> Text.literal("ForgivingMod configuration reloaded."), false);
        return 1;
    }

    public static void resetPlayerHealth(ServerPlayerEntity player) {
        EntityAttributeInstance healthAttribute = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (healthAttribute != null) {
            healthAttribute.setBaseValue(20.0);
            player.setHealth(player.getMaxHealth());
        }
    }
}
