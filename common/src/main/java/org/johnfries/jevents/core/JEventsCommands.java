package org.johnfries.jevents.core;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.johnfries.jevents.data.JEventsSavedData;

import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class JEventsCommands {
    private JEventsCommands() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("jevents")
                .requires(source -> source.hasPermissionLevel(3))
                .then(CommandManager.literal("status")
                        .executes(JEventsCommands::status))
                .then(CommandManager.literal("hardcore")
                        .then(CommandManager.literal("enabled")
                                .then(CommandManager.argument("value", BoolArgumentType.bool())
                                        .executes(context -> setHardcoreEnabled(context.getSource(), BoolArgumentType.getBool(context, "value")))))
                        .then(CommandManager.literal("spectator")
                                .then(CommandManager.argument("value", BoolArgumentType.bool())
                                        .executes(context -> setHardcoreSpectator(context.getSource(), BoolArgumentType.getBool(context, "value"))))))
                .then(CommandManager.literal("sgspawn")
                        .then(CommandManager.literal("set")
                                .executes(context -> setSgSpawn(context.getSource(), 6))
                                .then(CommandManager.argument("radius", IntegerArgumentType.integer(1, 128))
                                        .executes(context -> setSgSpawn(context.getSource(), IntegerArgumentType.getInteger(context, "radius")))))
                        .then(CommandManager.literal("clear")
                                .executes(context -> clearSgSpawn(context.getSource())))
                        .then(CommandManager.literal("tpall")
                                .executes(context -> teleportAllToSgSpawn(context.getSource()))))
                .then(CommandManager.literal("ban")
                        .then(CommandManager.literal("add")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .executes(context -> addBan(context.getSource(), EntityArgumentType.getPlayer(context, "player")))))
                        .then(CommandManager.literal("remove")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .executes(context -> removeBan(context.getSource(), EntityArgumentType.getPlayer(context, "player")))))
                        .then(CommandManager.literal("list")
                                .executes(context -> listBans(context.getSource()))))
                .then(CommandManager.literal("whitelist")
                        .then(CommandManager.literal("add")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .executes(context -> whitelistAdd(context.getSource(), EntityArgumentType.getPlayer(context, "player")))))
                        .then(CommandManager.literal("remove")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .executes(context -> whitelistRemove(context.getSource(), EntityArgumentType.getPlayer(context, "player")))))
                        .then(CommandManager.literal("list")
                                .executes(context -> whitelistList(context.getSource()))))
                .then(CommandManager.literal("chatmute")
                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                                .executes(context -> setChatMuted(context.getSource(), BoolArgumentType.getBool(context, "value")))))
                .then(CommandManager.literal("lifesteal")
                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                                .executes(context -> setLifesteal(context.getSource(), BoolArgumentType.getBool(context, "value")))))
                .then(CommandManager.literal("lastbreath")
                        .then(CommandManager.literal("enabled")
                                .then(CommandManager.argument("value", BoolArgumentType.bool())
                                        .executes(context -> setLastBreath(context.getSource(), BoolArgumentType.getBool(context, "value")))))
                        .then(CommandManager.literal("protect")
                                .then(CommandManager.literal("add")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> protectPlayer(context.getSource(), EntityArgumentType.getPlayer(context, "player"), true))))
                                .then(CommandManager.literal("remove")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> protectPlayer(context.getSource(), EntityArgumentType.getPlayer(context, "player"), false))))
                                .then(CommandManager.literal("list")
                                        .executes(context -> listProtectedPlayers(context.getSource())))))
                .then(CommandManager.literal("modmode")
                        .then(CommandManager.literal("set")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                                                .executes(context -> setModMode(
                                                        context.getSource(),
                                                        EntityArgumentType.getPlayer(context, "player"),
                                                        BoolArgumentType.getBool(context, "value")
                                                )))))
                        .then(CommandManager.literal("flightspeed")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .then(CommandManager.argument("speed", DoubleArgumentType.doubleArg(0.01D, 0.5D))
                                                .executes(context -> setModFlightSpeed(
                                                        context.getSource(),
                                                        EntityArgumentType.getPlayer(context, "player"),
                                                        (float) DoubleArgumentType.getDouble(context, "speed")
                                                )))))
                        .then(CommandManager.literal("list")
                                .executes(context -> listModModePlayers(context.getSource()))))
                .then(CommandManager.literal("inventory")
                        .then(CommandManager.literal("restore")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .executes(context -> restoreInventory(context.getSource(), EntityArgumentType.getPlayer(context, "player"))))))
                .then(CommandManager.literal("rename")
                        .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                .executes(context -> renameHeldItem(context.getSource(), StringArgumentType.getString(context, "name")))))
                .then(CommandManager.literal("mobrate")
                        .then(CommandManager.literal("global")
                                .then(CommandManager.argument("multiplier", DoubleArgumentType.doubleArg(0.0D, 4.0D))
                                        .executes(context -> setGlobalMobRate(context.getSource(), DoubleArgumentType.getDouble(context, "multiplier")))))
                        .then(CommandManager.literal("dynamic")
                                .then(CommandManager.argument("value", BoolArgumentType.bool())
                                        .executes(context -> setDynamicMobRate(context.getSource(), BoolArgumentType.getBool(context, "value")))))
                        .then(CommandManager.literal("player")
                                .then(CommandManager.literal("set")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .then(CommandManager.argument("multiplier", DoubleArgumentType.doubleArg(0.0D, 4.0D))
                                                        .executes(context -> setPlayerMobRate(
                                                                context.getSource(),
                                                                EntityArgumentType.getPlayer(context, "player"),
                                                                DoubleArgumentType.getDouble(context, "multiplier")
                                                        )))))
                                .then(CommandManager.literal("clear")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> clearPlayerMobRate(context.getSource(), EntityArgumentType.getPlayer(context, "player"))))))
                        .then(CommandManager.literal("list")
                                .executes(context -> listMobRates(context.getSource())))));
    }

    private static int status(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        JEventsSavedData data = JEventsSavedData.get(source.getMinecraftServer());

        success(source, "Hardcore: " + data.isHardcoreEnabled() + " (spectator on death: " + data.isHardcoreSpectatorEnabled() + ")");
        success(source, "Lifesteal: " + data.isLifestealEnabled() + " | Last Breath: " + data.isLastBreathEnabled() + " | Chat muted: " + data.isChatMuted());
        success(source, "Event bans: " + data.getEventBannedPlayers().size() + " | Chat whitelist: " + data.getChatWhitelistPlayers().size() + " | Mod mode: " + data.getModModePlayers().size());
        success(source, "Mob rate global: " + formatDouble(data.getGlobalMobSpawnMultiplier()) + " | dynamic: " + data.isDynamicMobSpawnScaling() + " | player overrides: " + data.getPlayerMobSpawnMultiplier().size());
        if (data.hasSgSpawn()) {
            success(source, "SG spawn: " + data.getSgSpawnDimension() + " @ " + data.getSgSpawnX() + ", " + data.getSgSpawnY() + ", " + data.getSgSpawnZ() + " (r=" + data.getSgSpawnRadius() + ")");
        } else {
            success(source, "SG spawn: not set");
        }
        return 1;
    }

    private static int setHardcoreEnabled(ServerCommandSource source, boolean value) {
        JEventsSavedData data = JEventsSavedData.get(source.getMinecraftServer());
        data.setHardcoreEnabled(value);
        success(source, "Hardcore set to " + value + ".");
        return 1;
    }

    private static int setHardcoreSpectator(ServerCommandSource source, boolean value) {
        JEventsSavedData data = JEventsSavedData.get(source.getMinecraftServer());
        data.setHardcoreSpectatorEnabled(value);
        success(source, "Hardcore spectator mode set to " + value + ".");
        return 1;
    }

    private static int setSgSpawn(ServerCommandSource source, int radius) {
        JEventsSavedData data = JEventsSavedData.get(source.getMinecraftServer());
        BlockPos origin = new BlockPos(source.getPosition());
        Identifier dimension = source.getWorld().getRegistryKey().getValue();
        data.setSgSpawn(dimension, origin.getX(), origin.getY(), origin.getZ(), radius);
        success(source, "SG spawn saved at " + dimension + " [" + origin.getX() + ", " + origin.getY() + ", " + origin.getZ() + "] radius " + radius + ".");
        return 1;
    }

    private static int clearSgSpawn(ServerCommandSource source) {
        JEventsSavedData data = JEventsSavedData.get(source.getMinecraftServer());
        data.clearSgSpawn();
        success(source, "SG spawn cleared.");
        return 1;
    }

    private static int teleportAllToSgSpawn(ServerCommandSource source) {
        JEventsSavedData data = JEventsSavedData.get(source.getMinecraftServer());
        if (!data.hasSgSpawn()) {
            failure(source, "SG spawn is not set.");
            return 0;
        }

        RegistryKey<World> key = RegistryKey.of(Registry.WORLD_KEY, data.getSgSpawnDimension());
        ServerWorld targetWorld = source.getMinecraftServer().getWorld(key);
        if (targetWorld == null) {
            failure(source, "SG spawn dimension is unavailable: " + data.getSgSpawnDimension());
            return 0;
        }

        int playerCount = source.getMinecraftServer().getPlayerManager().getPlayerList().size();
        if (playerCount == 0) {
            failure(source, "There are no online players.");
            return 0;
        }

        double centerX = data.getSgSpawnX() + 0.5D;
        double centerY = data.getSgSpawnY();
        double centerZ = data.getSgSpawnZ() + 0.5D;
        int radius = data.getSgSpawnRadius();

        int index = 0;
        for (ServerPlayerEntity player : source.getMinecraftServer().getPlayerManager().getPlayerList()) {
            double angle = (Math.PI * 2.0D * index) / playerCount;
            double x = centerX + Math.cos(angle) * radius;
            double z = centerZ + Math.sin(angle) * radius;
            float yaw = (float) ((Math.toDegrees(angle) + 180.0D) % 360.0D);
            player.teleport(targetWorld, x, centerY, z, yaw, 0.0F);
            index++;
        }

        success(source, "Teleported " + playerCount + " players to SG spawn.");
        return playerCount;
    }

    private static int addBan(ServerCommandSource source, ServerPlayerEntity player) {
        JEventsSavedData data = JEventsSavedData.get(source.getMinecraftServer());
        if (!data.addEventBannedPlayer(player.getUuid())) {
            failure(source, player.getGameProfile().getName() + " is already event-banned.");
            return 0;
        }

        player.networkHandler.disconnect(new LiteralText("You were event-banned by staff."));
        success(source, "Event-banned " + player.getGameProfile().getName() + ".");
        return 1;
    }

    private static int removeBan(ServerCommandSource source, ServerPlayerEntity player) {
        JEventsSavedData data = JEventsSavedData.get(source.getMinecraftServer());
        if (!data.removeEventBannedPlayer(player.getUuid())) {
            failure(source, player.getGameProfile().getName() + " is not on the event ban list.");
            return 0;
        }
        success(source, "Removed event ban for " + player.getGameProfile().getName() + ".");
        return 1;
    }

    private static int listBans(ServerCommandSource source) {
        JEventsSavedData data = JEventsSavedData.get(source.getMinecraftServer());
        Set<UUID> banned = data.getEventBannedPlayers();
        if (banned.isEmpty()) {
            success(source, "Event ban list is empty.");
            return 1;
        }

        String names = banned.stream()
                .sorted(Comparator.comparing(UUID::toString))
                .map(uuid -> displayPlayer(source.getMinecraftServer(), uuid))
                .collect(Collectors.joining(", "));
        success(source, "Event bans (" + banned.size() + "): " + names);
        return banned.size();
    }

    private static int whitelistAdd(ServerCommandSource source, ServerPlayerEntity player) {
        JEventsSavedData data = JEventsSavedData.get(source.getMinecraftServer());
        if (!data.addChatWhitelistPlayer(player.getUuid())) {
            failure(source, player.getGameProfile().getName() + " is already in the chat whitelist.");
            return 0;
        }
        success(source, "Added " + player.getGameProfile().getName() + " to chat whitelist.");
        return 1;
    }

    private static int whitelistRemove(ServerCommandSource source, ServerPlayerEntity player) {
        JEventsSavedData data = JEventsSavedData.get(source.getMinecraftServer());
        if (!data.removeChatWhitelistPlayer(player.getUuid())) {
            failure(source, player.getGameProfile().getName() + " is not in the chat whitelist.");
            return 0;
        }
        success(source, "Removed " + player.getGameProfile().getName() + " from chat whitelist.");
        return 1;
    }

    private static int whitelistList(ServerCommandSource source) {
        JEventsSavedData data = JEventsSavedData.get(source.getMinecraftServer());
        Set<UUID> entries = data.getChatWhitelistPlayers();
        if (entries.isEmpty()) {
            success(source, "Chat whitelist is empty.");
            return 1;
        }

        String names = entries.stream()
                .sorted(Comparator.comparing(UUID::toString))
                .map(uuid -> displayPlayer(source.getMinecraftServer(), uuid))
                .collect(Collectors.joining(", "));
        success(source, "Chat whitelist (" + entries.size() + "): " + names);
        return entries.size();
    }

    private static int setChatMuted(ServerCommandSource source, boolean value) {
        JEventsSavedData data = JEventsSavedData.get(source.getMinecraftServer());
        data.setChatMuted(value);
        success(source, "Chat mute set to " + value + ".");
        return 1;
    }

    private static int setLifesteal(ServerCommandSource source, boolean value) {
        JEventsSavedData data = JEventsSavedData.get(source.getMinecraftServer());
        data.setLifestealEnabled(value);
        success(source, "Lifesteal set to " + value + ".");
        return 1;
    }

    private static int setLastBreath(ServerCommandSource source, boolean value) {
        JEventsSavedData data = JEventsSavedData.get(source.getMinecraftServer());
        data.setLastBreathEnabled(value);
        success(source, "Last Breath set to " + value + ".");
        return 1;
    }

    private static int protectPlayer(ServerCommandSource source, ServerPlayerEntity player, boolean protect) {
        JEventsSavedData data = JEventsSavedData.get(source.getMinecraftServer());
        boolean changed = protect ? data.addLastBreathProtectedPlayer(player.getUuid()) : data.removeLastBreathProtectedPlayer(player.getUuid());
        if (!changed) {
            failure(source, player.getGameProfile().getName() + " is already " + (protect ? "protected." : "not protected."));
            return 0;
        }

        success(source, (protect ? "Protected " : "Unprotected ") + player.getGameProfile().getName() + " for Last Breath.");
        return 1;
    }

    private static int listProtectedPlayers(ServerCommandSource source) {
        JEventsSavedData data = JEventsSavedData.get(source.getMinecraftServer());
        Set<UUID> protectedPlayers = data.getLastBreathProtectedPlayers();
        if (protectedPlayers.isEmpty()) {
            success(source, "Last Breath protected list is empty.");
            return 1;
        }

        String names = protectedPlayers.stream()
                .sorted(Comparator.comparing(UUID::toString))
                .map(uuid -> displayPlayer(source.getMinecraftServer(), uuid))
                .collect(Collectors.joining(", "));
        success(source, "Last Breath protected (" + protectedPlayers.size() + "): " + names);
        return protectedPlayers.size();
    }

    private static int setModMode(ServerCommandSource source, ServerPlayerEntity player, boolean value) {
        JEventsSavedData data = JEventsSavedData.get(source.getMinecraftServer());
        data.setModMode(player.getUuid(), value);
        float speed = data.getModFlightSpeed(player.getUuid());
        JEventsService.applyModMode(player, value, speed);
        success(source, "Mod Mode for " + player.getGameProfile().getName() + " set to " + value + ".");
        return 1;
    }

    private static int setModFlightSpeed(ServerCommandSource source, ServerPlayerEntity player, float speed) {
        JEventsSavedData data = JEventsSavedData.get(source.getMinecraftServer());
        float clamped = JEventsService.clampFlightSpeed(speed);
        data.setModFlightSpeed(player.getUuid(), clamped);
        if (data.isModModeEnabled(player.getUuid())) {
            JEventsService.applyModMode(player, true, clamped);
        }
        success(source, "Mod Mode flight speed for " + player.getGameProfile().getName() + " set to " + clamped + ".");
        return 1;
    }

    private static int listModModePlayers(ServerCommandSource source) {
        JEventsSavedData data = JEventsSavedData.get(source.getMinecraftServer());
        Set<UUID> modMode = data.getModModePlayers();
        if (modMode.isEmpty()) {
            success(source, "No players currently have Mod Mode enabled.");
            return 1;
        }

        String names = modMode.stream()
                .sorted(Comparator.comparing(UUID::toString))
                .map(uuid -> displayPlayer(source.getMinecraftServer(), uuid) + "@" + formatDouble(data.getModFlightSpeed(uuid)))
                .collect(Collectors.joining(", "));
        success(source, "Mod Mode players (" + modMode.size() + "): " + names);
        return modMode.size();
    }

    private static int restoreInventory(ServerCommandSource source, ServerPlayerEntity target) {
        JEventsSavedData data = JEventsSavedData.get(source.getMinecraftServer());
        if (!JEventsService.restoreInventory(target, data, true)) {
            failure(source, "No saved inventory backup exists for " + target.getGameProfile().getName() + ".");
            return 0;
        }
        success(source, "Restored inventory for " + target.getGameProfile().getName() + ".");
        return 1;
    }

    private static int renameHeldItem(ServerCommandSource source, String newName) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayer();
        ItemStack held = player.getMainHandStack();
        if (held.isEmpty()) {
            failure(source, "Hold an item in your main hand first.");
            return 0;
        }

        held.setCustomName(new LiteralText(newName));
        success(source, "Renamed held item to \"" + newName + "\".");
        return 1;
    }

    private static int setGlobalMobRate(ServerCommandSource source, double multiplier) {
        JEventsSavedData data = JEventsSavedData.get(source.getMinecraftServer());
        data.setGlobalMobSpawnMultiplier(multiplier);
        success(source, "Global mob spawn multiplier set to " + formatDouble(multiplier) + ".");
        return 1;
    }

    private static int setDynamicMobRate(ServerCommandSource source, boolean value) {
        JEventsSavedData data = JEventsSavedData.get(source.getMinecraftServer());
        data.setDynamicMobSpawnScaling(value);
        success(source, "Dynamic mob spawn scaling set to " + value + ".");
        return 1;
    }

    private static int setPlayerMobRate(ServerCommandSource source, ServerPlayerEntity player, double multiplier) {
        JEventsSavedData data = JEventsSavedData.get(source.getMinecraftServer());
        data.setPlayerMobSpawnMultiplier(player.getUuid(), multiplier);
        success(source, "Mob spawn multiplier for " + player.getGameProfile().getName() + " set to " + formatDouble(multiplier) + ".");
        return 1;
    }

    private static int clearPlayerMobRate(ServerCommandSource source, ServerPlayerEntity player) {
        JEventsSavedData data = JEventsSavedData.get(source.getMinecraftServer());
        data.clearPlayerMobSpawnMultiplier(player.getUuid());
        success(source, "Cleared player mob spawn override for " + player.getGameProfile().getName() + ".");
        return 1;
    }

    private static int listMobRates(ServerCommandSource source) {
        JEventsSavedData data = JEventsSavedData.get(source.getMinecraftServer());
        Map<UUID, Double> overrides = data.getPlayerMobSpawnMultiplier();
        if (overrides.isEmpty()) {
            success(source, "Mob overrides: none. Global=" + formatDouble(data.getGlobalMobSpawnMultiplier()) + ", dynamic=" + data.isDynamicMobSpawnScaling());
            return 1;
        }

        String formatted = overrides.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(UUID::toString)))
                .map(entry -> displayPlayer(source.getMinecraftServer(), entry.getKey()) + "=" + formatDouble(entry.getValue()))
                .collect(Collectors.joining(", "));
        success(source, "Mob overrides (" + overrides.size() + "): " + formatted);
        return overrides.size();
    }

    private static String displayPlayer(MinecraftServer server, UUID uuid) {
        ServerPlayerEntity online = server.getPlayerManager().getPlayer(uuid);
        if (online != null) {
            return online.getGameProfile().getName();
        }
        return uuid.toString();
    }

    private static String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static void success(ServerCommandSource source, String message) {
        source.sendFeedback(new LiteralText("[jEvents] " + message), false);
    }

    private static void failure(ServerCommandSource source, String message) {
        source.sendError(new LiteralText("[jEvents] " + message));
    }
}
