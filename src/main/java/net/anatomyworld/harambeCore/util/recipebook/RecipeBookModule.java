package net.anatomyworld.harambeCore.util.recipebook;

import net.anatomyworld.harambeCore.GuiBuilder;
import net.anatomyworld.harambeCore.config.YamlConfigLoader;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class RecipeBookModule {

    private final JavaPlugin plugin;
    private final GuiBuilder guiBuilder;
    private RecipeBookPacketListener listener;

    public RecipeBookModule(JavaPlugin plugin, GuiBuilder guiBuilder) {
        this.plugin = plugin;
        this.guiBuilder = guiBuilder;
    }

    public void enable() {
        FileConfiguration cfg = YamlConfigLoader.load(plugin, "util/recipe-book.yml");

        Map<String, String> worldCmds = new HashMap<>();
        Set<String> allowedRootCommands = new HashSet<>();

        ConfigurationSection sec = cfg.getConfigurationSection("recipe-book-commands");
        if (sec != null) {
            for (String world : sec.getKeys(false)) {
                String cmd = sec.getString(world);
                if (cmd != null && !cmd.isBlank()) {
                    worldCmds.put(world, cmd);
                    String root = cmd.startsWith("/") ? cmd.substring(1) : cmd;
                    allowedRootCommands.add(root.split(" ")[0].toLowerCase());
                }
            }
        }

        if (listener != null) listener.shutdown();
        listener = new RecipeBookPacketListener(plugin, worldCmds, allowedRootCommands, guiBuilder);
    }

    public void disable() {
        if (listener != null) listener.shutdown();
    }
}
