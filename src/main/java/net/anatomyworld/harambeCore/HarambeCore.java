// HarambeCore.java
package net.anatomyworld.harambeCore;

import net.anatomyworld.harambeCore.death.DeathChestManager;
import net.anatomyworld.harambeCore.death.DeathChestModule;
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
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Main plugin entry-point.
 */
public final class HarambeCore extends JavaPlugin {

    /* ---------- static logger ---------- */
    public static final Logger logger = LoggerFactory.getLogger(HarambeCore.class);

    /* ---------- core components ---------- */
    private ItemRegistry      itemRegistry;
    private GuiBuilder        guiBuilder;
    private CommandHandler    commandHandler;
    private StorageManager    storageManager;
    private RewardHandler     rewardHandler;

    /* ---------- util / feature modules ---------- */
    private DialogueModule        dialogueModule;
    private PoisonModule          poisonModule;
    private RecipeBookModule      recipeBookModule;
    private RewardGroupModule     rewardGroupModule;
    private YLevelTeleportHandler yLevelTeleportHandler;
    private DeathChestModule      deathChestModule;
    private DeathChestManager     deathChestManager;

    /* ====================================================================== */
    /*  ENABLE                                                                */
    /* ====================================================================== */
    @Override
    public void onEnable() {

        logger.info("§aHarambeCore starting…");

        /* economy check ---------------------------------------------------- */
        if (!EconomyHandler.setupEconomy()) {
            logger.error("No Vault / economy plugin – disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        /* config & util dir ------------------------------------------------- */
        saveDefaultConfig();
        new File(getDataFolder(), "util").mkdirs();

        /* dialogue system --------------------------------------------------- */
        dialogueModule = new DialogueModule(this);
        dialogueModule.enable();
        Bukkit.getPluginManager().registerEvents(new DialogueListeners(dialogueModule), this);

        /* base listeners ---------------------------------------------------- */
        Bukkit.getPluginManager().registerEvents(new AdventureModeHandler(this), this);
        yLevelTeleportHandler = new YLevelTeleportHandler(this);
        Bukkit.getPluginManager().registerEvents(yLevelTeleportHandler, this);

        /* registries & storage --------------------------------------------- */
        itemRegistry   = new MythicMobsRegistry();
        storageManager = new StorageManager(this);

        /* GUI builder ------------------------------------------------------- */
        guiBuilder = new GuiBuilder(this, getConfig(), itemRegistry, storageManager);

        /* small util modules ------------------------------------------------ */
        poisonModule      = new PoisonModule(this);
        recipeBookModule  = new RecipeBookModule(this, guiBuilder);
        rewardGroupModule = new RewardGroupModule(this);
        deathChestModule  = new DeathChestModule(this);

        poisonModule.enable();
        recipeBookModule.enable();
        rewardGroupModule.enable();
        deathChestModule.enable();   // loads util/death-chest.yml

        /* reward system ----------------------------------------------------- */
        PlayerRewardData playerData = new PlayerRewardData(this);
        rewardHandler = new RewardHandler(rewardGroupModule.getManager(), playerData);

        /* death-chest runtime helper --------------------------------------- */
        deathChestManager = new DeathChestManager(this, deathChestModule);
        // (constructor registers its own furniture listeners)

        /* death-event hook -------------------------------------------------- */
        new DeathListener(this, rewardHandler, deathChestModule, deathChestManager);

        /* commands & GUI listener ------------------------------------------ */
        commandHandler = new CommandHandler(
                this,
                guiBuilder,
                rewardHandler,
                rewardGroupModule.getManager(),
                itemRegistry,
                dialogueModule
        );
        commandHandler.registerCommands();

        Bukkit.getPluginManager().registerEvents(
                new GuiEventListener(guiBuilder, rewardHandler, itemRegistry),
                this
        );

        logger.info("§aHarambeCore enabled successfully.");
    }

    /* ====================================================================== */
    /*  DISABLE                                                               */
    /* ====================================================================== */
    @Override
    public void onDisable() {
        recipeBookModule.disable();
        rewardGroupModule.disable();
        dialogueModule.disable();
        deathChestModule.disable();
        storageManager.onShutdown();

        logger.info("§cHarambeCore disabled.");
    }

    /* ====================================================================== */
    /*  /harambecore reload                                                   */
    /* ====================================================================== */
    public void reloadPlugin() {

        reloadConfig();
        guiBuilder.updateConfig(getConfig());
        commandHandler.registerCommands();          // pick up changes

        /* hot-reload utility modules */
        dialogueModule.enable();
        yLevelTeleportHandler.reload();
        poisonModule.enable();
        recipeBookModule.enable();
        rewardGroupModule.enable();
        deathChestModule.enable();                  // reload expiry etc.

        logger.info("§aHarambeCore reloaded.");
    }
}
