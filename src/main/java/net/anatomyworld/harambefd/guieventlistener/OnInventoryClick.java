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
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OnInventoryClick implements Listener {

  private final GuiBuilder guiBuilder;
  private final ItemRegistry itemRegistry;

  public OnInventoryClick(GuiBuilder guiBuilder, ItemRegistry itemRegistry) {
    this.guiBuilder = guiBuilder;
    this.itemRegistry = itemRegistry;
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void handleInventoryClick(InventoryClickEvent event) {
    // Ensure the entity interacting is a player
    HumanEntity humanEntity = event.getWhoClicked();
    if (!(humanEntity instanceof Player player)) {
      return;
    }

    // Validate top inventory and GUI key
    Inventory topInventory = event.getView().getTopInventory();
    String guiKey = guiBuilder.getGuiKeyByInventory(player, topInventory);
    if (guiKey == null) {
      return;
    }

    // Handle "enderlink" GUI behavior
    if ("enderlink".equalsIgnoreCase(guiKey)) {
      handleEnderlinkClick(event, player, topInventory, guiKey);
      return;
    }

    // Handle standard GUI item placement
    handleItemPlacement(event, player, topInventory, guiKey);
  }

  private void handleEnderlinkClick(InventoryClickEvent event, Player player, Inventory topInventory, String guiKey) {
    Inventory clickedInventory = event.getClickedInventory();
    if (clickedInventory == null || !clickedInventory.equals(topInventory)) {
      return;
    }

    int slot = event.getSlot();
    Map<Integer, String> buttonKeyMap = guiBuilder.getButtonKeyMap(guiKey);

    if (buttonKeyMap != null && buttonKeyMap.containsKey(slot)) {
      event.setCancelled(true);
      String buttonKey = buttonKeyMap.get(slot);

      if (!buttonKey.endsWith("_slot")) {
        guiBuilder.handleButtonClick(player, guiKey, buttonKey, new HashMap<>());
      }
    }
  }

  private void handleItemPlacement(InventoryClickEvent event, Player player, Inventory topInventory, String guiKey) {
    Inventory clickedInventory = event.getClickedInventory();
    if (clickedInventory == null || !clickedInventory.equals(topInventory)) {
      return;
    }

    InventoryAction action = event.getAction();
    if (!isPlaceAction(action)) {
      return;
    }

    ItemStack cursorItem = event.getCursor();
    int slot = event.getSlot();

    // Validate the item being place

    // Check item placement validity
    String requiredItemName = itemRegistry.getItemTag(cursorItem);
    List<Integer> allowedSlots = guiBuilder.getAllowedSlots(guiKey, requiredItemName);
    int maxAmountPerSlot = guiBuilder.getItemAmountForSlot(guiKey, slot);
    int maxTotalAmount = allowedSlots.size() * maxAmountPerSlot;

    int remainingAmount = ItemAmountValidator.distributeItems(
            topInventory, cursorItem, allowedSlots, maxAmountPerSlot, maxTotalAmount, requiredItemName,
            guiBuilder.shouldConsumeOnPlace(guiKey, slot)
    );

    // Update player's cursor item and finalize the event
    updateCursorItem(player, cursorItem, remainingAmount);
    event.setCancelled(true);
  }

  private boolean isPlaceAction(InventoryAction action) {
    return action == InventoryAction.PLACE_ONE ||
            action == InventoryAction.PLACE_SOME ||
            action == InventoryAction.PLACE_ALL ||
            action == InventoryAction.SWAP_WITH_CURSOR;
  }

  private void updateCursorItem(Player player, ItemStack cursorItem, int remainingAmount) {
    Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(Harambefd.class), () -> {
      if (remainingAmount > 0) {
        cursorItem.setAmount(remainingAmount);
        player.setItemOnCursor(cursorItem);
      } else {
        player.setItemOnCursor(null);
      }
    });
  }
}
