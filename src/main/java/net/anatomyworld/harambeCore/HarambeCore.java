package net.anatomyworld.harambeCore;

import net.anatomyworld.harambeCore.config.YamlConfigLoader;
import net.anatomyworld.harambeCore.dialogue.DialogueListeners;
import net.anatomyworld.harambeCore.item.ItemRegistry;
import net.anatomyworld.harambeCore.item.MythicMobsRegistry;
import net.anatomyworld.harambeCore.rewards.PlayerRewardData;
import net.anatomyworld.harambeCore.rewards.RewardHandler;
import net.anatomyworld.harambeCore.storage.StorageManager;
import net.anatomyworld.harambeCore.util.AdventureModeHandler;
import net.anatomyworld.harambeCore.util.EconomyHandler;
import net.anatomyworld.harambeCore.util.poison.PoisonModule;
import net.anatomyworld.harambeCore.util.recipebook.RecipeBookModule;
import net.anatomyworld.harambeCore.rewards.RewardGroupModule;

import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Main plugin entry point.
 */
public final class HarambeCore extends JavaPlugin {

    public static final Logger logger = LoggerFactory.getLogger(HarambeCore.class);

    private ItemRegistry     itemRegistry;
    private GuiBuilder       guiBuilder;
    private CommandHandler   commandHandler;
    private StorageManager   storageManager;
    private RewardHandler    rewardHandler;

    /* util modules */
    private PoisonModule      poisonModule;
    private RecipeBookModule  recipeBookModule;
    private RewardGroupModule rewardGroupModule;

    @Override
    public void onEnable() {
        logger.info("§aHarambeCore starting…");

        // 1. Economy check
        if (!EconomyHandler.setupEconomy()) {
            logger.error("No Vault / economy plugin – disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 2. Config + util folder
        saveDefaultConfig();
        new File(getDataFolder(), "util").mkdirs();

        // 3. Core listeners
        getServer().getPluginManager().registerEvents(new DialogueListeners(), this);
        getServer().getPluginManager().registerEvents(new AdventureModeHandler(this), this);

        // 4. Registries & storage
        itemRegistry   = new MythicMobsRegistry();
        storageManager = new StorageManager(this);

        // 5. Build GUI
        guiBuilder = new GuiBuilder(this, getConfig(), itemRegistry, storageManager);

        // 6. Load modules
        poisonModule      = new PoisonModule(this);
        recipeBookModule  = new RecipeBookModule(this);
        rewardGroupModule = new RewardGroupModule(this);

        poisonModule.enable();
        recipeBookModule.enable();
        rewardGroupModule.enable();

        // 7. Setup rewards
        PlayerRewardData playerData = new PlayerRewardData(this);
        rewardHandler = new RewardHandler(
                rewardGroupModule.getManager(),
                playerData
        );

        // 8. Commands & GUI listener
        commandHandler = new CommandHandler(
                this,
                guiBuilder,
                rewardHandler,
                rewardGroupModule.getManager(),
                itemRegistry
        );
        getServer().getPluginManager().registerEvents(
                new GuiEventListener(guiBuilder, rewardHandler, itemRegistry),
                this
        );

        logger.info("§aHarambeCore enabled successfully.");
    }

    @Override
    public void onDisable() {
        // shutdown modules and save GUI data
        recipeBookModule.disable();
        rewardGroupModule.disable();
        storageManager.onShutdown();

        logger.info("§cHarambeCore disabled.");
    }

    /** Call from your reload command */
    public void reloadPlugin() {
        reloadConfig();
        guiBuilder.updateConfig(getConfig());
        commandHandler.registerCommands();
        poisonModule.enable();
        recipeBookModule.enable();
        rewardGroupModule.enable();
        logger.info("§aHarambeCore reloaded.");
    }
}
