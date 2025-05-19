// src/main/java/net/anatomyworld/harambeCore/dialogue/DialogueModule.java
package net.anatomyworld.harambeCore.dialogue;

import net.anatomyworld.harambeCore.config.YamlConfigLoader;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class DialogueModule {
    private final JavaPlugin plugin;
    private Map<String, List<List<String>>> dialogueMap = Collections.emptyMap();
    private long tickDelay = 5L;

    public DialogueModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Load (or reload) util/dialog-wiki.yml into memory. */
    public void enable() {
        FileConfiguration cfg = YamlConfigLoader.load(plugin, "util/dialog-wiki.yml");
        tickDelay = cfg.getLong("tick-delay", 5L);

        ConfigurationSection root = cfg.getConfigurationSection("dialog-wiki");
        if (root == null) {
            plugin.getLogger().warning("[Dialogue] no 'dialog-wiki' section found");
            dialogueMap = Collections.emptyMap();
            return;
        }

        Map<String, List<List<String>>> map = new LinkedHashMap<>();
        for (String key : root.getKeys(false)) {
            List<List<String>> pages = new ArrayList<>();
            // YAML "pages" is a sequence-of-sequences
            List<?> rawPages = cfg.getList("dialog-wiki." + key + ".pages");
            if (rawPages != null) {
                for (Object pageObj : rawPages) {
                    if (pageObj instanceof List<?> frameList) {
                        List<String> frames = new ArrayList<>();
                        for (Object f : frameList) {
                            if (f != null) frames.add(f.toString());
                        }
                        pages.add(frames);
                    }
                }
            }
            map.put(key, pages);
        }

        dialogueMap = Collections.unmodifiableMap(map);
        plugin.getLogger().info("[Dialogue] loaded " + map.size() + " entries");
    }

    /** Nothing to persist on disable. */
    public void disable() { }

    /** @return the list of pages (each a list of frames) for this item key, or null */
    public List<List<String>> getPages(String itemKey) {
        return dialogueMap.get(itemKey);
    }

    /** @return configured tick delay between frames */
    public long getTickDelay() {
        return tickDelay;
    }
}
