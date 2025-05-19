package net.anatomyworld.harambeCore.util;

import net.anatomyworld.harambeCore.config.YamlConfigLoader;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class YLevelTeleportHandler implements Listener {

    private static class Rule {
        Integer below;
        Integer above;
        String toWorld;
        double toY;
    }

    private final JavaPlugin plugin;
    private final Map<String, Rule> rules = new HashMap<>();

    public YLevelTeleportHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /** Reload the teleport rules from disk */
    public void reload() {
        rules.clear();
        loadConfig();
        plugin.getLogger().info("[YLevelTeleport] Reloaded teleport rules.");
    }

    private void loadConfig() {
        FileConfiguration cfg = YamlConfigLoader.load(plugin, "util/world-yteleport.yml");
        ConfigurationSection section = cfg.getConfigurationSection("worlds");
        if (section == null) {
            plugin.getLogger().warning("[YLevelTeleport] No 'worlds' section found in world-yteleport.yml");
            return;
        }

        for (String worldName : section.getKeys(false)) {
            ConfigurationSection ruleSec = section.getConfigurationSection(worldName);
            if (ruleSec == null) continue;

            Rule rule = new Rule();
            if (ruleSec.contains("below")) rule.below = ruleSec.getInt("below");
            if (ruleSec.contains("above")) rule.above = ruleSec.getInt("above");
            rule.toWorld = ruleSec.getString("to-world");
            rule.toY = ruleSec.getDouble("to-y", 70);

            if (rule.toWorld != null) {
                rules.put(worldName, rule);
                plugin.getLogger().info("[YLevelTeleport] Loaded rule for world: " + worldName);
            } else {
                plugin.getLogger().warning("[YLevelTeleport] Missing 'to-world' for rule: " + worldName);
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockY() == event.getTo().getBlockY()) return;

        Player player = event.getPlayer();
        World currentWorld = player.getWorld();
        String worldName = currentWorld.getName();

        Rule rule = rules.get(worldName);
        if (rule == null) return;

        double y = player.getLocation().getY();

        if (rule.below != null && y < rule.below) {
            teleportPlayer(player, rule);
        } else if (rule.above != null && y > rule.above) {
            teleportPlayer(player, rule);
        }
    }

    private void teleportPlayer(Player player, Rule rule) {
        World dest = Bukkit.getWorld(rule.toWorld);
        if (dest == null) {
            plugin.getLogger().warning("[YLevelTeleport] Destination world '" + rule.toWorld + "' not found!");
            return;
        }

        player.teleport(dest.getSpawnLocation().clone().add(0, rule.toY - dest.getSpawnLocation().getY(), 0));
        plugin.getLogger().info("[YLevelTeleport] Teleported " + player.getName() + " to " + rule.toWorld + " at Y=" + rule.toY);
    }
}
