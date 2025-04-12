package net.anatomyworld.harambeCore.item;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class RewardHandler {

    public static void handleSubmitAction(Player player, String buttonKey, double payAmount, Map<Integer, ItemStack> consumedItems) {
        // Check if payment was already validated in GuiEventListener
        if (payAmount > 0) {
            player.sendMessage("Payment of " + payAmount + " has already been validated for '" + buttonKey + "'.");
        }

        // Ensure required items are placed
        if (consumedItems == null || consumedItems.isEmpty()) {
            player.sendMessage("You must place the required items for " + buttonKey + " to proceed.");
            return;
        }

        // Consume items and reward the player
        consumeItems(consumedItems);
        giveReward(player, Material.EMERALD, "Thank you! You have received an Emerald for clicking '" + buttonKey + "'.");
    }

    public static void handleSlotReward(Player player, String buttonKey, Material rewardMaterial, double payAmount, String message) {
        // Check if payment was already validated in GuiEventListener
        if (payAmount > 0) {
            player.sendMessage("Payment of " + payAmount + " has already been validated for '" + buttonKey + "'.");
        }

        giveReward(player, rewardMaterial, message != null ? message : "You have received a reward for clicking '" + buttonKey + "'!");
    }

    private static void consumeItems(Map<Integer, ItemStack> itemsToConsume) {
        for (Map.Entry<Integer, ItemStack> entry : itemsToConsume.entrySet()) {
            ItemStack item = entry.getValue();
            if (item != null) {
                item.setAmount(0); // Clear the item stack
            }
        }
    }

    public static void giveReward(Player player, Material material, String message) {
        if (material != null) {
            ItemStack reward = new ItemStack(material);
            player.getInventory().addItem(reward);
        }
        if (message != null && !message.isEmpty()) {
            player.sendMessage(message);
        }
    }
}
