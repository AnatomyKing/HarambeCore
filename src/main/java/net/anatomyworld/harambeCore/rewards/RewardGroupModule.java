/* net.anatomyworld.harambeCore.util.reward.RewardGroupModule */
package net.anatomyworld.harambeCore.rewards;

import net.anatomyworld.harambeCore.config.YamlConfigLoader;
import net.anatomyworld.harambeCore.rewards.RewardGroupManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads / saves util/reward-groups.yml and exposes a
 * ready-to-use {@link RewardGroupManager}.
 */
public final class RewardGroupModule {

    private final JavaPlugin plugin;
    private RewardGroupManager manager;
    private FileConfiguration yml;

    public RewardGroupModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Loads YAML, (re)creates manager */
    public void enable() {
        yml = YamlConfigLoader.load(plugin, "util/reward-groups.yml");
        manager = new RewardGroupManager(yml);
        save();                 // create file on first run
    }

    public void disable() { save(); }

    public RewardGroupManager getManager() { return manager; }

    private void save() {
        try {
            if (yml != null) yml.save(
                    plugin.getDataFolder().toPath().resolve("util/reward-groups.yml").toFile());
        } catch (IOException ex) {
            plugin.getLogger().warning("[Rewards] Failed to save reward-groups.yml: " + ex.getMessage());
        }
    }
}
