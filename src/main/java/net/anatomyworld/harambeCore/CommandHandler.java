package net.anatomyworld.harambeCore;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.anatomyworld.harambeCore.harambemethods.ItemRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Player;

public class CommandHandler {

    private final HarambeCore plugin;
    private final GuiBuilder guiBuilder;
    private final ItemRegistry itemRegistry;

    public CommandHandler(HarambeCore plugin, GuiBuilder guiBuilder, ItemRegistry itemRegistry) {
        this.plugin = plugin;
        this.guiBuilder = guiBuilder;
        this.itemRegistry = itemRegistry;
    }

    public void registerCommands() {
        CommandDispatcher<CommandSourceStack> dispatcher = ((CraftServer) Bukkit.getServer())
                .getServer()
                .getCommands()
                .getDispatcher();

        // Register /harambe command tree
        dispatcher.register(
                Commands.literal("harambe")
                        .then(Commands.literal("reload")
                                .executes(ctx -> {
                                    plugin.reloadPlugin();
                                    ctx.getSource().getBukkitSender().sendMessage("§aConfiguration reloaded.");
                                    return 1;
                                })
                        )
                        .then(Commands.literal("set")
                                .then(Commands.literal("item")
                                        .then(Commands.argument("item_name", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    Player player = (Player) ctx.getSource().getBukkitSender();
                                                    var item = player.getInventory().getItemInMainHand();
                                                    if (item.getType().isAir()) {
                                                        player.sendMessage("§cHold an item to set it.");
                                                        return 0;
                                                    }

                                                    String name = StringArgumentType.getString(ctx, "item_name").toLowerCase();
                                                    itemRegistry.registerItem(name, item);
                                                    player.getInventory().setItemInMainHand(null);
                                                    player.sendMessage("§aItem '" + name + "' has been set.");
                                                    return 1;
                                                })
                                        )
                                )
                                .then(Commands.literal("reward")
                                        .then(Commands.argument("item_name", StringArgumentType.word())
                                                .then(Commands.argument("reward_name", StringArgumentType.word())
                                                        .executes(ctx -> {
                                                            Player player = (Player) ctx.getSource().getBukkitSender();
                                                            var item = player.getInventory().getItemInMainHand();
                                                            if (item.getType().isAir()) {
                                                                player.sendMessage("§cHold an item to set it.");
                                                                return 0;
                                                            }

                                                            String itemName = StringArgumentType.getString(ctx, "item_name").toLowerCase();
                                                            String rewardName = StringArgumentType.getString(ctx, "reward_name").toLowerCase();
                                                            itemRegistry.registerReward(itemName, rewardName, item);
                                                            player.getInventory().setItemInMainHand(null);
                                                            player.sendMessage("§aReward '" + rewardName + "' set for '" + itemName + "'.");
                                                            return 1;
                                                        })
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("give")
                                .then(Commands.literal("item")
                                        .then(Commands.argument("target", StringArgumentType.word())
                                                .then(Commands.argument("item_name", StringArgumentType.word())
                                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                                .executes(ctx -> {
                                                                    Player sender = (Player) ctx.getSource().getBukkitSender();
                                                                    Player target = Bukkit.getPlayer(StringArgumentType.getString(ctx, "target"));
                                                                    String itemName = StringArgumentType.getString(ctx, "item_name").toLowerCase();
                                                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");

                                                                    if (target == null) {
                                                                        sender.sendMessage("§cTarget player not found.");
                                                                        return 0;
                                                                    }

                                                                    var item = itemRegistry.generateItem(itemName);
                                                                    if (item != null) {
                                                                        item.setAmount(amount);
                                                                        target.getInventory().addItem(item);
                                                                        sender.sendMessage("§aGave " + amount + "x '" + itemName + "' to " + target.getName());
                                                                        return 1;
                                                                    } else {
                                                                        sender.sendMessage("§cItem not found: " + itemName);
                                                                        return 0;
                                                                    }
                                                                })
                                                        )
                                                )
                                        )
                                )
                        )
        );

        // ✅ Register dynamic GUI commands
        guiBuilder.getGuiKeys().forEach(guiKey -> {
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
            );
        });

        plugin.getLogger().info("§aRegistered /harambe command tree and GUI commands.");
    }
}
