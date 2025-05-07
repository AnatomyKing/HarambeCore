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

/**
 * Handles the <strong>gui</strong> section of every <code>playerdata/&lt;uuid&gt;.yml</code>.
 * Rewards are handled by {@link net.anatomyworld.harambeCore.item.PlayerRewardData},
 * but live in the same file – so we must preserve that section on every save.
 */
public class StorageManager implements Listener {

    private final JavaPlugin plugin;
    private final File       dataFolder;
    private final Map<UUID, Map<String, Map<Integer, ItemStack>>> playerCache = new HashMap<>();

    public StorageManager(JavaPlugin plugin) {
        this.plugin    = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) dataFolder.mkdirs();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /* --------------------------------------------------------------------- */
    /*  Player lifecycle                                                     */
    /* --------------------------------------------------------------------- */

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        loadPlayerStorage(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        savePlayerStorage(id);
        unload(id);
    }

    /* --------------------------------------------------------------------- */
    /*  Public API                                                           */
    /* --------------------------------------------------------------------- */

    public void onShutdown() {
        // Save synchronously on the main thread to avoid IllegalPluginAccessException
        for (UUID uuid : playerCache.keySet()) {
            savePlayerStorageSync(uuid);
        }
    }


    public Map<Integer, ItemStack> getOrCreateStorage(UUID uuid, String guiKey) {
        return playerCache
                .computeIfAbsent(uuid, k -> new HashMap<>())
                .computeIfAbsent(guiKey, k -> new HashMap<>());
    }

    public void setItem(UUID uuid, String guiKey, int slot, ItemStack item) {
        getOrCreateStorage(uuid, guiKey).put(slot, item);
    }

    /* --------------------------------------------------------------------- */
    /*  Internal save / load                                                 */
    /* --------------------------------------------------------------------- */

    private void loadPlayerStorage(UUID uuid) {
        File file = new File(dataFolder, uuid + ".yml");
        if (!file.exists()) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            Map<String, Map<Integer, ItemStack>> guiMap = new HashMap<>();

            ConfigurationSection guiSec = yaml.getConfigurationSection("gui");
            if (guiSec != null) {
                for (String guiKey : guiSec.getKeys(false)) {
                    ConfigurationSection section = guiSec.getConfigurationSection(guiKey);
                    if (section == null) continue;

                    Map<Integer, ItemStack> slotMap = new HashMap<>();
                    for (String s : section.getKeys(false)) {
                        try {
                            int slot = Integer.parseInt(s);
                            ItemStack stack = section.getItemStack(s);
                            if (stack != null) slotMap.put(slot, stack);
                        } catch (NumberFormatException ignored) { }
                    }
                    guiMap.put(guiKey, slotMap);
                }
            }

            Bukkit.getScheduler().runTask(plugin, () -> playerCache.put(uuid, guiMap));
        });
    }

    public void savePlayerStorage(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File file = new File(dataFolder, uuid + ".yml");
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

            /* wipe only the gui section – leave rewards intact */
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


    private void savePlayerStorageSync(UUID uuid) {
        File file = new File(dataFolder, uuid + ".yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

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
    }

    private void saveAll() {
        for (UUID uuid : playerCache.keySet()) savePlayerStorage(uuid);
    }

    private void unload(UUID uuid) {
        playerCache.remove(uuid);
    }
}
