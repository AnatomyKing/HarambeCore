package net.anatomyworld.harambefd.guis.anygui;

import net.anatomyworld.harambefd.RewardHandler;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class InterfaceMethods {

    public static void handleButtonClick(Player player, String buttonKey, Map<Integer, ItemStack> consumedItems, Map<Integer, Double> payMap) {
        switch (buttonKey) {
            case "submit_button":
                double submitPay = payMap.getOrDefault(22, 0.0); // Slot 22 is the submit_button
                RewardHandler.handleSubmitAction(player, buttonKey, submitPay, consumedItems);
                break;

            case "diamond_button":
                double diamondPay = payMap.getOrDefault(0, 0.0); // Slot 0 is part of diamond_button
                RewardHandler.handleSlotReward(player, buttonKey, Material.DIAMOND, diamondPay, "You received a diamond for clicking '" + buttonKey + "'!");
                break;

            case "fish_button":
                player.performCommand("lfish catalog");
                player.sendMessage("Opening the Fish Catalog triggered by '" + buttonKey + "'.");
                break;

            case "sucker_slot":
            case "checker_slot":
            case "submit_slot":
                player.sendMessage("This slot (" + buttonKey + ") is for item placement; no actions are triggered.");
                break;

            default:
                player.sendMessage("Unknown button (" + buttonKey + ") clicked.");
                break;
        }
    }
}
