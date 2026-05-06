package org.johnfries.jevents.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import org.johnfries.jevents.JEventsMod;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class JEventsSavedData extends PersistentState {
    private static final String DATA_NAME = JEventsMod.MOD_ID + "_saved_data";

    private boolean hardcoreEnabled;
    private boolean hardcoreSpectatorEnabled = true;
    private boolean lifestealEnabled;
    private boolean lastBreathEnabled;
    private boolean chatMuted;

    private Identifier sgSpawnDimension;
    private int sgSpawnX;
    private int sgSpawnY;
    private int sgSpawnZ;
    private int sgSpawnRadius = 6;
    private boolean hasSgSpawn;

    private double globalMobSpawnMultiplier = 1.0D;
    private boolean dynamicMobSpawnScaling;

    private final Set<UUID> eventBannedPlayers = new HashSet<>();
    private final Set<UUID> chatWhitelistPlayers = new HashSet<>();
    private final Set<UUID> lastBreathProtectedPlayers = new HashSet<>();
    private final Set<UUID> modModePlayers = new HashSet<>();
    private final Map<UUID, Float> modModeFlightSpeed = new HashMap<>();
    private final Map<UUID, Double> playerMobSpawnMultiplier = new HashMap<>();
    private final Map<UUID, NbtCompound> inventoryBackups = new HashMap<>();

    public JEventsSavedData() {
        super(DATA_NAME);
    }

    public static JEventsSavedData get(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld is unavailable while loading jEvents data.");
        }
        return overworld.getPersistentStateManager().getOrCreate(JEventsSavedData::new, DATA_NAME);
    }

    @Override
    public void fromTag(NbtCompound nbt) {
        this.hardcoreEnabled = nbt.getBoolean("hardcoreEnabled");
        this.hardcoreSpectatorEnabled = !nbt.contains("hardcoreSpectatorEnabled") || nbt.getBoolean("hardcoreSpectatorEnabled");
        this.lifestealEnabled = nbt.getBoolean("lifestealEnabled");
        this.lastBreathEnabled = nbt.getBoolean("lastBreathEnabled");
        this.chatMuted = nbt.getBoolean("chatMuted");

        this.hasSgSpawn = nbt.getBoolean("hasSgSpawn");
        if (this.hasSgSpawn) {
            this.sgSpawnX = nbt.getInt("sgSpawnX");
            this.sgSpawnY = nbt.getInt("sgSpawnY");
            this.sgSpawnZ = nbt.getInt("sgSpawnZ");
            this.sgSpawnRadius = Math.max(1, nbt.getInt("sgSpawnRadius"));
            if (nbt.contains("sgSpawnDimension")) {
                this.sgSpawnDimension = new Identifier(nbt.getString("sgSpawnDimension"));
            } else {
                this.sgSpawnDimension = World.OVERWORLD.getValue();
            }
        } else {
            this.sgSpawnDimension = null;
        }

        this.globalMobSpawnMultiplier = nbt.contains("globalMobSpawnMultiplier") ? nbt.getDouble("globalMobSpawnMultiplier") : 1.0D;
        this.dynamicMobSpawnScaling = nbt.getBoolean("dynamicMobSpawnScaling");

        this.eventBannedPlayers.clear();
        this.chatWhitelistPlayers.clear();
        this.lastBreathProtectedPlayers.clear();
        this.modModePlayers.clear();
        this.modModeFlightSpeed.clear();
        this.playerMobSpawnMultiplier.clear();
        this.inventoryBackups.clear();

        readUuidSet(nbt, "eventBannedPlayers", this.eventBannedPlayers);
        readUuidSet(nbt, "chatWhitelistPlayers", this.chatWhitelistPlayers);
        readUuidSet(nbt, "lastBreathProtectedPlayers", this.lastBreathProtectedPlayers);
        readUuidSet(nbt, "modModePlayers", this.modModePlayers);
        readUuidFloatMap(nbt, "modModeFlightSpeed", this.modModeFlightSpeed);
        readUuidDoubleMap(nbt, "playerMobSpawnMultiplier", this.playerMobSpawnMultiplier);
        readUuidCompoundMap(nbt, "inventoryBackups", this.inventoryBackups);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putBoolean("hardcoreEnabled", this.hardcoreEnabled);
        nbt.putBoolean("hardcoreSpectatorEnabled", this.hardcoreSpectatorEnabled);
        nbt.putBoolean("lifestealEnabled", this.lifestealEnabled);
        nbt.putBoolean("lastBreathEnabled", this.lastBreathEnabled);
        nbt.putBoolean("chatMuted", this.chatMuted);

        nbt.putBoolean("hasSgSpawn", this.hasSgSpawn);
        if (this.hasSgSpawn && this.sgSpawnDimension != null) {
            nbt.putInt("sgSpawnX", this.sgSpawnX);
            nbt.putInt("sgSpawnY", this.sgSpawnY);
            nbt.putInt("sgSpawnZ", this.sgSpawnZ);
            nbt.putInt("sgSpawnRadius", this.sgSpawnRadius);
            nbt.putString("sgSpawnDimension", this.sgSpawnDimension.toString());
        }

        nbt.putDouble("globalMobSpawnMultiplier", this.globalMobSpawnMultiplier);
        nbt.putBoolean("dynamicMobSpawnScaling", this.dynamicMobSpawnScaling);

        writeUuidSet(nbt, "eventBannedPlayers", this.eventBannedPlayers);
        writeUuidSet(nbt, "chatWhitelistPlayers", this.chatWhitelistPlayers);
        writeUuidSet(nbt, "lastBreathProtectedPlayers", this.lastBreathProtectedPlayers);
        writeUuidSet(nbt, "modModePlayers", this.modModePlayers);
        writeUuidFloatMap(nbt, "modModeFlightSpeed", this.modModeFlightSpeed);
        writeUuidDoubleMap(nbt, "playerMobSpawnMultiplier", this.playerMobSpawnMultiplier);
        writeUuidCompoundMap(nbt, "inventoryBackups", this.inventoryBackups);

        return nbt;
    }

    private static void readUuidSet(NbtCompound nbt, String key, Set<UUID> output) {
        NbtList list = nbt.getList(key, 8);
        for (int i = 0; i < list.size(); i++) {
            try {
                output.add(UUID.fromString(list.getString(i)));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private static void writeUuidSet(NbtCompound nbt, String key, Set<UUID> values) {
        NbtList list = new NbtList();
        for (UUID value : values) {
            list.add(NbtString.of(value.toString()));
        }
        nbt.put(key, list);
    }

    private static void readUuidFloatMap(NbtCompound nbt, String key, Map<UUID, Float> output) {
        NbtList list = nbt.getList(key, 10);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            if (!entry.contains("uuid") || !entry.contains("value")) {
                continue;
            }
            try {
                output.put(UUID.fromString(entry.getString("uuid")), entry.getFloat("value"));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private static void writeUuidFloatMap(NbtCompound nbt, String key, Map<UUID, Float> values) {
        NbtList list = new NbtList();
        for (Map.Entry<UUID, Float> entry : values.entrySet()) {
            NbtCompound mapEntry = new NbtCompound();
            mapEntry.putString("uuid", entry.getKey().toString());
            mapEntry.putFloat("value", entry.getValue());
            list.add(mapEntry);
        }
        nbt.put(key, list);
    }

    private static void readUuidDoubleMap(NbtCompound nbt, String key, Map<UUID, Double> output) {
        NbtList list = nbt.getList(key, 10);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            if (!entry.contains("uuid") || !entry.contains("value")) {
                continue;
            }
            try {
                output.put(UUID.fromString(entry.getString("uuid")), entry.getDouble("value"));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private static void writeUuidDoubleMap(NbtCompound nbt, String key, Map<UUID, Double> values) {
        NbtList list = new NbtList();
        for (Map.Entry<UUID, Double> entry : values.entrySet()) {
            NbtCompound mapEntry = new NbtCompound();
            mapEntry.putString("uuid", entry.getKey().toString());
            mapEntry.putDouble("value", entry.getValue());
            list.add(mapEntry);
        }
        nbt.put(key, list);
    }

    private static void readUuidCompoundMap(NbtCompound nbt, String key, Map<UUID, NbtCompound> output) {
        NbtList list = nbt.getList(key, 10);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            if (!entry.contains("uuid") || !entry.contains("value", 10)) {
                continue;
            }
            try {
                output.put(UUID.fromString(entry.getString("uuid")), entry.getCompound("value"));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private static void writeUuidCompoundMap(NbtCompound nbt, String key, Map<UUID, NbtCompound> values) {
        NbtList list = new NbtList();
        for (Map.Entry<UUID, NbtCompound> entry : values.entrySet()) {
            NbtCompound mapEntry = new NbtCompound();
            mapEntry.putString("uuid", entry.getKey().toString());
            mapEntry.put("value", entry.getValue().copy());
            list.add(mapEntry);
        }
        nbt.put(key, list);
    }

    public boolean isHardcoreEnabled() {
        return hardcoreEnabled;
    }

    public void setHardcoreEnabled(boolean hardcoreEnabled) {
        this.hardcoreEnabled = hardcoreEnabled;
        markDirty();
    }

    public boolean isHardcoreSpectatorEnabled() {
        return hardcoreSpectatorEnabled;
    }

    public void setHardcoreSpectatorEnabled(boolean hardcoreSpectatorEnabled) {
        this.hardcoreSpectatorEnabled = hardcoreSpectatorEnabled;
        markDirty();
    }

    public boolean isLifestealEnabled() {
        return lifestealEnabled;
    }

    public void setLifestealEnabled(boolean lifestealEnabled) {
        this.lifestealEnabled = lifestealEnabled;
        markDirty();
    }

    public boolean isLastBreathEnabled() {
        return lastBreathEnabled;
    }

    public void setLastBreathEnabled(boolean lastBreathEnabled) {
        this.lastBreathEnabled = lastBreathEnabled;
        markDirty();
    }

    public boolean isChatMuted() {
        return chatMuted;
    }

    public void setChatMuted(boolean chatMuted) {
        this.chatMuted = chatMuted;
        markDirty();
    }

    public boolean hasSgSpawn() {
        return hasSgSpawn && sgSpawnDimension != null;
    }

    public Identifier getSgSpawnDimension() {
        return sgSpawnDimension;
    }

    public int getSgSpawnX() {
        return sgSpawnX;
    }

    public int getSgSpawnY() {
        return sgSpawnY;
    }

    public int getSgSpawnZ() {
        return sgSpawnZ;
    }

    public int getSgSpawnRadius() {
        return Math.max(1, sgSpawnRadius);
    }

    public void setSgSpawn(Identifier dimension, int x, int y, int z, int radius) {
        this.sgSpawnDimension = dimension;
        this.sgSpawnX = x;
        this.sgSpawnY = y;
        this.sgSpawnZ = z;
        this.sgSpawnRadius = Math.max(1, radius);
        this.hasSgSpawn = true;
        markDirty();
    }

    public void clearSgSpawn() {
        this.hasSgSpawn = false;
        this.sgSpawnDimension = null;
        markDirty();
    }

    public boolean isEventBanned(UUID playerId) {
        return eventBannedPlayers.contains(playerId);
    }

    public Set<UUID> getEventBannedPlayers() {
        return Collections.unmodifiableSet(eventBannedPlayers);
    }

    public boolean addEventBannedPlayer(UUID playerId) {
        boolean changed = eventBannedPlayers.add(playerId);
        if (changed) {
            markDirty();
        }
        return changed;
    }

    public boolean removeEventBannedPlayer(UUID playerId) {
        boolean changed = eventBannedPlayers.remove(playerId);
        if (changed) {
            markDirty();
        }
        return changed;
    }

    public Set<UUID> getChatWhitelistPlayers() {
        return Collections.unmodifiableSet(chatWhitelistPlayers);
    }

    public boolean isChatWhitelisted(UUID playerId) {
        return chatWhitelistPlayers.contains(playerId);
    }

    public boolean addChatWhitelistPlayer(UUID playerId) {
        boolean changed = chatWhitelistPlayers.add(playerId);
        if (changed) {
            markDirty();
        }
        return changed;
    }

    public boolean removeChatWhitelistPlayer(UUID playerId) {
        boolean changed = chatWhitelistPlayers.remove(playerId);
        if (changed) {
            markDirty();
        }
        return changed;
    }

    public Set<UUID> getLastBreathProtectedPlayers() {
        return Collections.unmodifiableSet(lastBreathProtectedPlayers);
    }

    public boolean isLastBreathProtected(UUID playerId) {
        return lastBreathProtectedPlayers.contains(playerId);
    }

    public boolean addLastBreathProtectedPlayer(UUID playerId) {
        boolean changed = lastBreathProtectedPlayers.add(playerId);
        if (changed) {
            markDirty();
        }
        return changed;
    }

    public boolean removeLastBreathProtectedPlayer(UUID playerId) {
        boolean changed = lastBreathProtectedPlayers.remove(playerId);
        if (changed) {
            markDirty();
        }
        return changed;
    }

    public boolean isModModeEnabled(UUID playerId) {
        return modModePlayers.contains(playerId);
    }

    public Set<UUID> getModModePlayers() {
        return Collections.unmodifiableSet(modModePlayers);
    }

    public boolean setModMode(UUID playerId, boolean enabled) {
        boolean changed = enabled ? modModePlayers.add(playerId) : modModePlayers.remove(playerId);
        if (!enabled) {
            modModeFlightSpeed.remove(playerId);
        }
        if (changed) {
            markDirty();
        }
        return changed;
    }

    public float getModFlightSpeed(UUID playerId) {
        return modModeFlightSpeed.getOrDefault(playerId, 0.08F);
    }

    public void setModFlightSpeed(UUID playerId, float speed) {
        modModeFlightSpeed.put(playerId, speed);
        markDirty();
    }

    public double getGlobalMobSpawnMultiplier() {
        return globalMobSpawnMultiplier;
    }

    public void setGlobalMobSpawnMultiplier(double globalMobSpawnMultiplier) {
        this.globalMobSpawnMultiplier = globalMobSpawnMultiplier;
        markDirty();
    }

    public boolean isDynamicMobSpawnScaling() {
        return dynamicMobSpawnScaling;
    }

    public void setDynamicMobSpawnScaling(boolean dynamicMobSpawnScaling) {
        this.dynamicMobSpawnScaling = dynamicMobSpawnScaling;
        markDirty();
    }

    public Map<UUID, Double> getPlayerMobSpawnMultiplier() {
        return Collections.unmodifiableMap(playerMobSpawnMultiplier);
    }

    public double getPlayerMobSpawnMultiplier(UUID playerId) {
        return playerMobSpawnMultiplier.getOrDefault(playerId, 1.0D);
    }

    public void setPlayerMobSpawnMultiplier(UUID playerId, double multiplier) {
        playerMobSpawnMultiplier.put(playerId, multiplier);
        markDirty();
    }

    public void clearPlayerMobSpawnMultiplier(UUID playerId) {
        if (playerMobSpawnMultiplier.remove(playerId) != null) {
            markDirty();
        }
    }

    public NbtCompound getInventoryBackup(UUID playerId) {
        NbtCompound backup = inventoryBackups.get(playerId);
        return backup == null ? null : backup.copy();
    }

    public void setInventoryBackup(UUID playerId, NbtCompound backup) {
        inventoryBackups.put(playerId, backup.copy());
        markDirty();
    }

    public void clearInventoryBackup(UUID playerId) {
        if (inventoryBackups.remove(playerId) != null) {
            markDirty();
        }
    }
}
