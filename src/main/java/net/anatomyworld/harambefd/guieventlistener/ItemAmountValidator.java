package net.anatomyworld.harambefd.guieventlistener;

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
   * Distributes items into the specified slots of the inventory, adhering to slot and total limits.
   *
   * @param inventory        The inventory to distribute items into.
   * @param item             The item to distribute.
   * @param slots            The slots to distribute items into.
   * @param maxAmount        The maximum amount of items allowed per slot.
   * @param totalMaxAmount   The total maximum amount of items allowed across all slots.
   * @param requiredItemTag  A tag to validate item placement, can be null or empty for no restriction.
   * @param consumeOnPlace   Whether to reduce the source item's amount when placing items.
   * @return The remaining amount of items that couldn't be placed.
   */
  public static int distributeItems(Inventory inventory, ItemStack item, List<Integer> slots,
                                    int maxAmount, int totalMaxAmount, String requiredItemTag, boolean consumeOnPlace) {
    int remaining = item.getAmount();
    int currentTotal = getTotalItemCount(inventory, slots);

    // Early return if the total amount already exceeds or meets the limit
    if (currentTotal >= totalMaxAmount) {
      return remaining;
    }

    for (int slot : slots) {
      if (remaining <= 0) {
        break;
      }

      ItemStack slotItem = inventory.getItem(slot);

      // Validate item tag if provided
      if (requiredItemTag != null && !requiredItemTag.isEmpty() &&
              slotItem != null && !requiredItemTag.equals(slotItem.getType().name())) {
        continue;
      }

      if (slotItem == null || slotItem.getType().isAir()) {
        // Place items in an empty or air slot
        int toPlace = Math.min(remaining, Math.min(maxAmount, totalMaxAmount - currentTotal));

        if (toPlace > 0) {
          ItemStack newItem = item.clone();
          newItem.setAmount(toPlace);
          inventory.setItem(slot, newItem);

          remaining -= toPlace;
          currentTotal += toPlace;

          if (consumeOnPlace) {
            item.setAmount(item.getAmount() - toPlace);
          }
        }
      } else if (slotItem.isSimilar(item)) {
        // Add to existing similar item stacks
        int availableSpace = maxAmount - slotItem.getAmount();
        int toPlace = Math.min(remaining, Math.min(availableSpace, totalMaxAmount - currentTotal));

        if (toPlace > 0) {
          slotItem.setAmount(slotItem.getAmount() + toPlace);

          remaining -= toPlace;
          currentTotal += toPlace;

          if (consumeOnPlace) {
            item.setAmount(item.getAmount() - toPlace);
          }
        }
      }
    }

    item.setAmount(remaining); // Update the original item stack's remaining amount
    return remaining;
  }
}
