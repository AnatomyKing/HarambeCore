package net.anatomyworld.harambeCore.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class YamlConfigLoader {
    public static FileConfiguration load(File file) {
        if (!file.exists()) return null;
        return YamlConfiguration.loadConfiguration(file);
    }
}
