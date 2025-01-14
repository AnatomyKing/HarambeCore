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
        Inventory topInventory = event.getView().getTopInventory(); // Custom GUI
        Inventory clickedInventory = event.getClickedInventory();   // Source inventory

        // Ensure the click is in the player's inventory or the custom GUI
        if (clickedInventory == null) return;

        // Check if the inventory is a custom GUI
        String guiKey = guiBuilder.getGuiKeyByInventory(player, topInventory);
        if (guiKey == null) return; // Not a custom GUI

        // Handle shift-clicks moving items into the custom GUI
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && clickedInventory != topInventory) {
            // Allow shift-clicks in the "enderlink" GUI
            if ("enderlink".equalsIgnoreCase(guiKey)) {
                return; // Let enderlink handle shift-clicks without blocking
            }

            // Block or handle shift-click for other GUIs
            if (handleShiftClick(event, player, guiKey, topInventory)) {
                event.setCancelled(true); // Cancel the event if blocked
                Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(Harambefd.class), player::updateInventory); // Sync inventory
                return;
            }
        }

        // Ensure the click is in the topInventory (custom GUI)
        if (clickedInventory != topInventory) return;

        // Handle direct placement
        if (handleDirectPlacement(event, player, guiKey, topInventory)) {
            event.setCancelled(true); // Cancel the event if blocked
            Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(Harambefd.class), player::updateInventory); // Sync inventory
        }
    }

    private boolean handleShiftClick(InventoryClickEvent event, Player player, String guiKey, Inventory topInventory) {
        ItemStack shiftClickedItem = event.getCurrentItem(); // Item being shift-clicked
        if (shiftClickedItem == null || shiftClickedItem.getType().isAir()) return false;

        // Retrieve item name (tag) for validation
        String itemName = itemRegistry.getItemTag(shiftClickedItem);

        // Block items without `item_name` from being shift-clicked
        if (!itemRegistry.isItemRegistered(shiftClickedItem)) {
            player.sendMessage("Only registered items with `item_name` can be shift-clicked into this GUI.");
            return true; // Blocked
        }

        // Get allowed slots for registered items in the custom GUI
        List<Integer> allowedSlots = guiBuilder.getAllowedSlots(guiKey, itemName);
        if (allowedSlots.isEmpty()) {
            player.sendMessage("No slot in this GUI accepts that item.");
            return true; // Blocked
        }

        // Distribute registered items into allowed slots
        int remaining = distributeItems(topInventory, shiftClickedItem, allowedSlots);
        if (remaining > 0) {
            shiftClickedItem.setAmount(remaining); // Update the remaining stack
        } else {
            event.setCurrentItem(null); // Clear the original slot if all items are placed
        }

        return true; // Block default shift-click behavior
    }

    private boolean handleDirectPlacement(InventoryClickEvent event, Player player, String guiKey, Inventory topInventory) {
        InventoryAction action = event.getAction();
        ItemStack cursorItem = event.getCursor();      // Item on the cursor
        int slot = event.getSlot();

        // Ensure the clicked slot belongs to the topInventory (custom GUI)
        if (!topInventory.equals(event.getClickedInventory())) return false;

        // Get the button key for the slot
        Map<Integer, String> buttonKeyMap = guiBuilder.getButtonKeyMap(guiKey);
        String buttonKey = buttonKeyMap != null ? buttonKeyMap.get(slot) : null;

        // Only validate special slots
        if (isSpecialSlot(buttonKey)) {
            if (cursorItem == null || cursorItem.getType().isAir()) return false;

            String requiredItemName = guiBuilder.getItemNameForSlot(guiKey, slot);
            if (!requiredItemName.equals(itemRegistry.getItemTag(cursorItem))) {
                player.sendMessage("You can only place '" + requiredItemName + "' in this slot.");
                return true; // Blocked
            }

            // Optionally log the action for debugging
            switch (action) {
                case PLACE_ALL -> player.sendMessage("Placed the entire stack.");
                case PLACE_ONE -> player.sendMessage("Placed one item.");
                case PLACE_SOME -> player.sendMessage("Placed part of the stack.");
                case SWAP_WITH_CURSOR -> player.sendMessage("Swapped with the cursor item.");
                default -> {}
            }
        }

        // Allow normal slots and non-special slots
        return false;
    }

    private int distributeItems(Inventory inventory, ItemStack item, List<Integer> allowedSlots) {
        int remaining = item.getAmount();

        for (int slot : allowedSlots) {
            if (remaining <= 0) break;

            ItemStack slotItem = inventory.getItem(slot);
            int maxStackSize = item.getMaxStackSize();

            if (slotItem == null || slotItem.getType().isAir()) {
                int toPlace = Math.min(remaining, maxStackSize);
                ItemStack newItem = item.clone();
                newItem.setAmount(toPlace);
                inventory.setItem(slot, newItem);
                remaining -= toPlace;
            } else if (slotItem.isSimilar(item)) {
                int spaceAvailable = maxStackSize - slotItem.getAmount();
                int toPlace = Math.min(remaining, spaceAvailable);
                slotItem.setAmount(slotItem.getAmount() + toPlace);
                remaining -= toPlace;
            }
        }

        return remaining; // Return any leftover items
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory topInventory = event.getView().getTopInventory(); // Custom GUI

        // Check if the inventory is a custom GUI
        String guiKey = guiBuilder.getGuiKeyByInventory(player, topInventory);
        if (guiKey == null) return; // Not a custom GUI

        // Get the dragged item
        ItemStack draggedItem = event.getCursor();
        if (draggedItem == null || draggedItem.getType().isAir()) return;

        String itemName = itemRegistry.getItemTag(draggedItem);

        // Validate each slot in the drag event
        for (int slot : event.getRawSlots()) {
            // Only handle slots in the custom GUI
            if (slot >= topInventory.getSize()) continue;

            // Get the button key for the slot
            Map<Integer, String> buttonKeyMap = guiBuilder.getButtonKeyMap(guiKey);
            String buttonKey = buttonKeyMap != null ? buttonKeyMap.get(slot) : null;

            // Only validate special slots
            if (isSpecialSlot(buttonKey)) {
                String requiredItemName = guiBuilder.getItemNameForSlot(guiKey, slot);

                if (!itemRegistry.isItemRegistered(draggedItem) || !requiredItemName.equals(itemName)) {
                    player.sendMessage("You can only drag '" + requiredItemName + "' into this slot.");
                    event.setCancelled(true); // Block the drag
                    Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(Harambefd.class), player::updateInventory); // Sync inventory
                    return;
                }
            }
        }
    }

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

