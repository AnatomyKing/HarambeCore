package net.anatomyworld.harambefd.guieventlistener;

import net.anatomyworld.harambefd.GuiBuilder;
import net.anatomyworld.harambefd.Harambefd;
import net.anatomyworld.harambefd.harambemethods.ItemRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

public class OnInventoryDrag implements Listener {

  private final GuiBuilder guiBuilder;
  private final ItemRegistry itemRegistry;

  public OnInventoryDrag(GuiBuilder guiBuilder, ItemRegistry itemRegistry) {
    this.guiBuilder = guiBuilder;
    this.itemRegistry = itemRegistry;
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void handleInventoryDrag(InventoryDragEvent event) {
    // Ensure the interacting entity is a Player
    HumanEntity humanEntity = event.getWhoClicked();
    if (!(humanEntity instanceof Player player)) {
      return;
    }

      // Validate top inventory and GUI key
    Inventory topInventory = event.getView().getTopInventory();
    if (topInventory == null) {
      return;
    }
    String guiKey = guiBuilder.getGuiKeyByInventory(player, topInventory);
    if (guiKey == null) {
      return;
    }

    // Handle special GUI restrictions
    if ("enderlink".equalsIgnoreCase(guiKey)) {
      handleEnderlinkRestrictions(event, topInventory, guiKey);
      return;
    }

    // Validate dragged item
    ItemStack draggedItem = event.getOldCursor();
    if (draggedItem == null || draggedItem.getType().isAir()) {
      return;
    }

    // Ensure dragged item is registered
    if (!itemRegistry.isItemRegistered(draggedItem)) {
      cancelEventWithMessage(event, player, "Only registered items can be placed in this GUI.");
      return;
    }

    // Check slot placements for validity
    String itemName = itemRegistry.getItemTag(draggedItem);
    if (!validateSlotPlacement(event, topInventory, guiKey, itemName)) {
      cancelEventWithMessage(event, player, "You cannot place '" + itemName + "' in some of the selected slots.");
      return;
    }

    // Check for inventory capacity
    List<Integer> allowedSlots = guiBuilder.getAllowedSlots(guiKey, itemName);
    int maxAmountPerSlot = guiBuilder.getItemAmountForSlot(guiKey, allowedSlots.get(0));
    int maxTotalAmount = allowedSlots.size() * maxAmountPerSlot;
    int currentTotal = ItemAmountValidator.getTotalItemCount(topInventory, allowedSlots);

    if (currentTotal >= maxTotalAmount) {
      cancelEventWithMessage(event, player, "The inventory is already full for this item.");
      return;
    }

    // Distribute items and handle consumption
    handleItemDistribution(event, player, topInventory, guiKey, draggedItem, allowedSlots, maxAmountPerSlot, maxTotalAmount, itemName);
  }

  private void handleEnderlinkRestrictions(InventoryDragEvent event, Inventory topInventory, String guiKey) {
    Map<Integer, String> buttonKeyMap = guiBuilder.getButtonKeyMap(guiKey);
    if (buttonKeyMap != null) {
      for (int rawSlot : event.getRawSlots()) {
        if (rawSlot >= 0 && rawSlot < topInventory.getSize() && buttonKeyMap.containsKey(rawSlot)) {
          event.setCancelled(true);
          return;
        }
      }
    }
  }

  private boolean validateSlotPlacement(InventoryDragEvent event, Inventory topInventory, String guiKey, String itemName) {
    for (Map.Entry<Integer, ItemStack> entry : event.getNewItems().entrySet()) {
      int slot = entry.getKey();
      if (slot >= topInventory.getSize()) {
        return false;
      }
      String requiredItemName = guiBuilder.getItemNameForSlot(guiKey, slot);
      if (requiredItemName == null || !requiredItemName.equals(itemName)) {
        return false;
      }
    }
    return true;
  }

  private void handleItemDistribution(InventoryDragEvent event, Player player, Inventory topInventory, String guiKey,
                                      ItemStack draggedItem, List<Integer> allowedSlots, int maxAmountPerSlot, int maxTotalAmount, String itemName) {
    ItemAmountValidator.distributeItems(topInventory, draggedItem, allowedSlots, maxAmountPerSlot, maxTotalAmount, itemName,
            guiBuilder.shouldConsumeOnPlace(guiKey, allowedSlots.get(0)));

    for (Map.Entry<Integer, ItemStack> entry : event.getNewItems().entrySet()) {
      int slot = entry.getKey();
      if (guiBuilder.shouldConsumeOnPlace(guiKey, slot)) {
        topInventory.setItem(slot, null);
        player.sendMessage("Item '" + itemName + "' has been consumed in slot " + slot + ".");
      }
    }

    updateCursorItem(player, draggedItem);
    event.setCancelled(true);
  }

  private void updateCursorItem(Player player, ItemStack draggedItem) {
    Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(Harambefd.class), () -> {
      if (draggedItem.getAmount() > 0) {
        player.setItemOnCursor(new ItemStack(draggedItem));
      } else {
        player.setItemOnCursor(null);
      }
    });
  }

  private void cancelEventWithMessage(InventoryDragEvent event, Player player, String message) {
    event.setCancelled(true);
    player.sendMessage(message);
  }
}
