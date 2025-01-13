package net.anatomyworld.harambefd.guieventlistener;

import net.anatomyworld.harambefd.harambemethods.ItemRegistry;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ItemAmountValidator {

  /**
   * Calculates the total amount of items in the specified slots of the inventory.
   *
   * @param inventory The inventory to check.
   * @param slots     The slots to calculate the total item count for.
   * @return The total number of items in the specified slots.
   */
  public static int getTotalItemCount(Inventory inventory, List<Integer> slots) {
    int total = 0;
    for (int slot : slots) {
      ItemStack item = inventory.getItem(slot);
      if (item != null && !item.getType().isAir()) {
        total += item.getAmount();
      }
    }
    return total;
  }

  /**
   * Distributes items into the specified slots of the inventory, respecting:
   *  - A per-slot maxAmount
   *  - The requiredItemName (if the slot already has items).
   */
  public static void distributeItems(Inventory inventory, ItemStack incomingItem, List<Integer> slots,
                                     int maxAmount, String requiredItemName, ItemRegistry itemRegistry) {
    if (incomingItem == null || incomingItem.getType().isAir()) return;

    int remaining = incomingItem.getAmount();

    for (int slot : slots) {
      if (remaining <= 0) break;

      ItemStack slotItem = inventory.getItem(slot);

      // Slot is empty or air
      if (slotItem == null || slotItem.getType().isAir()) {
        int toPlace = Math.min(remaining, maxAmount);

        // Clone incoming item and place 'toPlace' amount
        ItemStack newItem = incomingItem.clone();
        newItem.setAmount(toPlace);
        inventory.setItem(slot, newItem);

        remaining -= toPlace;
      }
      // Slot already has an item -> check if it matches required item name
      else {
        // Compare the item name in this slot to requiredItemName
        String slotItemName = itemRegistry.getItemTag(slotItem);
        if (requiredItemName.equals(slotItemName)) {
          int availableSpace = maxAmount - slotItem.getAmount();
          int toPlace = Math.min(remaining, availableSpace);

          if (toPlace > 0) {
            slotItem.setAmount(slotItem.getAmount() + toPlace);
            remaining -= toPlace;
          }
        }
      }
    }

    // Update the original stack's amount to whatever couldn't be placed
    incomingItem.setAmount(remaining);
  }

  /**
   * Distributes items into the specified slots of the inventory, treating them as a group limit.
   * Respects requiredItemName so only stacks of the same name are topped up.
   */
  public static void distributeItemsGrouped(Inventory inventory, ItemStack incomingItem, List<Integer> slots,
                                            int maxGroupTotal, String requiredItemName, ItemRegistry itemRegistry) {
    if (incomingItem == null || incomingItem.getType().isAir()) return;

    int remaining = incomingItem.getAmount();
    int currentTotal = getTotalItemCount(inventory, slots);

    // If group is at or beyond capacity, adjust remaining
    if (currentTotal + remaining > maxGroupTotal) {
      remaining = maxGroupTotal - currentTotal;
    }
    if (remaining <= 0) {
      incomingItem.setAmount(remaining);
      return;
    }

    for (int slot : slots) {
      if (remaining <= 0) break;

      ItemStack slotItem = inventory.getItem(slot);

      // If slot is empty or air
      if (slotItem == null || slotItem.getType().isAir()) {
        int toPlace = Math.min(remaining, maxGroupTotal);

        ItemStack newItem = incomingItem.clone();
        newItem.setAmount(toPlace);
        inventory.setItem(slot, newItem);

        remaining -= toPlace;
      }
      // If slot is occupied, ensure it's the required item name
      else {
        String slotItemName = itemRegistry.getItemTag(slotItem);
        if (requiredItemName.equals(slotItemName)) {
          int availableSpace = maxGroupTotal - slotItem.getAmount();
          int toPlace = Math.min(remaining, availableSpace);

          if (toPlace > 0) {
            slotItem.setAmount(slotItem.getAmount() + toPlace);
            remaining -= toPlace;
          }
        }
      }
    }

    // Update original item
    incomingItem.setAmount(remaining);
  }
}
