package net.anatomyworld.harambeCore;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.anatomyworld.harambeCore.item.ItemRegistry;
import net.anatomyworld.harambeCore.item.RewardHandler;
import net.anatomyworld.harambeCore.item.RewardGroupManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Player;

public class CommandHandler {

    private final HarambeCore plugin;
    private final GuiBuilder guiBuilder;
    private final RewardHandler rewardHandler;
    private final RewardGroupManager rewardGroupManager;
    private final ItemRegistry itemRegistry;

    public CommandHandler(HarambeCore plugin, GuiBuilder guiBuilder,
                          RewardHandler rewardHandler,
                          RewardGroupManager rewardGroupManager,
                          ItemRegistry itemRegistry) {
        this.plugin = plugin;
        this.guiBuilder = guiBuilder;
        this.rewardHandler = rewardHandler;
        this.rewardGroupManager = rewardGroupManager;
        this.itemRegistry = itemRegistry;
    }

    public void registerCommands() {
        CommandDispatcher<CommandSourceStack> dispatcher = ((CraftServer) Bukkit.getServer())
                .getServer()
                .getCommands()
                .getDispatcher();

        // Core /harambe command tree
        dispatcher.register(
                Commands.literal("harambe")
                        .then(Commands.literal("reload")
                                .executes(ctx -> {
                                    plugin.reloadPlugin();
                                    ctx.getSource().getBukkitSender().sendMessage("§aConfiguration reloaded.");
                                    return 1;
                                })
                        )

                        // Set reward group entries
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
                                                                    ctx.getSource().getBukkitSender().sendMessage("§aReward mapping added to group '" + group + "'.");
                                                                    return 1;
                                                                })
                                                        )
                                                )
                                        )
                                )

                                // Assign reward to a player
                                .then(Commands.literal("reward")
                                        .then(Commands.argument("player", StringArgumentType.word())
                                                .then(Commands.argument("item", StringArgumentType.word())
                                                        .executes(ctx -> {
                                                            Player target = Bukkit.getPlayer(StringArgumentType.getString(ctx, "player"));
                                                            String item = StringArgumentType.getString(ctx, "item");

                                                            if (target != null && rewardHandler.queueReward(target.getUniqueId(), item)) {
                                                                ctx.getSource().getBukkitSender().sendMessage("§aQueued reward from item '" + item + "' for " + target.getName());
                                                                return 1;
                                                            }
                                                            ctx.getSource().getBukkitSender().sendMessage("§cFailed to queue reward.");
                                                            return 0;
                                                        })
                                                )
                                        )
                                )
                        )

                        // Give reward to player
                        .then(Commands.literal("give")
                                .then(Commands.literal("reward")
                                        .then(Commands.argument("player", StringArgumentType.word())
                                                .then(Commands.argument("group", StringArgumentType.word())
                                                        .executes(ctx -> {
                                                            Player target = Bukkit.getPlayer(StringArgumentType.getString(ctx, "player"));
                                                            String group = StringArgumentType.getString(ctx, "group");

                                                            if (target != null) {
                                                                rewardHandler.giveAllRewards(target, group, itemRegistry);
                                                                ctx.getSource().getBukkitSender().sendMessage("§aGave all rewards from group '" + group + "' to " + target.getName());
                                                                return 1;
                                                            }
                                                            ctx.getSource().getBukkitSender().sendMessage("§cPlayer not found.");
                                                            return 0;
                                                        })
                                                        .then(Commands.argument("item", StringArgumentType.word())
                                                                .executes(ctx -> {
                                                                    Player target = Bukkit.getPlayer(StringArgumentType.getString(ctx, "player"));
                                                                    String group = StringArgumentType.getString(ctx, "group");
                                                                    String item = StringArgumentType.getString(ctx, "item");

                                                                    if (target != null) {
                                                                        boolean success = rewardHandler.giveReward(target, group, item, itemRegistry);
                                                                        if (success) {
                                                                            ctx.getSource().getBukkitSender().sendMessage("§aGave reward '" + item + "' to " + target.getName());
                                                                            return 1;
                                                                        } else {
                                                                            ctx.getSource().getBukkitSender().sendMessage("§cReward item not found or not queued.");
                                                                        }
                                                                    }
                                                                    return 0;
                                                                })
                                                        )
                                                )
                                        )
                                )
                        )
        );

        // Register dynamic GUI commands
        guiBuilder.getGuiKeys().forEach(guiKey -> dispatcher.register(
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
        ));

        plugin.getLogger().info("§aRegistered /harambe command tree and GUI commands.");
    }
}
