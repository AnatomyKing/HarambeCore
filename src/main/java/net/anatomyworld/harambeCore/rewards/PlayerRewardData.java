/* net.anatomyworld.harambeCore.rewards.PlayerRewardData */
package net.anatomyworld.harambeCore.rewards;

import net.anatomyworld.harambeCore.config.YamlConfigLoader;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;


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

    private static final String EXPIRY_NODE = "expiry_epoch";
    private static final long   TTL_MS      = 3_600_000L;   // 1 h

    /** set expiry to “now + TTL” (overwrite if already present) */
    public void setExpiry(UUID id, String group) {
        FileConfiguration yml = loadConfig(id);
        yml.set("rewards." + group + '.' + EXPIRY_NODE,
                System.currentTimeMillis() + TTL_MS);
        saveConfig(id, yml);
    }

    /** @return epoch millis, or 0 if none stored */
    public long getExpiry(UUID id, String group) {
        return loadConfig(id).getLong("rewards." + group + '.' + EXPIRY_NODE, 0L);
    }

    /** @return true if group EXISTS **and** is past its TTL */
    public boolean isExpired(UUID id, String group) {
        long exp = getExpiry(id, group);
        return exp > 0 && System.currentTimeMillis() >= exp;
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

    /** Append a clone of the ItemStack to the player’s personal queue. */
    public void addStackReward(UUID id, String group, ItemStack stack) {
        if (stack == null) return;
        FileConfiguration yml = loadConfig(id);
        String path = "rewards." + group + ".stack_queue";

        @SuppressWarnings("unchecked")
        List<ItemStack> list = (List<ItemStack>) yml.getList(path);
        if (list == null) list = new ArrayList<>();
        list.add(stack.clone());

        yml.set(path, list);
        saveConfig(id, yml);
    }

    /** Return a *copy* of the queued stacks (never null). */
    @SuppressWarnings("unchecked")
    public List<ItemStack> getStackRewards(UUID id, String group) {
        FileConfiguration yml = loadConfig(id);
        List<ItemStack> list = (List<ItemStack>) yml.getList("rewards." + group + ".stack_queue");
        return list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    /** Pop (and discard) the first queued ItemStack. */
    @SuppressWarnings("unchecked")
    public void popFirstStackReward(UUID id, String group) {
        String path = "rewards." + group + ".stack_queue";
        FileConfiguration yml = loadConfig(id);
        List<ItemStack> list = (List<ItemStack>) yml.getList(path);
        if (list == null || list.isEmpty()) return;

        list.remove(0);
        yml.set(path, list);
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
