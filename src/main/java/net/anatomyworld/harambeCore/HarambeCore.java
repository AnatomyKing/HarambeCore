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
        config = getConfig();
        ensureSubmitItemsFolderExists();

        // Initialize custom systems
        itemRegistry = new ItemRegistry(this);
        guiBuilder = new GuiBuilder(this, config);
        commandHandler = new CommandHandler(this, guiBuilder, itemRegistry);

        registerGuiEventListener();
        registerPoisonEffect();

        // Register packet listener to block recipe book GUI by re-routing to SMOKER
        recipeBookPacketListener = new RecipeBookPacketListener(this);

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

    /**
     * Example of a reload routine
     */
    public void reloadPlugin() {
        reloadConfig();
        config = getConfig();

        guiBuilder.updateConfig(config);
        commandHandler.registerCommands();
        registerPoisonEffect();

        // Re-inject packet listeners for all players
        if (recipeBookPacketListener != null) {
            recipeBookPacketListener.shutdown();
        }
        recipeBookPacketListener = new RecipeBookPacketListener(this);

        getLogger().info("§aPlugin fully reloaded.");
    }

    private void ensureSubmitItemsFolderExists() {
        File submitItemsFolder = new File(getDataFolder(), "submititems");
        if (!submitItemsFolder.exists() && !submitItemsFolder.mkdirs()) {
            getLogger().warning("Failed to create 'submititems' directory.");
        }
    }

    private void registerGuiEventListener() {
        getServer().getPluginManager().registerEvents(new GuiEventListener(guiBuilder, itemRegistry), this);
    }

    /**
     * Example event registration for a custom PoisonEffect
     */
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
