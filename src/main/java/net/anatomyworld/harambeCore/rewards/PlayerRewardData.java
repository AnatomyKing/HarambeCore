/* net.anatomyworld.harambeCore.rewards.PlayerRewardData */
package net.anatomyworld.harambeCore.rewards;

import net.anatomyworld.harambeCore.config.YamlConfigLoader;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class PlayerRewardData {

    private final JavaPlugin plugin;

    public PlayerRewardData(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /* ---------------------------------------------------------- */
    /*  Public API                                                */
    /* ---------------------------------------------------------- */

    /* ---------- add ---------- */

    public void addReward(UUID id, String group, String rewardId) {
        addReward(id, group, rewardId, 1);
    }

    public void addReward(UUID id, String group, String rewardId, int amount) {
        if (amount <= 0) return;

        FileConfiguration yml = loadConfig(id);
        String path           = path(group) + "." + rewardId;
        int   old             = yml.getInt(path, 0);

        yml.set(path, old + amount);
        saveConfig(id, yml);
    }

    /* ---------- query ---------- */

    /** @return map of rewardId → count (empty if none) */
    public Map<String, Integer> getAllRewards(UUID id, String group) {
        FileConfiguration yml   = loadConfig(id);
        ConfigurationSection sec = yml.getConfigurationSection(path(group));
        if (sec == null) return new HashMap<>();

        Map<String, Integer> map = new HashMap<>();
        for (String key : sec.getKeys(false)) {
            map.put(key, sec.getInt(key, 0));
        }
        return map;
    }

    /* ---------- remove ---------- */

    public void removeReward(UUID id, String group, String rewardId) {
        removeReward(id, group, rewardId, 1);
    }

    public void removeReward(UUID id, String group, String rewardId, int amount) {
        if (amount <= 0) return;

        FileConfiguration yml = loadConfig(id);
        String path           = path(group) + "." + rewardId;
        int   old             = yml.getInt(path, 0);
        int   now             = old - amount;

        if (now <= 0) yml.set(path, null);
        else          yml.set(path, now);

        saveConfig(id, yml);
    }

    public void removeGroup(UUID id, String group) {
        FileConfiguration yml = loadConfig(id);
        yml.set("rewards." + group, null);
        saveConfig(id, yml);
    }

    /* ---------------------------------------------------------- */
    /*  Helpers                                                   */
    /* ---------------------------------------------------------- */

    private String path(String group) {
        return "rewards." + group + ".queued";
    }

    /** Loads (and auto-creates) playerdata/&lt;uuid&gt;.yml */
    private FileConfiguration loadConfig(UUID id) {
        return YamlConfigLoader.load(plugin, "playerdata/" + id + ".yml");
    }

    private void saveConfig(UUID id, FileConfiguration yml) {
        try {
            File file = new File(plugin.getDataFolder(), "playerdata/" + id + ".yml");
            yml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to save reward data for " + id + ": " + ex.getMessage());
        }
    }
}
