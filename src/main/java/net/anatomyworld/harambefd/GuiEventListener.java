package net.anatomyworld.harambefd;

import net.anatomyworld.harambefd.harambemethods.ItemRegistry;
import net.anatomyworld.harambefd.guis.enderlink.EnderlinkMethods;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

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

        // Handle shift-clicks or number key actions
        if (event.isShiftClick() || event.getAction() == InventoryAction.HOTBAR_SWAP) {
            event.setCancelled(true);
            player.sendMessage("Shift-clicks and hotbar swaps are not allowed in this GUI.");
            return;
        }

        // Block invalid actions using the generalized method
        if (validateAndBlock(event, player, guiKey, slot)) {
            event.setCancelled(true); // Ensure the action is blocked
            Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(Harambefd.class), player::updateInventory); // Sync inventory
            return; // Stop further processing
        }

        // Additional logic for valid interactions can go here
    }

    private boolean validateAndBlock(InventoryClickEvent event, Player player, String guiKey, int slot) {
        // Get the button key for the slot
        Map<Integer, String> buttonKeyMap = guiBuilder.getButtonKeyMap(guiKey);
        String buttonKey = buttonKeyMap != null ? buttonKeyMap.get(slot) : null;

        // Check if the slot is a special slot
        if (isSpecialSlot(buttonKey)) {
            ItemStack cursorItem = event.getCursor(); // Item being placed
            ItemStack slotItem = event.getCurrentItem(); // Item currently in the slot
            String requiredItemName = guiBuilder.getItemNameForSlot(guiKey, slot);

            // Block invalid items on the cursor
            if (cursorItem != null && !cursorItem.getType().isAir() &&
                    !requiredItemName.equals(itemRegistry.getItemTag(cursorItem))) {
                player.sendMessage("You can only place '" + requiredItemName + "' in this slot.");
                return true; // Blocked
            }

            // Block invalid interactions with items in the slot
            if (slotItem != null && !slotItem.getType().isAir() &&
                    !requiredItemName.equals(itemRegistry.getItemTag(slotItem))) {
                player.sendMessage("You cannot interact with invalid items in this slot.");
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

