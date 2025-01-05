package net.anatomyworld.harambefd.guieventlistener;

import net.anatomyworld.harambefd.EconomyHandler;
import net.anatomyworld.harambefd.GuiBuilder;
import net.anatomyworld.harambefd.harambemethods.ItemRegistry;
import net.anatomyworld.harambefd.guieventlistener.ItemAmountValidator;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class GuiUtils {

    private GuiUtils() {
        // Utility class; prevent instantiation.
    }

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
        int maxAmount = guiBuilder.getItemAmountForSlot(guiKey, slot);
        int currentTotal = ItemAmountValidator.getTotalItemCount(inventory, List.of(slot));

        if (currentTotal >= maxAmount) {
            event.setCancelled(true);
            player.sendMessage("This slot cannot hold more items.");
            return;
        }

        double payAmount = guiBuilder.getSlotPayMap(guiKey).getOrDefault(slot, 0.0D);
        if (payAmount > 0.0D) {
            if (!EconomyHandler.hasEnoughBalance(player, payAmount)) {
                player.sendMessage("You don't have enough balance to perform this action.");
                event.setCancelled(true);
                return;
            }
            EconomyHandler.withdrawBalance(player, payAmount);
        }

        // Consume the item and clear the slot.
        itemStack.setAmount(0);
        inventory.setItem(slot, null);
        player.sendMessage("Item '" + itemRegistry.getItemTag(itemStack) + "' has been consumed.");
    }

    /**
     * Determines if a slot is a special slot based on its button key.
     *
     * @param buttonKey The button key to check.
     * @return True if the slot is special, false otherwise.
     */
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
