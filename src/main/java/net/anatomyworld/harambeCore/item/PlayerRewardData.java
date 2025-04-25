package net.anatomyworld.harambeCore.item;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages the <strong>rewards</strong> section inside each player's YAML file.
 * Every call hits disk – fast enough for small files (<10 kB).  Optimise later if needed.
 */
public class PlayerRewardData {

    private final File playerDataFolder;

    public PlayerRewardData(File pluginDataFolder) {
        this.playerDataFolder = new File(pluginDataFolder, "playerdata");
        if (!playerDataFolder.exists()) playerDataFolder.mkdirs();
    }

    /* --------------------------------------------------------------------- */
    /*  Public API (unchanged)                                               */
    /* --------------------------------------------------------------------- */

    public void addReward(UUID player, String group, String rewardId) {
        YamlConfiguration yml = load(player);
        List<String> list = yml.getStringList(path(group));
        list.add(rewardId);                       // duplicates allowed
        yml.set(path(group), list);
        save(player, yml);
    }

    public List<String> getAllRewards(UUID player, String group) {
        return new ArrayList<>(load(player).getStringList(path(group)));
    }

    public void removeReward(UUID player, String group, String rewardId) {
        YamlConfiguration yml = load(player);
        List<String> list = yml.getStringList(path(group));
        list.remove(rewardId);                    // removes one instance
        yml.set(path(group), list);
        save(player, yml);
    }

    public void removeGroup(UUID player, String group) {
        YamlConfiguration yml = load(player);
        yml.set("rewards." + group, null);
        save(player, yml);
    }

    /* --------------------------------------------------------------------- */
    /*  Helpers                                                              */
    /* --------------------------------------------------------------------- */

    private String path(String group) {
        return "rewards." + group + ".queued";
    }

    private YamlConfiguration load(UUID uuid) {
        return YamlConfiguration.loadConfiguration(new File(playerDataFolder, uuid + ".yml"));
    }

    private void save(UUID uuid, YamlConfiguration yml) {
        try { yml.save(new File(playerDataFolder, uuid + ".yml")); }
        catch (IOException ex) { ex.printStackTrace(); }
    }
}
