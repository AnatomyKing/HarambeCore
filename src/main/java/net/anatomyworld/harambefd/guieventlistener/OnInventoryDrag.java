package net.anatomyworld.harambefd.guieventlistener;

import net.anatomyworld.harambefd.GuiBuilder;
import net.anatomyworld.harambefd.Harambefd;
import net.anatomyworld.harambefd.harambemethods.ItemRegistry;
import net.anatomyworld.harambefd.guieventlistener.ItemAmountValidator;
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
    HumanEntity entity = event.getWhoClicked();
    if (!(entity instanceof Player player)) {
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


    // Retrieve the dragged item
    ItemStack draggedItem = event.getOldCursor();
    if (draggedItem == null || draggedItem.getType().isAir()) {
      return;
    }

    // Distribute items into allowed slots
    handleItemDistribution(event, player, topInventory, guiKey, draggedItem);
  }



  private void handleItemDistribution(InventoryDragEvent event, Player player, Inventory topInventory, String guiKey,
                                      ItemStack draggedItem) {
    // Retrieve constraints for the dragged item's placement
    String itemName = itemRegistry.getItemTag(draggedItem);
    List<Integer> allowedSlots = guiBuilder.getAllowedSlots(guiKey, itemName);
    int maxAmountPerSlot = guiBuilder.getItemAmountForSlot(guiKey, allowedSlots.get(0));
    int maxTotalAmount = allowedSlots.size() * maxAmountPerSlot;

    // Distribute items into the inventory
    int remainingAmount = ItemAmountValidator.distributeItems(
            topInventory, draggedItem, allowedSlots, maxAmountPerSlot, maxTotalAmount, itemName,
            guiBuilder.shouldConsumeOnPlace(guiKey, allowedSlots.get(0))
    );

    // Update player's cursor with any remaining items
    updateCursorItem(player, draggedItem, remainingAmount);
    event.setCancelled(true);
  }

  private void updateCursorItem(Player player, ItemStack draggedItem, int remainingAmount) {
    Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(Harambefd.class), () -> {
      if (remainingAmount > 0) {
        draggedItem.setAmount(remainingAmount);
        player.setItemOnCursor(draggedItem);
      } else {
        player.setItemOnCursor(null);
      }
    });
  }
}
