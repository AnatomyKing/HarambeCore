/* net.anatomyworld.harambeCore.config.YamlConfigLoader */
package net.anatomyworld.harambeCore.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

public final class YamlConfigLoader {
    private YamlConfigLoader() {}

    /**
     * Ensures the file plugin.getDataFolder()/relativePath exists (copying
     * defaults from the jar if needed), then loads & returns it.
     */
    public static FileConfiguration load(JavaPlugin plugin, String relativePath) {
        File file = new File(plugin.getDataFolder(), relativePath);

        // create parent folders & copy default resource if missing
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try (InputStream in = plugin.getResource(relativePath)) {
                if (in != null) Files.copy(in, file.toPath());
            } catch (Exception ex) {
                plugin.getLogger().warning("Unable to save default " + relativePath + ": " + ex.getMessage());
            }
        }

        return YamlConfiguration.loadConfiguration(file);
    }
}
