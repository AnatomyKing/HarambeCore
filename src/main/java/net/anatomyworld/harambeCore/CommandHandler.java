package net.anatomyworld.harambeCore;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.anatomyworld.harambeCore.death.DeathChestModule;
import net.anatomyworld.harambeCore.death.DeathKeyCommandHelper;
import net.anatomyworld.harambeCore.dialogue.DialogueManager;
import net.anatomyworld.harambeCore.dialogue.DialogueModule;
import net.anatomyworld.harambeCore.item.ItemRegistry;
import net.anatomyworld.harambeCore.rewards.RewardHandler;
import net.anatomyworld.harambeCore.rewards.RewardGroupManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.bukkit.Bukkit;

import org.bukkit.entity.Player;

/**
 * Registers all plugin commands.
 */
public class CommandHandler {

    private final HarambeCore plugin;
    private final GuiBuilder guiBuilder;
    private final RewardHandler rewardHandler;
    private final RewardGroupManager rewardGroupManager;
    private final ItemRegistry itemRegistry;
    private final DialogueModule dialogueModule;
    private final DeathChestModule deathChestModule;

    public CommandHandler(HarambeCore plugin,
                          GuiBuilder guiBuilder,
                          RewardHandler rewardHandler,
                          RewardGroupManager rewardGroupManager,
                          ItemRegistry itemRegistry,
                          DialogueModule dialogueModule,
                          DeathChestModule deathChestModule) {
        this.plugin = plugin;
        this.guiBuilder = guiBuilder;
        this.rewardHandler = rewardHandler;
        this.rewardGroupManager = rewardGroupManager;
        this.itemRegistry = itemRegistry;
        this.dialogueModule = dialogueModule;
        this.deathChestModule = deathChestModule;
    }

