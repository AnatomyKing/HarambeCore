package net.anatomyworld.harambefd.guieventlistener;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ItemAmountValidator {

  /**
   * Calculates the total number of items present in the specified slots of the inventory.
   *
   * @param inventory The inventory to scan.
   * @param slots     The list of slot indices to check.
   * @return The total count of items in the specified slots.
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
   * Distributes items across the specified slots in the inventory, respecting the maximum allowed amount.
   *
   * @param inventory       The inventory where the items will be distributed.
   * @param item            The item to distribute.
   * @param slots           The list of slots where the items can be placed.
   * @param maxAmount       The maximum number of items allowed per slot.
   * @param totalMaxAmount  The cumulative maximum number of items across all slots.
   * @param requiredItemTag The required item tag to validate item placement.
   * @param consumeOnPlace  Whether items should be consumed during placement.
   * @return The remaining amount of the item after distribution.
   */
  public static int distributeItems(
          Inventory inventory,
          ItemStack item,
          List<Integer> slots,
          int maxAmount,
          int totalMaxAmount,
          String requiredItemTag,
          boolean consumeOnPlace
  ) {
    int remaining = item.getAmount();

    // Check the total current item count in all slots
    int currentTotal = getTotalItemCount(inventory, slots);
    if (currentTotal >= totalMaxAmount) {
      return remaining; // Inventory is already full
    }

    for (int slot : slots) {
      if (remaining <= 0) break;

      ItemStack slotItem = inventory.getItem(slot);

      // Validate required item tag
      if (requiredItemTag != null && !requiredItemTag.isEmpty()) {
        if (slotItem != null && !requiredItemTag.equals(slotItem.getType().name())) {
          continue; // Skip invalid items
        }
      }

      // If slot is empty or has valid items
      if (slotItem == null || slotItem.getType().isAir()) {
        int toPlace = Math.min(remaining, maxAmount);
        int cumulativeSpace = totalMaxAmount - currentTotal;

        if (cumulativeSpace <= 0) break; // No space left in the overall slots
        toPlace = Math.min(toPlace, cumulativeSpace);

        ItemStack newItem = item.clone();
        newItem.setAmount(toPlace);
        inventory.setItem(slot, newItem);
        remaining -= toPlace;
        currentTotal += toPlace;

        if (consumeOnPlace) {
          item.setAmount(item.getAmount() - toPlace);
        }

      } else if (slotItem.isSimilar(item)) {
        int availableSpace = maxAmount - slotItem.getAmount();
        int cumulativeSpace = totalMaxAmount - currentTotal;

        if (availableSpace > 0 && cumulativeSpace > 0) {
          int toPlace = Math.min(remaining, Math.min(availableSpace, cumulativeSpace));
          slotItem.setAmount(slotItem.getAmount() + toPlace);
          remaining -= toPlace;
          currentTotal += toPlace;

          if (consumeOnPlace) {
            item.setAmount(item.getAmount() - toPlace);
          }
        }
      }
    }

    item.setAmount(remaining);
    return remaining;
  }
}
