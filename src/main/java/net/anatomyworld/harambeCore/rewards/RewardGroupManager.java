/* net.anatomyworld.harambeCore.rewards.RewardGroupManager */
package net.anatomyworld.harambeCore.rewards;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds the <strong>in-memory</strong> mapping of group → (item → reward).
 * Does not touch disk; the module feeds it a loaded FileConfiguration.
 */
public class RewardGroupManager {

    private final Map<String, Map<String, String>> rewardGroups = new HashMap<>();
    private final FileConfiguration backingYml;

    public RewardGroupManager(FileConfiguration cfg) {
        this.backingYml = cfg;
        load();
    }

    /* ------------------------------------------------------------------ */
    /*  Public getters                                                    */
    /* ------------------------------------------------------------------ */

    public RewardEntry getEntryForItem(String itemKey) {
        for (var e : rewardGroups.entrySet()) {
            String reward = e.getValue().get(itemKey);
            if (reward != null) return new RewardEntry(e.getKey(), itemKey, reward);
        }
        return null;
    }

    public void addReward(String group, String item, String reward) {
        Map<String,String> map = rewardGroups.computeIfAbsent(group, k -> new HashMap<>());
        map.put(item, reward);

        int idx = map.size();
        String base = "reward-groups." + group + ".entries." + idx;
        backingYml.set(base + ".item",   item);
        backingYml.set(base + ".reward", reward);
    }

    /* ------------------------------------------------------------------ */
    /*  Internal load helper                                              */
    /* ------------------------------------------------------------------ */

    private void load() {
        ConfigurationSection grpSec = backingYml.getConfigurationSection("reward-groups");
        if (grpSec == null) return;

        for (String group : grpSec.getKeys(false)) {
            ConfigurationSection entries = grpSec.getConfigurationSection(group + ".entries");
            if (entries == null) continue;

            Map<String,String> map = new HashMap<>();
            for (String k : entries.getKeys(false)) {
                String item   = entries.getString(k + ".item");
                String reward = entries.getString(k + ".reward");
                if (item != null && reward != null) map.put(item, reward);
            }
            rewardGroups.put(group, map);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Record helper                                                     */
    /* ------------------------------------------------------------------ */

    public record RewardEntry(String groupName, String itemKey, String rewardId) {}
}
