package net.anatomyworld.harambefd;

import net.anatomyworld.harambefd.harambemethods.ItemRegistry;
import net.anatomyworld.harambefd.guis.enderlink.EnderlinkMethods;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class GuiEventListener implements Listener {

    private final GuiBuilder guiBuilder;
    private final ItemRegistry itemRegistry;

    public GuiEventListener(GuiBuilder guiBuilder, ItemRegistry itemRegistry) {
        this.guiBuilder = guiBuilder;
        this.itemRegistry = itemRegistry;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Get the inventories involved
        Inventory topInventory = event.getView().getTopInventory();
        Inventory clickedInventory = event.getClickedInventory();

        // Ensure the click is in the top inventory (custom GUI)
        if (clickedInventory == null || !clickedInventory.equals(topInventory)) return;

        // Check if the inventory is a custom GUI
        String guiKey = guiBuilder.getGuiKeyByInventory(player, topInventory);
        if (guiKey == null) return;

        // Get the slot that was clicked
        int slot = event.getSlot();

        // Block invalid actions using the generalized method
        if (validateAndBlock(event, player, guiKey, slot)) {
            return; // Stop further processing if the action is blocked
        }

        // Additional logic for valid interactions can go here
    }

    private boolean validateAndBlock(InventoryClickEvent event, Player player, String guiKey, int slot) {
        // Get the button key for the slot
        Map<Integer, String> buttonKeyMap = guiBuilder.getButtonKeyMap(guiKey);
        String buttonKey = buttonKeyMap != null ? buttonKeyMap.get(slot) : null;

        // Check if the slot is a special slot
        if (isSpecialSlot(buttonKey)) {
            // Get the item on the cursor
            ItemStack cursorItem = event.getCursor();


            // Validate the item against the required name for the slot
            String requiredItemName = guiBuilder.getItemNameForSlot(guiKey, slot);
            if (!requiredItemName.equals(itemRegistry.getItemTag(cursorItem))) {
                event.setCancelled(true);
                player.sendMessage("You can only place '" + requiredItemName + "' in this slot.");
                player.updateInventory(); // Sync client inventory state
                return true; // Blocked
            }
        }

        return false; // Not blocked
    }

    // Utility method to check if a slot is special
    private boolean isSpecialSlot(String buttonKey) {
        return buttonKey != null && buttonKey.endsWith("_slot");
    }


    /**
     * ###############  KEY FIX  ###############
     * Final step to save Enderlink items whenever the GUI closes.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Inventory topInventory = event.getView().getTopInventory();

        String guiKey = guiBuilder.getGuiKeyByInventory(player, topInventory);
        if (guiKey == null) return;

        if ("enderlink".equalsIgnoreCase(guiKey)) {
            EnderlinkMethods.saveCurrentPage(player, guiBuilder);
        }
    }
}

