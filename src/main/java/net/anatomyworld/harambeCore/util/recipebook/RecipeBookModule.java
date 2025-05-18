/* net.anatomyworld.harambeCore.util.recipebook.RecipeBookModule */
package net.anatomyworld.harambeCore.util.recipebook;

import net.anatomyworld.harambeCore.config.YamlConfigLoader;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public final class RecipeBookModule {

    private final JavaPlugin plugin;
    private RecipeBookPacketListener listener;

    public RecipeBookModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Loads util/recipe-book.yml and (re)starts the packet listener. */
    public void enable() {
        // load (and save default if needed)
        FileConfiguration cfg = YamlConfigLoader.load(plugin, "util/recipe-book.yml");

        Map<String,String> worldCmds = new HashMap<>();
        ConfigurationSection sec = cfg.getConfigurationSection("world-commands");
        if (sec != null) {
            for (String world : sec.getKeys(false)) {
                worldCmds.put(world, sec.getString(world));
            }
        }

        // hot-reload support
        if (listener != null) listener.shutdown();
        listener = new RecipeBookPacketListener(plugin, worldCmds);
    }

    /** Clean shutdown when plugin disables. */
    public void disable() {
        if (listener != null) listener.shutdown();
    }
}