    public void registerCommands() {
        CommandDispatcher<CommandSourceStack> dispatcher =
                ((org.bukkit.craftbukkit.CraftServer) Bukkit.getServer())
                        .getServer()
                        .getCommands()
                        .getDispatcher();

        // --- /anyphone info ...
        dispatcher.register(
                Commands.literal("anyphone")
                        .then(Commands.literal("info")
                                .then(Commands.argument("key", StringArgumentType.word())
                                        .executes(ctx -> {
                                            var sender = ctx.getSource().getBukkitSender();
                                            if (!(sender instanceof Player player)) {
                                                sender.sendMessage("§cOnly players can use this command.");
                                                return 0;
                                            }
                                            String key = StringArgumentType.getString(ctx, "key");
                                            var pages = dialogueModule.getPages(key);
                                            if (pages == null || pages.isEmpty()) {
                                                player.sendMessage("§cNo dialogue found for key '" + key + "'.");
                                                return 0;
                                            }
                                            DialogueManager.startDialogue(
                                                    player, pages, dialogueModule.getTickDelay());
                                            return 1;
                                        })
                                )
                        )
        );

        // --- Core /harambe tree
        var harambe = Commands.literal("harambe")
                .then(Commands.literal("reload")
                        .executes(ctx -> {
                            plugin.reloadPlugin();
                            ctx.getSource().getBukkitSender()
                                    .sendMessage("§aConfiguration reloaded.");
                            return 1;
                        })
                )

                // /harambe set ...
                .then(Commands.literal("set")
                        .then(Commands.literal("rewardgroup")
                                .then(Commands.argument("group", StringArgumentType.word())
                                        .then(Commands.argument("item", StringArgumentType.word())
                                                .then(Commands.argument("reward", StringArgumentType.word())
                                                        .executes(ctx -> {
                                                            String group = StringArgumentType.getString(ctx, "group");
                                                            String item = StringArgumentType.getString(ctx, "item");
                                                            String reward = StringArgumentType.getString(ctx, "reward");

                                                            rewardGroupManager.addReward(group, item, reward);
                                                            ctx.getSource().getBukkitSender()
                                                                    .sendMessage("§aReward mapping added to group '" + group + "'.");
                                                            return 1;
                                                        })
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("reward")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .then(Commands.argument("item", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    Player target = Bukkit
                                                            .getPlayer(StringArgumentType.getString(ctx, "player"));
                                                    String item = StringArgumentType.getString(ctx, "item");

                                                    if (target != null && rewardHandler.queueReward(
                                                            target.getUniqueId(), item)) {
                                                        ctx.getSource().getBukkitSender()
                                                                .sendMessage("§aQueued reward from item '"
                                                                        + item + "' for " + target.getName());
                                                        return 1;
                                                    }
                                                    ctx.getSource().getBukkitSender()
                                                            .sendMessage("§cFailed to queue reward.");
                                                    return 0;
                                                })
                                        )
                                )
                        )
                )

                // /harambe give ...
                .then(Commands.literal("give")

                        // /harambe give reward ...
                        .then(Commands.literal("reward")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .then(Commands.argument("group", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    Player target = Bukkit
                                                            .getPlayer(StringArgumentType.getString(ctx, "player"));
                                                    String group = StringArgumentType.getString(ctx, "group");

                                                    if (target != null) {
                                                        rewardHandler.giveAllRewards(
                                                                target, group, itemRegistry);
                                                        ctx.getSource().getBukkitSender()
                                                                .sendMessage("§aGave all rewards from group '"
                                                                        + group + "' to " + target.getName());
                                                        return 1;
                                                    }
                                                    ctx.getSource().getBukkitSender()
                                                            .sendMessage("§cPlayer not found.");
                                                    return 0;
                                                })
                                                .then(Commands.argument("item", StringArgumentType.word())
                                                        .executes(ctx -> {
                                                            Player target = Bukkit
                                                                    .getPlayer(StringArgumentType.getString(ctx, "player"));
                                                            String group = StringArgumentType.getString(ctx, "group");
                                                            String item = StringArgumentType.getString(ctx, "item");

                                                            if (target != null) {
                                                                boolean success = rewardHandler.giveReward(
                                                                        target, group, item, itemRegistry);
                                                                if (success) {
                                                                    ctx.getSource().getBukkitSender()
                                                                            .sendMessage("§aGave reward '"
                                                                                    + item + "' to " + target.getName());
                                                                    return 1;
                                                                } else {
                                                                    ctx.getSource().getBukkitSender()
                                                                            .sendMessage("§cReward item not found or not queued.");
                                                                }
                                                            }
                                                            return 0;
                                                        })
                                                )
                                        )
                                )
                        )

                        // --- NEW: /harambe give deathkey <target> <owner> [group]
                        .then(Commands.literal("deathkey")
                                .then(Commands.argument("target", StringArgumentType.word())
                                        .then(Commands.argument("owner", StringArgumentType.word())
                                                // without group
                                                .executes(ctx -> {
                                                    var sender = ctx.getSource().getBukkitSender();
                                                    if (!(sender instanceof Player)) {
                                                        sender.sendMessage("§cOnly players can use this command.");
                                                        return 0;
                                                    }
                                                    Player target = Bukkit
                                                            .getPlayer(StringArgumentType.getString(ctx, "target"));
                                                    if (target == null) {
                                                        return 0;
                                                    }
                                                    String ownerName = StringArgumentType
                                                            .getString(ctx, "owner");

                                                    DeathKeyCommandHelper.giveKeyToPlayer(
                                                            target, ownerName, null, deathChestModule);

                                                    return 1;
                                                })
                                                // with group
                                                .then(Commands.argument("group", StringArgumentType.word())
                                                        .executes(ctx -> {
                                                            var sender = ctx.getSource().getBukkitSender();
                                                            if (!(sender instanceof Player)) {
                                                                sender.sendMessage("§cOnly players can use this command.");
                                                                return 0;
                                                            }
                                                            Player target = Bukkit
                                                                    .getPlayer(StringArgumentType.getString(ctx, "target"));
                                                            if (target == null) {
                                                                return 0;
                                                            }
                                                            String ownerName = StringArgumentType
                                                                    .getString(ctx, "owner");
                                                            String group = StringArgumentType
                                                                    .getString(ctx, "group");

                                                            DeathKeyCommandHelper.giveKeyToPlayer(
                                                                    target, ownerName, group, deathChestModule);
                                                            return 1;
                                                        })
                                                )
                                        )
                                )
                        )
                );

        dispatcher.register(harambe);

        // --- dynamic GUI commands
        guiBuilder.getGuiKeys().forEach(guiKey ->
                dispatcher.register(
                        Commands.literal(guiKey.toLowerCase())
                                .executes(ctx -> {
                                    var sender = ctx.getSource().getBukkitSender();
                                    if (sender instanceof Player player) {
                                        guiBuilder.createAndOpenGui(guiKey, player);
                                        return 1;
                                    } else {
                                        sender.sendMessage("§cOnly players can open GUIs.");
                                        return 0;
                                    }
                                })
                )
        );

        plugin.getLogger().info("§aRegistered /harambe command tree and GUI commands.");
    }
}
