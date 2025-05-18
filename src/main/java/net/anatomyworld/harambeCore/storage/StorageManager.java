package net.anatomyworld.harambeCore.storage;

import net.anatomyworld.harambeCore.config.YamlConfigLoader;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Handles the <strong>gui</strong> section of each player's
 * playerdata/<uuid>.yml file.  Uses YamlConfigLoader to ensure
 * the file and parent folder exist.
 */
public class StorageManager implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, Map<String, Map<Integer, ItemStack>>> playerCache = new HashMap<>();

    public StorageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        // No need to mkdir playerdata/ – loader will create parent folder
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /* ------------------------------------------------------------------ */
    /*  Player lifecycle                                                  */
    /* ------------------------------------------------------------------ */

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        // Load asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            FileConfiguration yaml = YamlConfigLoader.load(plugin, "playerdata/" + uuid + ".yml");
            Map<String, Map<Integer, ItemStack>> guiMap = new HashMap<>();

            ConfigurationSection guiSec = yaml.getConfigurationSection("gui");
            if (guiSec != null) {
                for (String guiKey : guiSec.getKeys(false)) {
                    ConfigurationSection section = guiSec.getConfigurationSection(guiKey);
                    if (section == null) continue;

                    Map<Integer, ItemStack> slotMap = new HashMap<>();
                    for (String slotStr : section.getKeys(false)) {
                        try {
                            int slot = Integer.parseInt(slotStr);
                            ItemStack stack = section.getItemStack(slotStr);
                            if (stack != null) slotMap.put(slot, stack);
                        } catch (NumberFormatException ignored) {}
                    }
                    guiMap.put(guiKey, slotMap);
                }
            }

            // Put into cache on main thread
            Bukkit.getScheduler().runTask(plugin, () -> playerCache.put(uuid, guiMap));
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        savePlayerStorage(uuid);
        unload(uuid);
    }

    /** Save all online players’ GUI cache on plugin disable. */
    public void onShutdown() {
        for (UUID uuid : new ArrayList<>(playerCache.keySet())) {
            savePlayerStorageSync(uuid);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Public API                                                        */
    /* ------------------------------------------------------------------ */

    /** Get or create a per-player, per-GUI storage map. */
    public Map<Integer, ItemStack> getOrCreateStorage(UUID uuid, String guiKey) {
        return playerCache
                .computeIfAbsent(uuid, k -> new HashMap<>())
                .computeIfAbsent(guiKey, k -> new HashMap<>());
    }

    /** Explicitly set one slot’s item in the cache. */
    public void setItem(UUID uuid, String guiKey, int slot, ItemStack item) {
        getOrCreateStorage(uuid, guiKey).put(slot, item);
    }

    /* ------------------------------------------------------------------ */
    /*  Internal save / load                                              */
    /* ------------------------------------------------------------------ */

    /** Async save on player quit or intermediate. */
    public void savePlayerStorage(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File file = new File(plugin.getDataFolder(), "playerdata/" + uuid + ".yml");
            FileConfiguration yaml = YamlConfigLoader.load(plugin, "playerdata/" + uuid + ".yml");

            // Wipe only the gui section, preserve other data (e.g. rewards)
            yaml.set("gui", null);
            Map<String, Map<Integer, ItemStack>> guiMap = playerCache.get(uuid);
            if (guiMap != null) {
                for (var guiEntry : guiMap.entrySet()) {
                    String guiKey = guiEntry.getKey();
                    for (var slotEntry : guiEntry.getValue().entrySet()) {
                        yaml.set("gui." + guiKey + "." + slotEntry.getKey(), slotEntry.getValue());
                    }
                }
            }

            try {
                yaml.save(file);
            } catch (IOException ex) {
                plugin.getLogger().severe("Failed to save GUI data for " + uuid + ": " + ex.getMessage());
            }
        });
    }

    /** Sync save on shutdown to avoid threading issues. */
    private void savePlayerStorageSync(UUID uuid) {
        File file = new File(plugin.getDataFolder(), "playerdata/" + uuid + ".yml");
        FileConfiguration yaml = YamlConfigLoader.load(plugin, "playerdata/" + uuid + ".yml");

        yaml.set("gui", null);
        Map<String, Map<Integer, ItemStack>> guiMap = playerCache.get(uuid);
        if (guiMap != null) {
            for (var guiEntry : guiMap.entrySet()) {
                String guiKey = guiEntry.getKey();
                for (var slotEntry : guiEntry.getValue().entrySet()) {
                    yaml.set("gui." + guiKey + "." + slotEntry.getKey(), slotEntry.getValue());
                }
            }
        }

        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to sync‐save GUI data for " + uuid + ": " + ex.getMessage());
        }
    }

    /** Remove from memory after saving. */
    private void unload(UUID uuid) {
        playerCache.remove(uuid);
    }
}
