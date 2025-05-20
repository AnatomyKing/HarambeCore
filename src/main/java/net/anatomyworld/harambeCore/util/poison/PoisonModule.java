package net.anatomyworld.harambeCore.util.poison;

import net.anatomyworld.harambeCore.config.YamlConfigLoader;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PoisonModule {

    private final JavaPlugin plugin;

    public PoisonModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Loads util/poison-block.yml and registers the listener */
    public void enable() {
        FileConfiguration cfg = YamlConfigLoader.load(plugin, "util/poison-block.yml");

        String worldName  = cfg.getString("poison-world", "world");
        String blockDataS = cfg.getString("poison-block");

        plugin.getLogger().info("[Poison] world  = " + worldName);
        plugin.getLogger().info("[Poison] block  = " + blockDataS);

        if (blockDataS == null || blockDataS.isEmpty()) {
            plugin.getLogger().warning("[Poison] poison-block missing or empty in poison-block.yml");
            return;
        }

        try {
            BlockData bd = Bukkit.createBlockData(blockDataS);
            Bukkit.getPluginManager().registerEvents(
                    new PoisonEffect(plugin, worldName, bd),
                    plugin
            );
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("[Poison] invalid block data string: " + blockDataS);
        }
    }
}
