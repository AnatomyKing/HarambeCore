package net.anatomyworld.harambeCore;

import net.anatomyworld.harambeCore.item.ItemRegistry;
import net.anatomyworld.harambeCore.item.MythicMobsRegistry;
import net.anatomyworld.harambeCore.item.PlayerRewardData;
import net.anatomyworld.harambeCore.item.RewardGroupManager;
import net.anatomyworld.harambeCore.item.RewardHandler;
import net.anatomyworld.harambeCore.util.EconomyHandler;
import net.anatomyworld.harambeCore.util.PoisonEffect;
import net.anatomyworld.harambeCore.util.RecipeBookPacketListener;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class HarambeCore extends JavaPlugin implements Listener {

    public static final Logger logger = LoggerFactory.getLogger(HarambeCore.class);

    private FileConfiguration config;
    private ItemRegistry itemRegistry;
    private GuiBuilder guiBuilder;
    private CommandHandler commandHandler;
    private RecipeBookPacketListener recipeBookPacketListener;

    // These are now instance fields to allow correct passing into listeners
    private RewardHandler rewardHandler;

    @Override
    public void onEnable() {
        getLogger().info("§aHarambeCore has been enabled!");

        // Economy check
        if (!EconomyHandler.setupEconomy()) {
            getLogger().severe("Vault not found or no economy plugin found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Load config and folders
        saveDefaultConfig();
        this.config = getConfig();
        ensureDataFolders();

        // Initialize plugin components
        this.itemRegistry = new MythicMobsRegistry();
        this.guiBuilder = new GuiBuilder(this, config);
        RewardGroupManager rewardGroupManager = new RewardGroupManager(this);
        PlayerRewardData playerRewardData = new PlayerRewardData(getDataFolder());
        this.rewardHandler = new RewardHandler(rewardGroupManager, playerRewardData);
        this.commandHandler = new CommandHandler(this, guiBuilder, rewardHandler, rewardGroupManager, itemRegistry);

        // Register everything
        registerGuiEventListener();
        registerPoisonEffect();
        commandHandler.registerCommands();

        // Custom packet listener setup
        ConfigurationSection section = config.getConfigurationSection("recipe-book-commands");
        Map<String, String> worldCommandMap = new HashMap<>();
        if (section != null) {
            for (String worldName : section.getKeys(false)) {
                worldCommandMap.put(worldName, section.getString(worldName));
            }
        }
        recipeBookPacketListener = new RecipeBookPacketListener(this, worldCommandMap);

        getLogger().info("§aHarambeCore plugin setup complete!");
    }

    @Override
    public void onDisable() {
        if (recipeBookPacketListener != null) {
            recipeBookPacketListener.shutdown();
        }

        getLogger().info("§cHarambeCore has been disabled!");
    }

    public void reloadPlugin() {
        reloadConfig();
        this.config = getConfig();

        guiBuilder.updateConfig(config);
        registerPoisonEffect();
        commandHandler.registerCommands();

        if (recipeBookPacketListener != null) {
            recipeBookPacketListener.shutdown();
        }

        // Load world-specific recipe book commands from config
        ConfigurationSection section = config.getConfigurationSection("recipe-book-commands");
        Map<String, String> worldCommandMap = new HashMap<>();
        if (section != null) {
            for (String worldName : section.getKeys(false)) {
                worldCommandMap.put(worldName, section.getString(worldName));
            }
        }

        recipeBookPacketListener = new RecipeBookPacketListener(this, worldCommandMap);

        getLogger().info("§aPlugin fully reloaded.");
    }

    private void ensureDataFolders() {
        File configFolder = new File(getDataFolder(), "config");
        File playerDataFolder = new File(getDataFolder(), "playerdata");

        if (!configFolder.exists()) configFolder.mkdirs();
        if (!playerDataFolder.exists()) playerDataFolder.mkdirs();
    }

    private void registerGuiEventListener() {
        getServer().getPluginManager().registerEvents(
                new GuiEventListener(guiBuilder, rewardHandler, itemRegistry), this);
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
