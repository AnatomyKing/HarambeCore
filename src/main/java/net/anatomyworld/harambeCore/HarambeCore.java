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
    private RecipeBookPacketListener recipeBookPacketListener;

    @Override
    public void onEnable() {
        getLogger().info("§aHarambeCore has been enabled!");

        // Ensure Vault or economy plugin is present
        if (!EconomyHandler.setupEconomy()) {
            getLogger().severe("Vault not found or no economy plugin found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        this.config = getConfig();
        ensureSubmitItemsFolderExists();

        // Initialize custom systems
        itemRegistry = new ItemRegistry(this);
        guiBuilder = new GuiBuilder(this, config);
        commandHandler = new CommandHandler(this, guiBuilder, itemRegistry);

        registerGuiEventListener();
        registerPoisonEffect();

        // Retrieve the recipe book command from config; default to "enderlink"
        String recipeBookCommand = config.getString("recipe-book-command", "enderlink");
        // Register packet listener and pass our config-based command
        recipeBookPacketListener = new RecipeBookPacketListener(this, recipeBookCommand);

        // Register your custom commands
        commandHandler.registerCommands();

        getLogger().info("§aHarambeCore plugin setup complete!");
    }

    @Override
    public void onDisable() {
        // Remove packet interceptors
        if (recipeBookPacketListener != null) {
            recipeBookPacketListener.shutdown();
        }

        getLogger().info("§cHarambeCore has been disabled!");
    }

    public void reloadPlugin() {
        reloadConfig();
        this.config = getConfig();

        guiBuilder.updateConfig(config);
        commandHandler.registerCommands();
        registerPoisonEffect();

        // Re-inject packet listeners for all players
        if (recipeBookPacketListener != null) {
            recipeBookPacketListener.shutdown();
        }

        // Again, read from config in case it changed, then re-create the listener
        String recipeBookCommand = config.getString("recipe-book-command", "enderlink");
        recipeBookPacketListener = new RecipeBookPacketListener(this, recipeBookCommand);

        getLogger().info("§aPlugin fully reloaded.");
    }

    private void ensureSubmitItemsFolderExists() {
        File submitItemsFolder = new File(getDataFolder(), "submititems");
        if (!submitItemsFolder.exists() && !submitItemsFolder.mkdirs()) {
            getLogger().warning("Failed to create 'submititems' directory.");
        }
    }

    private void registerGuiEventListener() {
        getServer().getPluginManager().registerEvents(
                new GuiEventListener(guiBuilder, itemRegistry), this);
    }

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

