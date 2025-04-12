package net.anatomyworld.harambeCore.harambemethods;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class YamlUtil {

    public static void savePlayerData(File file, Map<Integer, ItemStack[]> data) {
        YamlConfiguration config = new YamlConfiguration();
        data.forEach((page, items) -> config.set(String.valueOf(page), items));

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<Integer, ItemStack[]> loadPlayerData(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        Map<Integer, ItemStack[]> data = new HashMap<>();

        for (String key : config.getKeys(false)) {
            int page = Integer.parseInt(key);
            ItemStack[] items = config.getList(key).toArray(new ItemStack[54]);
            data.put(page, items);
        }
        return data;
    }
}