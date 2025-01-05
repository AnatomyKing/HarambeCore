package net.anatomyworld.harambefd;

import net.anatomyworld.harambefd.guieventlistener.*;
import net.anatomyworld.harambefd.harambemethods.ItemRegistry;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;

public class GuiEventListener {

    private final GuiBuilder guiBuilder;
    private final ItemRegistry itemRegistry;
    private final JavaPlugin plugin;

    public GuiEventListener(GuiBuilder guiBuilder, ItemRegistry itemRegistry, JavaPlugin plugin) {
        this.guiBuilder = guiBuilder;
        this.itemRegistry = itemRegistry;
        this.plugin = plugin;

        registerListeners();
    }

    private void registerListeners() {
        PluginManager pluginManager = plugin.getServer().getPluginManager();

        pluginManager.registerEvents(new OnInventoryShiftClick(guiBuilder, itemRegistry), plugin);
        pluginManager.registerEvents(new OnInventoryClick(guiBuilder, itemRegistry), plugin);
        pluginManager.registerEvents(new HandleCustomGui(guiBuilder, itemRegistry), plugin);
        pluginManager.registerEvents(new OnInventoryClose(guiBuilder, itemRegistry), plugin);
        pluginManager.registerEvents(new OnInventoryDrag(guiBuilder, itemRegistry), plugin);
    }
}
