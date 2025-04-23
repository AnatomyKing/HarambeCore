package net.anatomyworld.harambeCore.item;

import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class PlayerRewardData {

    private final File dataFile;
    private final YamlConfiguration config;

    public PlayerRewardData(File dataFolder) {
        this.dataFile = new File(dataFolder, "playerdata/rewards.yml");
        this.config = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void addReward(UUID player, String group, String rewardId) {
        List<String> list = config.getStringList(player.toString() + ".collect." + group);
        if (!list.contains(rewardId)) {
            list.add(rewardId);
            config.set(player + ".collect." + group, list);
            save();
        }
    }

    public List<String> getAllRewards(UUID player, String group) {
        return new ArrayList<>(config.getStringList(player.toString() + ".collect." + group));
    }

    public void removeReward(UUID player, String group, String rewardId) {
        List<String> list = config.getStringList(player.toString() + ".collect." + group);
        list.remove(rewardId);
        config.set(player + ".collect." + group, list);
        save();
    }

    public void removeGroup(UUID player, String group) {
        config.set(player.toString() + ".collect." + group, null);
        save();
    }

    private void save() {
        try {
            config.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
