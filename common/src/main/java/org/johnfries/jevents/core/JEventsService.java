package org.johnfries.jevents.core;

import me.shedaniel.architectury.event.EventResult;
import me.shedaniel.architectury.event.events.ChatEvent;
import me.shedaniel.architectury.event.events.CommandRegistrationEvent;
import me.shedaniel.architectury.event.events.EntityEvent;
import me.shedaniel.architectury.event.events.PlayerEvent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.GameMode;
import net.minecraft.world.MobSpawnerLogic;
import net.minecraft.world.WorldAccess;
import org.johnfries.jevents.data.JEventsSavedData;

import java.util.UUID;

public final class JEventsService {
    private static final UUID SYSTEM_SENDER = new UUID(0L, 0L);

    private JEventsService() {
    }

    public static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, selection) -> JEventsCommands.register(dispatcher));
        PlayerEvent.PLAYER_JOIN.register(JEventsService::onPlayerJoin);
        PlayerEvent.PLAYER_RESPAWN.register(JEventsService::onPlayerRespawn);
        EntityEvent.LIVING_DEATH.register(JEventsService::onLivingDeath);
        ChatEvent.SERVER.register(JEventsService::onChatMessage);
        EntityEvent.LIVING_CHECK_SPAWN.register(JEventsService::onCheckSpawn);
    }

    private static void onPlayerJoin(ServerPlayerEntity player) {
        JEventsSavedData data = JEventsSavedData.get(player.server);
        UUID playerId = player.getUuid();

        if (data.isEventBanned(playerId)) {
            player.networkHandler.disconnect(new LiteralText("You are banned from participating in jEvents-managed events on this server."));
            return;
        }

        if (data.isModModeEnabled(playerId)) {
            applyModMode(player, true, data.getModFlightSpeed(playerId));
        }
    }

    private static void onPlayerRespawn(ServerPlayerEntity player, boolean conqueredEnd) {
        JEventsSavedData data = JEventsSavedData.get(player.server);
        UUID playerId = player.getUuid();

        if (data.isHardcoreEnabled() && data.isHardcoreSpectatorEnabled()) {
            player.setGameMode(GameMode.SPECTATOR);
            player.sendSystemMessage(new LiteralText("[jEvents] Hardcore is enabled. You were moved to spectator mode."), SYSTEM_SENDER);
        }

        if (data.isModModeEnabled(playerId)) {
            applyModMode(player, true, data.getModFlightSpeed(playerId));
        }
    }

    private static ActionResult onLivingDeath(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof ServerPlayerEntity)) {
            return ActionResult.PASS;
        }

        ServerPlayerEntity player = (ServerPlayerEntity) entity;
        if (player.world.isClient) {
            return ActionResult.PASS;
        }

        JEventsSavedData data = JEventsSavedData.get(player.server);
        UUID playerId = player.getUuid();

        if (data.isLastBreathEnabled() && data.isLastBreathProtected(playerId)) {
            player.setHealth(1.0F);
            player.timeUntilRegen = 40;
            player.sendSystemMessage(new LiteralText("[jEvents] Last Breath prevented your death."), SYSTEM_SENDER);
            return ActionResult.FAIL;
        }

        snapshotInventory(player, data);

        if (data.isLifestealEnabled()) {
            applyLifesteal(player, source);
        }

        if (data.isHardcoreEnabled() && !data.isHardcoreSpectatorEnabled()) {
            data.addEventBannedPlayer(playerId);
            player.networkHandler.disconnect(new LiteralText("[jEvents] Hardcore is enabled. You died and are now event-banned."));
        }

        return ActionResult.PASS;
    }

    private static TypedActionResult<Text> onChatMessage(ServerPlayerEntity player, String rawMessage, Text component) {
        JEventsSavedData data = JEventsSavedData.get(player.server);
        if (!data.isChatMuted()) {
            return TypedActionResult.pass(component);
        }

        if (data.isChatWhitelisted(player.getUuid())) {
            return TypedActionResult.pass(component);
        }

        player.sendSystemMessage(new LiteralText("[jEvents] Chat is muted right now."), SYSTEM_SENDER);
        return TypedActionResult.fail(component);
    }

    private static EventResult onCheckSpawn(LivingEntity entity, WorldAccess world, double x, double y, double z, SpawnReason reason, MobSpawnerLogic spawner) {
        if (!(entity instanceof HostileEntity) || !(world instanceof ServerWorld)) {
            return EventResult.pass();
        }

        ServerWorld serverWorld = (ServerWorld) world;
        JEventsSavedData data = JEventsSavedData.get(serverWorld.getServer());

        double multiplier = Math.max(0.0D, data.getGlobalMobSpawnMultiplier());
        if (data.isDynamicMobSpawnScaling()) {
            int players = Math.max(1, serverWorld.getPlayers().size());
            double dynamic = Math.max(0.20D, Math.min(1.0D, 16.0D / players));
            multiplier *= dynamic;
        }

        for (ServerPlayerEntity nearby : serverWorld.getPlayers()) {
            double distanceSq = nearby.squaredDistanceTo(x, y, z);
            if (distanceSq <= 64.0D * 64.0D) {
                multiplier *= data.getPlayerMobSpawnMultiplier(nearby.getUuid());
            }
        }

        if (multiplier <= 0.0D) {
            return EventResult.interruptFalse();
        }

        if (multiplier < 1.0D && serverWorld.random.nextDouble() > multiplier) {
            return EventResult.interruptFalse();
        }

        if (multiplier > 1.0D) {
            double forceChance = Math.min(0.95D, multiplier - 1.0D);
            if (serverWorld.random.nextDouble() < forceChance) {
                return EventResult.interruptTrue();
            }
        }

        return EventResult.pass();
    }

    public static void applyModMode(ServerPlayerEntity player, boolean enabled, float flightSpeed) {
        if (enabled) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));
            player.abilities.allowFlying = true;
            player.abilities.flying = true;
            player.abilities.setFlySpeed(clampFlightSpeed(flightSpeed));
        } else {
            player.removeStatusEffect(StatusEffects.INVISIBILITY);
            if (!player.isCreative() && !player.isSpectator()) {
                player.abilities.allowFlying = false;
                player.abilities.flying = false;
            }
            player.abilities.setFlySpeed(0.05F);
        }
        player.sendAbilitiesUpdate();
    }

    public static float clampFlightSpeed(float speed) {
        if (speed < 0.01F) {
            return 0.01F;
        }
        return Math.min(speed, 0.5F);
    }

    private static void snapshotInventory(ServerPlayerEntity player, JEventsSavedData data) {
        NbtCompound backup = new NbtCompound();
        NbtList inventoryTag = player.inventory.writeNbt(new NbtList());
        backup.put("inventory", inventoryTag);
        backup.putInt("selectedSlot", player.inventory.selectedSlot);
        backup.putInt("xpLevel", player.experienceLevel);
        backup.putFloat("xpProgress", player.experienceProgress);
        backup.putInt("totalXp", player.totalExperience);
        data.setInventoryBackup(player.getUuid(), backup);
    }

    public static boolean restoreInventory(ServerPlayerEntity player, JEventsSavedData data, boolean clearBackup) {
        NbtCompound backup = data.getInventoryBackup(player.getUuid());
        if (backup == null || !backup.contains("inventory", 9)) {
            return false;
        }

        NbtList inventoryTag = backup.getList("inventory", 10);
        player.inventory.readNbt(inventoryTag);
        if (backup.contains("selectedSlot")) {
            player.inventory.selectedSlot = backup.getInt("selectedSlot");
        }
        if (backup.contains("xpLevel")) {
            player.experienceLevel = backup.getInt("xpLevel");
        }
        if (backup.contains("xpProgress")) {
            player.experienceProgress = backup.getFloat("xpProgress");
        }
        if (backup.contains("totalXp")) {
            player.totalExperience = backup.getInt("totalXp");
        }

        player.inventory.markDirty();
        player.currentScreenHandler.sendContentUpdates();
        player.playerScreenHandler.sendContentUpdates();

        if (clearBackup) {
            data.clearInventoryBackup(player.getUuid());
        }

        return true;
    }

    private static void applyLifesteal(ServerPlayerEntity victim, DamageSource source) {
        if (!(source.getAttacker() instanceof ServerPlayerEntity)) {
            return;
        }

        ServerPlayerEntity killer = (ServerPlayerEntity) source.getAttacker();
        if (killer.getUuid().equals(victim.getUuid())) {
            return;
        }

        adjustMaxHealth(killer, 2.0D, true);
        adjustMaxHealth(victim, -2.0D, false);

        killer.sendSystemMessage(new LiteralText("[jEvents] Lifesteal: +1 heart."), SYSTEM_SENDER);
        victim.sendSystemMessage(new LiteralText("[jEvents] Lifesteal: -1 heart."), SYSTEM_SENDER);
    }

    private static void adjustMaxHealth(ServerPlayerEntity player, double delta, boolean healDelta) {
        EntityAttributeInstance maxHealth = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }

        double oldBase = maxHealth.getBaseValue();
        double newBase = Math.max(2.0D, oldBase + delta);
        maxHealth.setBaseValue(newBase);

        if (healDelta && delta > 0.0D) {
            float healed = player.getHealth() + (float) delta;
            player.setHealth((float) Math.min(newBase, healed));
        } else if (player.getHealth() > newBase) {
            player.setHealth((float) newBase);
        }
    }
}
