package net.anatomyworld.harambefd;

import net.anatomyworld.harambefd.guis.Krobus.KrobusGui;
import net.anatomyworld.harambefd.guis.Spawn.SpawnGui;
import net.anatomyworld.harambefd.guis.anygui.InterfaceGui;
import net.anatomyworld.harambefd.guis.enderlink.EnderlinkGui;
import net.anatomyworld.harambefd.harambemethods.ItemRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandHandler implements CommandExecutor, TabCompleter {

    private final Harambefd plugin;
    private final GuiBuilder guiBuilder;
    private final ItemRegistry itemRegistry;

    public CommandHandler(Harambefd plugin, GuiBuilder guiBuilder, ItemRegistry itemRegistry) {
        this.plugin = plugin;
        this.guiBuilder = guiBuilder;
        this.itemRegistry = itemRegistry;
    }

    public void registerCommands() {
        plugin.getCommand("harambe").setExecutor(this);
        plugin.getCommand("harambe").setTabCompleter(this); // Register tab completer
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("harambe")) {
            if (args.length == 0) {
                player.sendMessage("Usage: /harambe <GUI_NAME|set|reload|give>");
                return true;
            }

            // Check if the first argument is a GUI name
            switch (args[0].toLowerCase()) {
                case "krobus":
                    new KrobusGui(guiBuilder).open(player);
                    return true;
                case "spawn":
                    new SpawnGui(guiBuilder).open(player);
                    return true;
                case "enderlink":
                    new EnderlinkGui(guiBuilder).open(player);
                    return true;
                case "interface":
                    new InterfaceGui(guiBuilder).open(player);
                    return true;
                default:
                    // Handle other harambe commands (set, reload, give)
                    return handleHarambeCommand(player, args);
            }
        }

        return false;
    }

    private boolean handleHarambeCommand(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage("Usage: /harambe <set|reload|give>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "set":
                return handleSetCommand(player, args);
            case "reload":
                plugin.reloadConfig();
                guiBuilder.updateConfig(plugin.getConfig());
                player.sendMessage("Configuration reloaded.");
                return true;
            case "give":
                return handleGiveCommand(player, args);
            default:
                player.sendMessage("Unknown command.");
                return true;
        }
    }

    private boolean handleSetCommand(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("Usage: /harambe set <item|reward> <item_name> [reward_name]");
            return true;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType() == Material.AIR) {
            player.sendMessage("You must hold an item to set it.");
            return true;
        }

        String itemName = args[2].toLowerCase();

        switch (args[1].toLowerCase()) {
            case "item":
                itemRegistry.registerItem(itemName, itemInHand);
                player.sendMessage("Submit item '" + itemName + "' has been set and registered.");
                player.getInventory().setItemInMainHand(null);
                return true;

            case "reward":
                if (args.length != 4) {
                    player.sendMessage("Usage: /harambe set reward <submit_item_name> <reward_name>");
                    return true;
                }

                String rewardName = args[3].toLowerCase();
                itemRegistry.registerReward(itemName, rewardName, itemInHand);
                player.sendMessage("Reward item '" + rewardName + "' has been set for submit item '" + itemName + "'.");
                player.getInventory().setItemInMainHand(null);
                return true;

            default:
                player.sendMessage("Invalid sub-command. Use: /harambe set <item|reward> <name>");
                return true;
        }
    }

    private boolean handleGiveCommand(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage("Usage: /harambe give <item|reward> <player> <submit_item_name> [reward_name] [amount]");
            return true;
        }

        String type = args[1].toLowerCase();
        Player targetPlayer = Bukkit.getPlayer(args[2]);

        if (targetPlayer == null) {
            player.sendMessage("Player not found!");
            return true;
        }

        String submitItemTag = args[3].toLowerCase();
        int amount = 1; // Default amount

        switch (type) {
            case "item":
                // For /harambe give item <player> <itemname> [amount]
                if (args.length >= 5) {
                    try {
                        amount = Integer.parseInt(args[4]);
                    } catch (NumberFormatException e) {
                        player.sendMessage("Invalid amount, must be a number.");
                        return true;
                    }
                }

                ItemStack item = itemRegistry.generateItem(submitItemTag);
                if (item != null) {
                    item.setAmount(amount);
                    targetPlayer.getInventory().addItem(item);
                    player.sendMessage("Gave item '" + submitItemTag + "' to " + targetPlayer.getName());
                    targetPlayer.sendMessage("You have been given item '" + submitItemTag + "'!");
                } else {
                    player.sendMessage("Item '" + submitItemTag + "' not found.");
                }
                return true;

            case "reward":
                // For /harambe give reward <player> <itemname> <rewardname> [amount]
                if (args.length < 5) {
                    player.sendMessage("Usage: /harambe give reward <player> <submit_item_name> <reward_name> [amount]");
                    return true;
                }

                String rewardName = args[4].toLowerCase();

                if (args.length >= 6) {
                    try {
                        amount = Integer.parseInt(args[5]);
                    } catch (NumberFormatException e) {
                        player.sendMessage("Invalid amount, must be a number.");
                        return true;
                    }
                }

                ItemStack rewardItem = itemRegistry.loadRewardItem(submitItemTag, rewardName);
                if (rewardItem != null) {
                    rewardItem.setAmount(amount);
                    targetPlayer.getInventory().addItem(rewardItem);
                    player.sendMessage("Gave reward '" + rewardName + "' to " + targetPlayer.getName());
                    targetPlayer.sendMessage("You have been given reward '" + rewardName + "'!");
                } else {
                    player.sendMessage("Reward '" + rewardName + "' not found for submit item '" + submitItemTag + "'.");
                }
                return true;

            default:
                player.sendMessage("Invalid sub-command. Use: /harambe give <item|reward> <player> <submit_item_name> [reward_name] [amount]");
                return true;
        }
    }

    // Tab completion implementation
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("harambe")) {
            if (args.length == 1) {
                completions.addAll(Arrays.asList(
                        "set", "reload", "give", "krobus", "spawn", "interface", "enderlink"
                ));
            } else if (args.length == 2) {
                String firstArg = args[0].toLowerCase();
                if (firstArg.equals("set")) {
                    completions.addAll(Arrays.asList("item", "reward"));
                } else if (firstArg.equals("give")) {
                    completions.addAll(Arrays.asList("item", "reward"));
                }
            } else if (args.length >= 3) {
                String firstArg = args[0].toLowerCase();
                String secondArg = args[1].toLowerCase();

                if (firstArg.equals("set")) {
                    if (secondArg.equals("item") && args.length == 3) {
                        completions.add("<item_name>");
                    } else if (secondArg.equals("reward") && args.length == 4) {
                        completions.add("<reward_name>");
                    }
                } else if (firstArg.equals("give")) {
                    if (secondArg.equals("item")) {
                        if (args.length == 3) {
                            completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                        } else if (args.length == 4) {
                            completions.add("<submit_item_name>");
                        } else if (args.length == 5) {
                            completions.add("<amount>");
                        }
                    } else if (secondArg.equals("reward")) {
                        if (args.length == 3) {
                            completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                        } else if (args.length == 4) {
                            completions.add("<submit_item_name>");
                        } else if (args.length == 5) {
                            completions.add("<reward_name>");
                        } else if (args.length == 6) {
                            completions.add("<amount>");
                        }
                    }
                }
            }
        }

        return filterCompletions(completions, args[args.length - 1]);
    }

    private List<String> filterCompletions(List<String> completions, String currentArg) {
        return completions.stream()
                .filter(c -> c.toLowerCase().startsWith(currentArg.toLowerCase()))
                .collect(Collectors.toList());
    }
}