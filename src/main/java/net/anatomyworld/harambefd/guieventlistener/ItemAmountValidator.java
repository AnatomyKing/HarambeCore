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
   * Distributes items into the specified slots of the inventory.
   *
   * @param inventory The inventory to distribute items into.
   * @param item      The item to distribute.
   * @param slots     The slots to distribute items into.
   * @param maxAmount The maximum amount of items allowed per slot.
   */
  public static void distributeItems(Inventory inventory, ItemStack item, List<Integer> slots, int maxAmount) {
    int remaining = item.getAmount();

    for (int slot : slots) {
      if (remaining <= 0) {
        break;
      }

      ItemStack slotItem = inventory.getItem(slot);

      if (slotItem == null || slotItem.getType().isAir()) {
        // Place items in an empty or air slot
        int toPlace = Math.min(remaining, maxAmount);

        ItemStack newItem = item.clone();
        newItem.setAmount(toPlace);
        inventory.setItem(slot, newItem);

        remaining -= toPlace;

      } else if (slotItem.isSimilar(item)) {
        // Add to existing similar item stacks
        int availableSpace = maxAmount - slotItem.getAmount();
        int toPlace = Math.min(remaining, availableSpace);

        if (toPlace > 0) {
          slotItem.setAmount(slotItem.getAmount() + toPlace);
          remaining -= toPlace;
        }
      }
    }

    item.setAmount(remaining); // Update the original item stack's remaining amount
  }
}
