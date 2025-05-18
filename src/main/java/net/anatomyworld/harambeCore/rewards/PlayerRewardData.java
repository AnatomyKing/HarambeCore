/* net.anatomyworld.harambeCore.rewards.PlayerRewardData */
package net.anatomyworld.harambeCore.rewards;

import net.anatomyworld.harambeCore.config.YamlConfigLoader;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages the <strong>rewards</strong> section inside each player's YAML file.
 * Now uses YamlConfigLoader to create /playerdata/<uuid>.yml automatically.
 */
public class PlayerRewardData {

    private final JavaPlugin plugin;

    public PlayerRewardData(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /* ---------------------------------------------------------- */
    /*  Public API                                                */
    /* ---------------------------------------------------------- */

    public void addReward(UUID id, String group, String reward) {
        FileConfiguration y = loadConfig(id);
        List<String> list  = y.getStringList(path(group));
        list.add(reward);
        y.set(path(group), list);
        saveConfig(id, y);
    }

    public List<String> getAllRewards(UUID id, String group) {
        return new ArrayList<>(loadConfig(id).getStringList(path(group)));
    }

    public void removeReward(UUID id, String group, String reward) {
        FileConfiguration y = loadConfig(id);
        List<String> list  = y.getStringList(path(group));
        list.remove(reward);
        y.set(path(group), list);
        saveConfig(id, y);
    }

    public void removeGroup(UUID id, String group) {
        FileConfiguration y = loadConfig(id);
        y.set("rewards." + group, null);
        saveConfig(id, y);
    }

    /* ---------------------------------------------------------- */
    /*  Helpers                                                   */
    /* ---------------------------------------------------------- */

    private String path(String group) {
        return "rewards." + group + ".queued";
    }

    /** Delegates to YamlConfigLoader — creates playerdata/<uuid>.yml if missing */
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
