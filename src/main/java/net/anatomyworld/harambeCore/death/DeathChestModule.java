package net.anatomyworld.harambeCore.death;

import net.anatomyworld.harambeCore.config.YamlConfigLoader;
import net.anatomyworld.harambeCore.rewards.PlayerRewardData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.UUID;

/** Loads / saves <code>util/death-chest.yml</code> and exposes its settings. */
public final class DeathChestModule {

    private final JavaPlugin plugin;
    private FileConfiguration yml;

    public DeathChestModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /* ------------------- life-cycle ------------------- */

    public void enable() {
        yml = YamlConfigLoader.load(plugin, "util/death-chest.yml");

        // Forward TTL to PlayerRewardData
        long ttlMinutes = yml.getLong("expiry_minutes", 60);
        PlayerRewardData.setTtlMs(ttlMinutes * 60_000L);

        // Clean expired queues at startup
        PlayerRewardData data = new PlayerRewardData(plugin);
        purgeExpiredQueues(data);

        save(); // create file on first run if missing
    }

    public void disable() {
        save();
    }

    private void save() {
        try {
            if (yml != null)
                yml.save(plugin.getDataFolder()
                        .toPath()
                        .resolve("util/death-chest.yml")
                        .toFile());
        } catch (IOException ex) {
            plugin.getLogger().warning("[DeathChest] Could not save config: " + ex.getMessage());
        }
    }

    /* ─── NEW: cleanup method ─── */

    public void purgeExpiredQueues(PlayerRewardData data) {
        File playerDataDir = plugin.getDataFolder().toPath().resolve("playerdata").toFile();
        if (!playerDataDir.exists() || !playerDataDir.isDirectory()) return;

        File[] files = playerDataDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        int removed = 0;

        for (File file : files) {
            String filename = file.getName();
            String uuidStr = filename.substring(0, filename.length() - 4); // strip ".yml"

            UUID playerId;
            try {
                playerId = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("[DeathChest] Invalid playerdata file: " + filename);
                continue;
            }

            var yml = YamlConfigLoader.load(plugin, "playerdata/" + uuidStr + ".yml");
            var rewardsSec = yml.getConfigurationSection("rewards");
            if (rewardsSec == null) continue;

            for (String group : new HashSet<>(rewardsSec.getKeys(false))) {
                if (data.isExpired(playerId, group)) {
                    data.removeGroup(playerId, group);
                    removed++;
                }
            }
        }

        plugin.getLogger().info("[DeathChest] Purged " + removed + " expired reward group(s).");
    }

    /* ------------------- accessors -------------------- */

    public String furnitureItemId() {
        return yml.getString("furniture_item", "death_chest_harambenium");
    }

    public long expiryMinutes() {
        return yml.getLong("expiry_minutes", 60);
    }

    /** root broadcast section (<code>broadcast:</code>) */
    public ConfigurationSection broadcast() {
        return yml.getConfigurationSection("broadcast");
    }

    /** key cosmetics section (<code>key:</code>) */
    public ConfigurationSection key() {
        return yml.getConfigurationSection("key");
    }
}
