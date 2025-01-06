package net.anatomyworld.harambefd.guieventlistener;

import net.anatomyworld.harambefd.EconomyHandler;
import net.anatomyworld.harambefd.GuiBuilder;
import net.anatomyworld.harambefd.harambemethods.ItemRegistry;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class GuiUtils {


    /**
     * Handles consuming items in a slot.
     *
     * @param event         The InventoryClickEvent to process.
     * @param player        The player interacting with the GUI.
     * @param inventory     The inventory being interacted with.
     * @param slot          The slot index in the inventory.
     * @param itemStack     The item being interacted with.
     * @param guiKey        The key for the current GUI.
     * @param guiBuilder    The GuiBuilder instance.
     * @param itemRegistry  The ItemRegistry instance.
     */
    public static void consumeItems(
            InventoryClickEvent event,
            Player player,
            Inventory inventory,
            int slot,
            ItemStack itemStack,
            String guiKey,
            GuiBuilder guiBuilder,
            ItemRegistry itemRegistry
    ) {
        // Check if the item is registered
        if (!itemRegistry.isItemRegistered(itemStack)) {
            player.sendMessage("This item is not registered and cannot be consumed.");
            event.setCancelled(true);
            return;
        }

        // Retrieve item tag for validation
        String itemTag = itemRegistry.getItemTag(itemStack);
        if (itemTag == null || itemTag.isEmpty()) {
            player.sendMessage("This item has no valid tag and cannot be consumed.");
            event.setCancelled(true);
            return;
        }

        // Get slot limits
        int maxAmount = guiBuilder.getItemAmountForSlot(guiKey, slot);
        int currentTotal = ItemAmountValidator.getTotalItemCount(inventory, List.of(slot));

        if (currentTotal >= maxAmount) {
            event.setCancelled(true);
            player.sendMessage("This slot cannot hold more items.");
            return;
        }

        // Handle economy payments
        double payAmount = guiBuilder.getSlotPayMap(guiKey).getOrDefault(slot, 0.0D);
        if (!handleEconomy(player, payAmount)) {
            event.setCancelled(true);
            return;
        }

        // Determine how much to consume
        int toConsume = Math.min(itemStack.getAmount(), maxAmount - currentTotal);
        currentTotal += toConsume;

        // Update item amount and clear slot if consumed fully
        itemStack.setAmount(itemStack.getAmount() - toConsume);
        if (itemStack.getAmount() == 0) {
            inventory.setItem(slot, null);
        }

        player.sendMessage("Consumed " + toConsume + " items of type: " + itemTag + ". Current total: " + currentTotal + "/" + maxAmount);
        event.setCancelled(true);
    }

    /**
     * Handles economy payments.
     *
     * @param player    The player interacting with the GUI.
     * @param payAmount The amount to be deducted from the player's balance.
     * @return True if the player had enough balance and the transaction succeeded, false otherwise.
     */
    private static boolean handleEconomy(Player player, double payAmount) {
        if (payAmount <= 0.0D) return true;

        if (!EconomyHandler.hasEnoughBalance(player, payAmount)) {
            player.sendMessage("You don't have enough balance to perform this action.");
            return false;
        }

        EconomyHandler.withdrawBalance(player, payAmount);
        player.sendMessage("Paid " + payAmount + " for this action.");
        return true;
    }

    public static boolean isSpecialSlot(String buttonKey) {
        return buttonKey.endsWith("_slot");
    }

    /**
     * Validates whether an item can be placed in the slot.
     *
     * @param player       The player interacting with the GUI.
     * @param cursorItem   The item on the player's cursor.
     * @param requiredName The required item name for the slot.
     * @param itemRegistry The ItemRegistry instance.
     * @return True if the item is valid for the slot, false otherwise.
     */
    public static boolean validateItemPlacement(Player player, ItemStack cursorItem, String requiredName, ItemRegistry itemRegistry) {
        if (cursorItem == null || !itemRegistry.isItemRegistered(cursorItem)) {
            player.sendMessage("Only registered items can be placed in this slot!");
            return false;
        }

        String cursorItemName = itemRegistry.getItemTag(cursorItem);
        if (!requiredName.equals(cursorItemName)) {
            player.sendMessage("You can only place the item '" + requiredName + "' in this slot.");
            return false;
        }

        return true;
    }
}
