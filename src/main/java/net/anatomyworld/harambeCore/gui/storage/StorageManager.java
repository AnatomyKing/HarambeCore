package net.anatomyworld.harambeCore.gui.storage;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class StorageManager implements Listener {

    private final JavaPlugin plugin;
    private final File dataFolder;
    private final Map<UUID, Map<String, Map<Integer, ItemStack>>> playerCache = new HashMap<>();

    public StorageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) dataFolder.mkdirs();

        // Register internal listener
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /* ------------------- PLAYER LISTENERS ------------------- */

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        loadPlayerStorage(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        savePlayerStorage(uuid);
        unload(uuid);
    }

    /* ------------------- PUBLIC API ------------------- */

    public void onShutdown() {
        saveAll();
    }

    public Map<Integer, ItemStack> getOrCreateStorage(UUID uuid, String guiKey) {
        return playerCache
                .computeIfAbsent(uuid, k -> new HashMap<>())
                .computeIfAbsent(guiKey, k -> new HashMap<>());
    }

    @SuppressWarnings("unused")
    public void setItem(UUID uuid, String guiKey, int slot, ItemStack item) {
        getOrCreateStorage(uuid, guiKey).put(slot, item);
    }

    /* ------------------- INTERNAL SAVE/LOAD ------------------- */

    private void loadPlayerStorage(UUID uuid) {
        File file = new File(dataFolder, uuid + ".yml");
        if (!file.exists()) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            Map<String, Map<Integer, ItemStack>> guiMap = new HashMap<>();

            for (String guiKey : config.getKeys(false)) {
                ConfigurationSection section = config.getConfigurationSection(guiKey);
                if (section == null) continue;

                Map<Integer, ItemStack> slotMap = new HashMap<>();
                for (String key : section.getKeys(false)) {
                    try {
                        int slot = Integer.parseInt(key);
                        ItemStack item = section.getItemStack(key);
                        if (item != null) slotMap.put(slot, item);
                    } catch (NumberFormatException ignored) {}
                }
                guiMap.put(guiKey, slotMap);
            }

            Bukkit.getScheduler().runTask(plugin, () -> playerCache.put(uuid, guiMap));
        });
    }

    public void savePlayerStorage(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File file = new File(dataFolder, uuid + ".yml");
            YamlConfiguration config = new YamlConfiguration();

            Map<String, Map<Integer, ItemStack>> guiMap = playerCache.get(uuid);
            if (guiMap == null) return;

            for (Map.Entry<String, Map<Integer, ItemStack>> guiEntry : guiMap.entrySet()) {
                String guiKey = guiEntry.getKey();
                Map<Integer, ItemStack> slotMap = guiEntry.getValue();
                for (Map.Entry<Integer, ItemStack> slotEntry : slotMap.entrySet()) {
                    config.set(guiKey + "." + slotEntry.getKey(), slotEntry.getValue());
                }
            }

            try {
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save storage file for " + uuid + ": " + e.getMessage());
            }
        });
    }

    private void saveAll() {
        for (UUID uuid : playerCache.keySet()) {
            savePlayerStorage(uuid);
        }
    }

    private void unload(UUID uuid) {
        playerCache.remove(uuid);
    }
}
