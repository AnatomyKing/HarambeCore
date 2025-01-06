package net.anatomyworld.harambefd;

import net.anatomyworld.harambefd.harambemethods.ItemRegistry;
import net.anatomyworld.harambefd.guis.enderlink.EnderlinkMethods;
import net.anatomyworld.harambefd.guieventlistener.ItemAmountValidator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        Inventory topInventory = event.getView().getTopInventory();
        Inventory clickedInventory = event.getClickedInventory();

        String guiKey = guiBuilder.getGuiKeyByInventory(player, topInventory);
        if (guiKey == null) return; // Not one of our GUIs

        // ============ ENDERLINK LOGIC ============
        if ("enderlink".equalsIgnoreCase(guiKey)) {
            // Only handle clicks in the top inventory
            if (clickedInventory == null || !clickedInventory.equals(topInventory)) return;

            int slot = event.getSlot();
            Map<Integer, String> buttonKeyMap = guiBuilder.getButtonKeyMap(guiKey);
            if (buttonKeyMap != null && buttonKeyMap.containsKey(slot)) {
                // It's a button/filler slot => can't move or remove that item
                event.setCancelled(true);

                // If it's actually a button (not filler), handle the click
                String buttonKey = buttonKeyMap.get(slot);
                if (!buttonKey.endsWith("_slot")) {
                    guiBuilder.handleButtonClick(player, guiKey, buttonKey, new HashMap<>());
                }
            }
            // Otherwise it's a normal storage slot => allow normal movement
            return;
        }

        // =========== NON-ENDERLINK GUIs ===========
        // SHIFT-CLICK
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            ItemStack shiftClickedItem = event.getCurrentItem();
            if (shiftClickedItem != null) {
                if (!itemRegistry.isItemRegistered(shiftClickedItem)) {
                    event.setCancelled(true);
                    player.sendMessage("Only registered items can be shift-clicked into this GUI.");
                    return;
                }

                String itemName = itemRegistry.getItemTag(shiftClickedItem);
                List<Integer> allowedSlots = guiBuilder.getAllowedSlots(guiKey, itemName);
                int maxAmountPerSlot = guiBuilder.getItemAmountForSlot(guiKey, allowedSlots.get(0)); // Assuming uniform maxAmount

                // Validate the total item count in allowed slots before distributing
                int currentTotal = ItemAmountValidator.getTotalItemCount(topInventory, allowedSlots);
                int maxTotal = allowedSlots.size() * maxAmountPerSlot;

                if (currentTotal >= maxTotal) {
                    player.sendMessage("The inventory is already full for this item.");
                    event.setCancelled(true);
                    return;
                }

                // Use ItemAmountValidator to distribute items
                ItemAmountValidator.distributeItems(topInventory, shiftClickedItem, allowedSlots, maxAmountPerSlot);

                // Clear the item from the cursor if fully distributed
                if (shiftClickedItem.getAmount() <= 0) {
                    event.setCurrentItem(null);
                }

                event.setCancelled(true);
                return;
            }
        }

        // DIRECT PLACEMENTS
        if (clickedInventory != null && clickedInventory.equals(topInventory)) {
            switch (event.getAction()) {
                case PLACE_ONE, PLACE_SOME, PLACE_ALL, SWAP_WITH_CURSOR -> {
                    ItemStack cursorItem = event.getCursor();
                    int slot = event.getSlot();

                    // Check if the cursor item is registered
                    if (cursorItem != null && !itemRegistry.isItemRegistered(cursorItem)) {
                        event.setCancelled(true);
                        player.sendMessage("Only registered items can be placed here.");
                        return;
                    }

                    // Get the max allowed amount for this slot
                    int maxAmount = guiBuilder.getItemAmountForSlot(guiKey, slot);
                    List<Integer> singleSlot = List.of(slot);
                    ItemAmountValidator.distributeItems(topInventory, cursorItem, singleSlot, maxAmount);

                    // Update the cursor with the remaining items
                    int remainingAmount = cursorItem.getAmount();
                    Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(Harambefd.class), () -> {
                        if (remainingAmount > 0) {
                            cursorItem.setAmount(remainingAmount);
                        } else {
                            player.setItemOnCursor(null); // Clear the cursor
                        }
                    });

                    event.setCancelled(true);
                }
                default -> {
                }
            }

            // Prevent usage of number keys
            if (event.getClick() == ClickType.NUMBER_KEY) {
                event.setCancelled(true);
                return;
            }

            handleCustomGuiClick(event, player, guiKey);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory topInventory = event.getView().getTopInventory();
        if (topInventory == null) return;

        String guiKey = guiBuilder.getGuiKeyByInventory(player, topInventory);
        if (guiKey == null) return;

        // Get button/filler slots for the GUI, if any
        Map<Integer, String> buttonKeyMap = guiBuilder.getButtonKeyMap(guiKey);

        Map<Integer, ItemStack> slotItems = event.getNewItems();

        for (Map.Entry<Integer, ItemStack> entry : slotItems.entrySet()) {
            int slot = entry.getKey();
            ItemStack draggedItem = entry.getValue();

            // Ensure the slot is within bounds
            if (slot >= topInventory.getSize()) {
                event.setCancelled(true);
                return;
            }

            // Cancel if dragging over a button or filler slot
            if (buttonKeyMap != null && buttonKeyMap.containsKey(slot)) {
                event.setCancelled(true);
                return;
            }

            // Validate dragged item
            if (draggedItem == null || !itemRegistry.isItemRegistered(draggedItem)) {
                event.setCancelled(true);
                player.sendMessage("Only registered items can be placed in this GUI.");
                return;
            }

            String itemName = itemRegistry.getItemTag(draggedItem);
            String requiredItemName = guiBuilder.getItemNameForSlot(guiKey, slot);

            // Check if the item matches the required name
            if (requiredItemName == null || !requiredItemName.equals(itemName)) {
                event.setCancelled(true);
                player.sendMessage("You cannot place '" + itemName + "' in slot " + slot + ".");
                return;
            }

            boolean consumeOnPlace = guiBuilder.shouldConsumeOnPlace(guiKey, slot);
            if (consumeOnPlace) {
                topInventory.setItem(slot, null);
                player.sendMessage("Item '" + itemName + "' has been consumed in slot " + slot + ".");
            } else {
                topInventory.setItem(slot, draggedItem);
            }
        }

        event.setCancelled(true);
    }

    private void handleCustomGuiClick(InventoryClickEvent event, Player player, String guiKey) {
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();


        Map<Integer, String> buttonKeyMap = guiBuilder.getButtonKeyMap(guiKey);
        if (buttonKeyMap == null) {
            event.setCancelled(true);
            return;
        }

        int slot = event.getSlot();
        if (!buttonKeyMap.containsKey(slot)) {
            event.setCancelled(true);
            return;
        }

        String buttonKey = buttonKeyMap.get(slot);

        if (isSpecialSlot(buttonKey)) {
            String requiredItemName = guiBuilder.getItemNameForSlot(guiKey, slot);

            if (requiredItemName == null || requiredItemName.isEmpty()) {
                event.setCancelled(true);
                player.sendMessage("There is no defined item for this slot.");
                return;
            }

            if (cursorItem != null && !cursorItem.getType().isAir()) {
                if (!itemRegistry.isItemRegistered(cursorItem)) {
                    event.setCancelled(true);
                    player.sendMessage("Only registered items can be placed in this slot!");
                    return;
                }

                String cursorItemName = itemRegistry.getItemTag(cursorItem);
                if (!requiredItemName.equals(cursorItemName)) {
                    event.setCancelled(true);
                    player.sendMessage("You can only place the item '" + requiredItemName + "' in this slot.");
                    return;
                }

                boolean consumeOnPlace = guiBuilder.shouldConsumeOnPlace(guiKey, slot);
                if (consumeOnPlace) {
                    double payAmount = guiBuilder.getSlotPayMap(guiKey).getOrDefault(slot, 0.0);

                    if (payAmount > 0) {
                        if (!EconomyHandler.hasEnoughBalance(player, payAmount)) {
                            player.sendMessage("You don't have enough balance to perform this action.");
                            event.setCancelled(true);
                            return;
                        }
                        EconomyHandler.withdrawBalance(player, payAmount);
                    }

                    cursorItem.setAmount(0);
                    player.getOpenInventory().getTopInventory().setItem(slot, null);
                    player.sendMessage("Item '" + cursorItemName + "' has been consumed from the slot.");
                }
            }
            return;
        }

        event.setCancelled(true);
        if (clickedItem != null && !clickedItem.getType().isAir()) {
            List<Integer> slotsToProcess = guiBuilder.getSlotsForButton(guiKey, buttonKey);
            Map<Integer, ItemStack> itemsToConsume = new HashMap<>();
            Inventory topInventory = player.getOpenInventory().getTopInventory();

            for (int slotToProcess : slotsToProcess) {
                ItemStack itemInSlot = topInventory.getItem(slotToProcess);
                if (itemInSlot != null && !itemInSlot.getType().isAir()) {
                    double payAmount = guiBuilder.getSlotPayMap(guiKey).getOrDefault(slotToProcess, 0.0);

                    if (payAmount > 0) {
                        if (!EconomyHandler.hasEnoughBalance(player, payAmount)) {
                            player.sendMessage("You don't have enough balance for this action.");
                            continue; // Skip consumption if balance is insufficient
                        }
                        EconomyHandler.withdrawBalance(player, payAmount);
                    }

                    itemsToConsume.put(slotToProcess, itemInSlot.clone());
                    topInventory.setItem(slotToProcess, null);
                }
            }

            guiBuilder.handleButtonClick(player, guiKey, buttonKey, itemsToConsume);
        }
    }

    private boolean isSpecialSlot(String buttonKey) {
        return buttonKey.endsWith("_slot");
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

