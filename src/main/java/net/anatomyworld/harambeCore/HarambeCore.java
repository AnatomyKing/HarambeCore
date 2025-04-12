package net.anatomyworld.harambeCore;

import net.anatomyworld.harambeCore.item.EconomyHandler;
import net.anatomyworld.harambeCore.item.ItemRegistry;
import net.anatomyworld.harambeCore.world.PoisonEffect;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class HarambeCore extends JavaPlugin implements Listener {

    public static final Logger logger = LoggerFactory.getLogger(HarambeCore.class);

    private FileConfiguration config;
    private ItemRegistry itemRegistry;
    private GuiBuilder guiBuilder;
    private CommandHandler commandHandler;

    @Override
    public void onEnable() {
        getLogger().info("§aHarambeCore has been enabled!");

        // Check economy hook
        if (!EconomyHandler.setupEconomy()) {
            getLogger().severe("Vault not found or no economy plugin found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Prepare config and folders
        saveDefaultConfig();
        config = getConfig();
        ensureSubmitItemsFolderExists();

        // Initialize systems
        itemRegistry = new ItemRegistry(this);
        guiBuilder = new GuiBuilder(this, config);
        commandHandler = new CommandHandler(this, guiBuilder, itemRegistry);

        // Register event listeners
        registerGuiEventListener();
        registerPoisonEffect();

        // Register commands (Brigadier)
        commandHandler.registerCommands();

        getLogger().info("§aHarambeCore plugin setup complete!");
    }

    @Override
    public void onDisable() {
        getLogger().info("§cHarambeCore has been disabled!");
    }

    /**
     * Called when reloading the plugin (hot-reload safe)
     */
    public void reloadPlugin() {
        reloadConfig();
        config = getConfig();

        guiBuilder.updateConfig(config);
        commandHandler.registerCommands(); // re-register commands to reflect new GUIs
        registerPoisonEffect();

        getLogger().info("§aPlugin fully reloaded.");
    }

    // Ensure 'submititems' folder exists
    private void ensureSubmitItemsFolderExists() {
        File submitItemsFolder = new File(getDataFolder(), "submititems");
        if (!submitItemsFolder.exists() && !submitItemsFolder.mkdirs()) {
            getLogger().warning("Failed to create 'submititems' directory.");
        }
    }

    // Register GUI Event Listener
    private void registerGuiEventListener() {
        getServer().getPluginManager().registerEvents(new GuiEventListener(guiBuilder, itemRegistry), this);
    }

    // Register PoisonEffect system
    private void registerPoisonEffect() {
        String poisonWorld = config.getString("poison.poison-world", "dungeon_build");
        String poisonBlockString = config.getString("poison.poison-block");

        getLogger().info("Loaded poison-world: " + poisonWorld);
        getLogger().info("Loaded poison-block: " + (poisonBlockString != null ? poisonBlockString : "null"));

        if (poisonBlockString != null && !poisonBlockString.isEmpty()) {
            try {
                BlockData poisonBlockData = Bukkit.createBlockData(poisonBlockString);
                getLogger().info("Loaded poison-block data: " + poisonBlockData.getAsString());
                PoisonEffect poisonEffect = new PoisonEffect(poisonWorld, poisonBlockData);
                getServer().getPluginManager().registerEvents(poisonEffect, this);
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid poison-block data: " + poisonBlockString);
            }
        } else {
            getLogger().warning("poison-block configuration is missing or empty.");
        }
    }

}