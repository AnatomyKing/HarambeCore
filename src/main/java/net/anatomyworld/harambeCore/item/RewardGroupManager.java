package net.anatomyworld.harambeCore.item;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RewardGroupManager {

    private final File configFile;
    private final YamlConfiguration config;
    private final Map<String, Map<String, String>> rewardGroups = new HashMap<>();

    public RewardGroupManager(Plugin plugin) {
        this.configFile = new File(plugin.getDataFolder(), "config/reward-groups.yml");
        this.config = YamlConfiguration.loadConfiguration(configFile);
        load();
    }

    private void load() {
        ConfigurationSection groupsSection = config.getConfigurationSection("reward-groups");
        if (groupsSection == null) return;

        for (String group : groupsSection.getKeys(false)) {
            ConfigurationSection entries = groupsSection.getConfigurationSection(group + ".entries");
            if (entries == null) continue;

            Map<String, String> map = new HashMap<>();
            for (String key : entries.getKeys(false)) {
                String item = entries.getString(key + ".item");
                String reward = entries.getString(key + ".reward");
                if (item != null && reward != null) {
                    map.put(item, reward);
                }
            }
            rewardGroups.put(group, map);
        }
    }

    public void addReward(String group, String item, String reward) {
        Map<String, String> groupMap = rewardGroups.computeIfAbsent(group, k -> new HashMap<>());
        groupMap.put(item, reward);

        int index = groupMap.size();
        config.set("reward-groups." + group + ".entries." + index + ".item", item);
        config.set("reward-groups." + group + ".entries." + index + ".reward", reward);
        save();
    }

    public RewardEntry getEntryForItem(String item) {
        for (var entry : rewardGroups.entrySet()) {
            String group = entry.getKey();
            String reward = entry.getValue().get(item);
            if (reward != null) return new RewardEntry(group, item, reward);
        }
        return null;
    }

    private void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public record RewardEntry(String groupName, String item, String reward) {}
}
