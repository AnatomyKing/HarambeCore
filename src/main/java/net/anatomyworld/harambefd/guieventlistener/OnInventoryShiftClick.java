package net.anatomyworld.harambefd.guieventlistener;

import net.anatomyworld.harambefd.GuiBuilder;
import net.anatomyworld.harambefd.harambemethods.ItemRegistry;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class OnInventoryShiftClick implements Listener {

  private final GuiBuilder guiBuilder;
  private final ItemRegistry itemRegistry;

  public OnInventoryShiftClick(GuiBuilder guiBuilder, ItemRegistry itemRegistry) {
    this.guiBuilder = guiBuilder;
    this.itemRegistry = itemRegistry;
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void handleShiftClick(InventoryClickEvent event) {
    // Ensure the entity interacting is a player
    HumanEntity humanEntity = event.getWhoClicked();
    if (!(humanEntity instanceof Player)) {
      return;
    }
    Player player = (Player) humanEntity;

    // Check if the action is a shift-click
    if (event.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY) {
      return;
    }

    // Retrieve the top inventory and GUI key
    Inventory topInventory = event.getView().getTopInventory();
    String guiKey = guiBuilder.getGuiKeyByInventory(player, topInventory);

    if (guiKey == null) {
      return;
    }

    // Handle specific GUI restrictions
    if ("enderlink".equalsIgnoreCase(guiKey)) {
      event.setCancelled(true);
      player.sendMessage("Shift-clicking is disabled in the Enderlink GUI.");
      return;
    }

    // Validate the item being shift-clicked
    ItemStack shiftClickedItem = event.getCurrentItem();
    if (shiftClickedItem == null || shiftClickedItem.getType().isAir()) {
      return;
    }

    // Check if the item is registered
    if (!itemRegistry.isItemRegistered(shiftClickedItem)) {
      event.setCancelled(true);
      player.sendMessage("Only registered items can be shift-clicked into this GUI.");
      return;
    }

    // Handle item distribution in allowed slots
    String itemName = itemRegistry.getItemTag(shiftClickedItem);
    List<Integer> allowedSlots = guiBuilder.getAllowedSlots(guiKey, itemName);
    int maxAmountPerSlot = guiBuilder.getItemAmountForSlot(guiKey, allowedSlots.get(0));
    int maxTotalAmount = allowedSlots.size() * maxAmountPerSlot;

    int remainingAmount = ItemAmountValidator.distributeItems(
            topInventory,
            shiftClickedItem,
            allowedSlots,
            maxAmountPerSlot,
            maxTotalAmount,
            itemName,
            guiBuilder.shouldConsumeOnPlace(guiKey, allowedSlots.get(0))
    );

    // Update inventory based on remaining item amount
    if (remainingAmount <= 0) {
      event.setCurrentItem(null);
    }

    // Cancel the default shift-click behavior
    event.setCancelled(true);
  }
}
