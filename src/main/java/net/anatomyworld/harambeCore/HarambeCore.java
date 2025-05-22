// HarambeCore.java
package net.anatomyworld.harambeCore;

import net.anatomyworld.harambeCore.config.YamlConfigLoader;
import net.anatomyworld.harambeCore.death.DeathListener;
import net.anatomyworld.harambeCore.dialogue.DialogueListeners;
import net.anatomyworld.harambeCore.dialogue.DialogueModule;
import net.anatomyworld.harambeCore.item.ItemRegistry;
import net.anatomyworld.harambeCore.item.MythicMobsRegistry;
import net.anatomyworld.harambeCore.rewards.PlayerRewardData;
import net.anatomyworld.harambeCore.rewards.RewardHandler;
import net.anatomyworld.harambeCore.rewards.RewardGroupModule;
import net.anatomyworld.harambeCore.storage.StorageManager;
import net.anatomyworld.harambeCore.util.AdventureModeHandler;
import net.anatomyworld.harambeCore.util.EconomyHandler;
import net.anatomyworld.harambeCore.util.YLevelTeleportHandler;
import net.anatomyworld.harambeCore.util.poison.PoisonModule;
import net.anatomyworld.harambeCore.util.recipebook.RecipeBookModule;

import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public final class HarambeCore extends JavaPlugin {

    public static final Logger logger = LoggerFactory.getLogger(HarambeCore.class);

    private ItemRegistry       itemRegistry;
    private GuiBuilder         guiBuilder;
    private CommandHandler     commandHandler;
    private StorageManager     storageManager;
    private RewardHandler      rewardHandler;

    /* util modules */
    private DialogueModule     dialogueModule;
    private PoisonModule       poisonModule;
    private RecipeBookModule   recipeBookModule;
    private RewardGroupModule  rewardGroupModule;
    private YLevelTeleportHandler yLevelTeleportHandler;

    @Override
    public void onEnable() {
        logger.info("§aHarambeCore starting…");

        // Economy check
        if (!EconomyHandler.setupEconomy()) {
            logger.error("No Vault / economy plugin – disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Config + util folder
        saveDefaultConfig();
        new File(getDataFolder(), "util").mkdirs();

        // Dialogue module (loads util/dialog-wiki.yml)
        dialogueModule = new DialogueModule(this);
        dialogueModule.enable();
        getServer().getPluginManager().registerEvents(
                new DialogueListeners(dialogueModule),
                this
        );

        // Core listeners
        getServer().getPluginManager().registerEvents(
                new AdventureModeHandler(this),
                this
        );

        yLevelTeleportHandler = new YLevelTeleportHandler(this);
        getServer().getPluginManager().registerEvents(yLevelTeleportHandler, this);

        // Registries & storage
        itemRegistry   = new MythicMobsRegistry();
        storageManager = new StorageManager(this);

        // Build GUI
        guiBuilder = new GuiBuilder(this, getConfig(), itemRegistry, storageManager);

        // Load other util modules
        poisonModule      = new PoisonModule(this);
        recipeBookModule  = new RecipeBookModule(this, guiBuilder);
        rewardGroupModule = new RewardGroupModule(this);

        poisonModule.enable();
        recipeBookModule.enable();
        rewardGroupModule.enable();

        // Setup rewards
        PlayerRewardData playerData = new PlayerRewardData(this);
        rewardHandler = new RewardHandler(
                rewardGroupModule.getManager(),
                playerData
        );

        new DeathListener(this, rewardHandler);// ✨ NEW

        // Commands & GUI listener
        commandHandler = new CommandHandler(
                this,
                guiBuilder,
                rewardHandler,
                rewardGroupModule.getManager(),
                itemRegistry,
                dialogueModule
        );
        commandHandler.registerCommands();

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
        dialogueModule.disable();
        storageManager.onShutdown();

        logger.info("§cHarambeCore disabled.");
    }

    /** Call from your `/harambecore reload` command */
    public void reloadPlugin() {
        reloadConfig();
        guiBuilder.updateConfig(getConfig());
        commandHandler.registerCommands();  // re-register after reload
        dialogueModule.enable();
        yLevelTeleportHandler.reload();
        poisonModule.enable();
        recipeBookModule.enable();
        rewardGroupModule.enable();
        logger.info("§aHarambeCore reloaded.");
    }
}