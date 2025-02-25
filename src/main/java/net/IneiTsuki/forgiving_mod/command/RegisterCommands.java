package net.IneiTsuki.forgiving_mod.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.IneiTsuki.forgiving_mod.config.DeathTracker;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.BannedPlayerList;
import net.minecraft.text.Text;

import java.util.UUID;

public class RegisterCommands {

    @SuppressWarnings("unused")
    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("ForgivingMod")
                .requires(source -> source.hasPermissionLevel(2)) // Requires OP level 2
                .then(CommandManager.literal("Reset")
                        .then(CommandManager.argument("Player", StringArgumentType.word())
                                .executes(context -> resetPlayer(context.getSource(), StringArgumentType.getString(context, "Player")))))));
    }

    private static int resetPlayer(ServerCommandSource source, String playerName) {
        MinecraftServer server = source.getServer();
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerName);

        if (player == null) {
            source.sendError(Text.literal("Player not found or not online."));
            return 0;
        }

        UUID playerID = player.getUuid();

        // Reset the death count
        DeathTracker.resetDeathCount(playerID);
        source.sendFeedback(() -> Text.literal("Reset deaths for " + playerName), false);

        // Reset player's health to the default value (20.0)
        resetPlayerHealth(player);

        // Unban the player if they are banned
        BannedPlayerList banList = server.getPlayerManager().getUserBanList();
        if (banList.contains(player.getGameProfile())) {
            banList.remove(player.getGameProfile());
            source.sendFeedback(() -> Text.literal("Unbanned " + playerName), false);
        }
        return 1;
    }

    private static void resetPlayerHealth(ServerPlayerEntity player) {
        // Reset health to the default value (20.0)
        EntityAttributeInstance healthAttribute = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (healthAttribute != null) {
            healthAttribute.setBaseValue(20.0);  // Reset health to the base value (default health)
            player.setHealth(player.getMaxHealth());  // Restore full health
        }
    }
}
