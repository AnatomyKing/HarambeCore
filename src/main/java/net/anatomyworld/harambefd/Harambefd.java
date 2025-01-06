package net.anatomyworld.harambefd;

import com.civious.dungeonmmo.api.DungeonMMOAPI;
import net.anatomyworld.harambefd.guis.enderlink.EnderlinkMethods;
import net.anatomyworld.harambefd.harambemethods.PoisonEffect;
import net.anatomyworld.harambefd.harambemethods.ItemRegistry;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class Harambefd extends JavaPlugin implements Listener {

    public static final Logger logger = LoggerFactory.getLogger(Harambefd.class);
    private FileConfiguration config;
    private ItemRegistry itemRegistry;
    private GuiBuilder guiBuilder;  // Declare GuiBuilder here

    @Override
    public void onEnable() {
        getLogger().info("Harambefd has been enabled!");

        if (!EconomyHandler.setupEconomy()) {
            getLogger().severe("Vault not found or no economy plugin found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        // Ensure default config is saved if it doesn't exist
        saveDefaultConfig();
        config = getConfig();  // Initialize the config again to ensure it's loaded

        // Ensure the 'submititems' folder exists
        ensureSubmitItemsFolderExists();

        EnderlinkMethods.initialize(this);

        // Initialize the ItemRegistry
        itemRegistry = new ItemRegistry(this);

        // Initialize the GuiBuilder with the config
        guiBuilder = new GuiBuilder(config);  // Moved outside the local block to be accessible globally

        // Register DungeonMMO Item Provider
        //registerDungeonMMOItemProvider();


        // Register commands and events
        CommandHandler commandHandler = new CommandHandler(this, guiBuilder, itemRegistry);
        commandHandler.registerCommands();

        // Register GUI Event Listener separately
        registerGuiEventListener();

        // Register the PoisonEffect with configuration data
        registerPoisonEffect();

        getLogger().info("Harambefd plugin setup complete!");
    }

    @Override
    public void onDisable() {
        EnderlinkMethods.saveAllStorage();
        getLogger().info("Harambefd has been disabled!");
    }

    // Method to ensure 'submititems' folder exists
    private void ensureSubmitItemsFolderExists() {
        File submitItemsFolder = new File(getDataFolder(), "submititems");
        if (!submitItemsFolder.exists() && !submitItemsFolder.mkdirs()) {
            getLogger().warning("Failed to create 'submititems' directory.");
        }
    }

    // Register the DungeonMMO Item Provider
    //private void registerDungeonMMOItemProvider() {
    //    DungeonMMOAPI.getInstance().registerItemProvider("harambefd", itemTag -> {
    //        if (itemTag == null || itemTag.isEmpty()) {
    //            logger.error("Received null or empty itemTag: {}", itemTag);
    //            return null;
    //        }
    //        return itemRegistry.generateItem(itemTag);
    //    });
    //}

    // Register the PoisonEffect listener
    private void registerPoisonEffect() {
        // Get poison world and block data from config
        String poisonWorld = config.getString("poison.poison-world", "dungeon_build");
        String poisonBlockString = config.getString("poison.poison-block");

        // Log to check if the configuration values are loaded
        getLogger().info("Loaded poison-world: " + poisonWorld);
        getLogger().info("Loaded poison-block: " + (poisonBlockString != null ? poisonBlockString : "null"));

        BlockData poisonBlockData = null;
        try {
            if (poisonBlockString != null && !poisonBlockString.isEmpty()) {
                poisonBlockData = Bukkit.createBlockData(poisonBlockString);
                getLogger().info("Loaded poison-block data: " + poisonBlockData.getAsString());
            } else {
                getLogger().warning("poison-block configuration is missing or empty.");
            }
        } catch (IllegalArgumentException e) {
            getLogger().warning("Invalid poison-block data: " + poisonBlockString);
        }

        if (poisonBlockData != null) {
            PoisonEffect poisonEffect = new PoisonEffect(poisonWorld, poisonBlockData);
            getServer().getPluginManager().registerEvents(poisonEffect, this);
        } else {
            getLogger().warning("Failed to load poison-block from config.");
        }
    }

    // Register the GUI Event Listener separately
    private void registerGuiEventListener() {
        GuiEventListener guiEventListener = new GuiEventListener(guiBuilder, itemRegistry);
        getServer().getPluginManager().registerEvents(guiEventListener, this);
    }
}

