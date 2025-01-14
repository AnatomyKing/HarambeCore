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

        Inventory topInventory = event.getView().getTopInventory(); // Custom GUI
        Inventory clickedInventory = event.getClickedInventory();   // Source inventory

        if (clickedInventory == null) return;

        String guiKey = guiBuilder.getGuiKeyByInventory(player, topInventory);
        if (guiKey == null) return; // Not a custom GUI

        // Handle shift-clicks moving items into the custom GUI
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && clickedInventory != topInventory) {
            if ("enderlink".equalsIgnoreCase(guiKey)) return; // Allow shift-clicks in "enderlink" GUI

            if (handleShiftClick(event, player, guiKey, topInventory)) {
                event.setCancelled(true); // Cancel the event if blocked
                syncInventory(player);
            }
            return;
        }

        // Ensure the click is in the topInventory (custom GUI)
        if (clickedInventory != topInventory) return;

        // Handle direct placement
        if (handleDirectPlacement(event, player, guiKey, topInventory)) {
            event.setCancelled(true); // Cancel the event if blocked
            syncInventory(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory topInventory = event.getView().getTopInventory(); // Custom GUI
        String guiKey = guiBuilder.getGuiKeyByInventory(player, topInventory);
        if (guiKey == null) return; // Not a custom GUI

        ItemStack draggedItem = event.getOldCursor();
        if (draggedItem == null || draggedItem.getType().isAir()) return;

        String itemName = itemRegistry.getItemTag(draggedItem);

        for (int slot : event.getRawSlots()) {
            if (slot >= topInventory.getSize()) continue;

            Map<Integer, String> buttonKeyMap = guiBuilder.getButtonKeyMap(guiKey);
            String buttonKey = buttonKeyMap != null ? buttonKeyMap.get(slot) : null;

            if (isSpecialSlot(buttonKey)) {
                String requiredItemName = guiBuilder.getItemNameForSlot(guiKey, slot);

                if (!itemRegistry.isItemRegistered(draggedItem) || !requiredItemName.equals(itemName)) {
                    player.sendMessage("You can only drag '" + requiredItemName + "' into this slot.");
                    event.setCancelled(true); // Block the drag
                    returnDraggedItemToCursor(player, draggedItem);
                    return;
                }
            }
        }
    }

    private boolean handleShiftClick(InventoryClickEvent event, Player player, String guiKey, Inventory topInventory) {
        ItemStack shiftClickedItem = event.getCurrentItem();
        if (shiftClickedItem == null || shiftClickedItem.getType().isAir()) return false;

        String itemName = itemRegistry.getItemTag(shiftClickedItem);

        if (!itemRegistry.isItemRegistered(shiftClickedItem)) {
            player.sendMessage("Only registered items with `item_name` can be shift-clicked into this GUI.");
            return true; // Blocked
        }

        List<Integer> allowedSlots = guiBuilder.getAllowedSlots(guiKey, itemName);
        if (allowedSlots.isEmpty()) {
            player.sendMessage("No slot in this GUI accepts that item.");
            return true; // Blocked
        }

        int remaining = distributeItems(topInventory, shiftClickedItem, allowedSlots);
        if (remaining > 0) {
            shiftClickedItem.setAmount(remaining);
        } else {
            event.setCurrentItem(null);
        }

        return true; // Block default shift-click behavior
    }

    private boolean handleDirectPlacement(InventoryClickEvent event, Player player, String guiKey, Inventory topInventory) {
        ItemStack cursorItem = event.getCursor();
        int slot = event.getSlot();

        if (!topInventory.equals(event.getClickedInventory())) return false;

        Map<Integer, String> buttonKeyMap = guiBuilder.getButtonKeyMap(guiKey);
        String buttonKey = buttonKeyMap != null ? buttonKeyMap.get(slot) : null;

        if (isSpecialSlot(buttonKey)) {
            if (cursorItem == null || cursorItem.getType().isAir()) return false;

            String requiredItemName = guiBuilder.getItemNameForSlot(guiKey, slot);
            if (!requiredItemName.equals(itemRegistry.getItemTag(cursorItem))) {
                player.sendMessage("You can only place '" + requiredItemName + "' in this slot.");
                returnDraggedItemToCursor(player, cursorItem);
                return true; // Blocked
            }
        }

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

    private void returnDraggedItemToCursor(Player player, ItemStack item) {
        Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(Harambefd.class), () -> {
            player.setItemOnCursor(item);
            player.updateInventory();
        });
    }

    private void syncInventory(Player player) {
        Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(Harambefd.class), player::updateInventory);
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

