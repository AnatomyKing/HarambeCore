package net.anatomyworld.harambefd.guieventlistener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.anatomyworld.harambefd.EconomyHandler;
import net.anatomyworld.harambefd.GuiBuilder;
import net.anatomyworld.harambefd.harambemethods.ItemRegistry;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class HandleCustomGui implements Listener {
  private final GuiBuilder guiBuilder;
  private final ItemRegistry itemRegistry;

  public HandleCustomGui(GuiBuilder guiBuilder, ItemRegistry itemRegistry) {
    this.guiBuilder = guiBuilder;
    this.itemRegistry = itemRegistry;
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void handleCustomGuiClick(InventoryClickEvent event) {
    Player player;
    HumanEntity humanEntity = event.getWhoClicked();
    if (humanEntity instanceof Player) {
      player = (Player) humanEntity;
    } else {
      return;
    }

    Inventory topInventory = event.getView().getTopInventory();
    String guiKey = this.guiBuilder.getGuiKeyByInventory(player, topInventory);
    if (guiKey == null)
      return;

    Inventory clickedInventory = event.getClickedInventory();
    if (clickedInventory == null || !clickedInventory.equals(topInventory))
      return;

    int slot = event.getSlot();
    Map<Integer, String> buttonKeyMap = this.guiBuilder.getButtonKeyMap(guiKey);
    if (buttonKeyMap == null || !buttonKeyMap.containsKey(slot)) {
      event.setCancelled(true);
      return;
    }

    String buttonKey = buttonKeyMap.get(slot);
    if ("enderlink".equalsIgnoreCase(guiKey)) {
      handleEnderlinkSlot(event, player, guiKey, topInventory, slot, buttonKey);
      return;
    }

    // Removed 'slot' parameter from the method call
    handleStandardSlot(event, player, guiKey, topInventory, buttonKey);
  }

  private void handleEnderlinkSlot(InventoryClickEvent event, Player player, String guiKey, Inventory topInventory, int slot, String buttonKey) {
    if (isSpecialSlot(buttonKey)) {
      String requiredItemName = this.guiBuilder.getItemNameForSlot(guiKey, slot);
      if (requiredItemName == null || requiredItemName.isEmpty()) {
        event.setCancelled(true);
        player.sendMessage("There is no defined item for this slot.");
        return;
      }

      ItemStack cursorItem = event.getCursor();
      if (cursorItem != null && !cursorItem.getType().isAir()) {
        if (!this.itemRegistry.isItemRegistered(cursorItem)) {
          event.setCancelled(true);
          player.sendMessage("Only registered items can be placed in this slot!");
          return;
        }

        String cursorItemName = this.itemRegistry.getItemTag(cursorItem);
        if (!requiredItemName.equals(cursorItemName)) {
          event.setCancelled(true);
          player.sendMessage("You can only place the item '" + requiredItemName + "' in this slot.");
          return;
        }

        boolean consumeOnPlace = this.guiBuilder.shouldConsumeOnPlace(guiKey, slot);
        if (consumeOnPlace)
          consumeItems(event, player, topInventory, slot, cursorItem, guiKey);
      }
    }
  }

  // Removed 'slot' parameter from this method
  private void handleStandardSlot(InventoryClickEvent event, Player player, String guiKey, Inventory topInventory, String buttonKey) {
    event.setCancelled(true);
    ItemStack clickedItem = event.getCurrentItem();
    if (clickedItem != null && !clickedItem.getType().isAir()) {
      List<Integer> slotsToProcess = this.guiBuilder.getSlotsForButton(guiKey, buttonKey);
      Map<Integer, ItemStack> itemsToConsume = new HashMap<>();
      for (int slotToProcess : slotsToProcess) {
        ItemStack itemInSlot = topInventory.getItem(slotToProcess);
        if (itemInSlot != null && !itemInSlot.getType().isAir()) {
          double payAmount = this.guiBuilder.getSlotPayMap(guiKey).getOrDefault(slotToProcess, 0.0D);
          if (payAmount > 0.0D) {
            if (!EconomyHandler.hasEnoughBalance(player, payAmount)) {
              player.sendMessage("You don't have enough balance for this action.");
              continue;
            }
            EconomyHandler.withdrawBalance(player, payAmount);
          }
          itemsToConsume.put(slotToProcess, itemInSlot.clone());
          topInventory.setItem(slotToProcess, null);
        }
      }
      this.guiBuilder.handleButtonClick(player, guiKey, buttonKey, itemsToConsume);
    }
  }

  private void consumeItems(InventoryClickEvent event, Player player, Inventory inventory, int slot, ItemStack cursorItem, String guiKey) {
    int maxAmount = this.guiBuilder.getItemAmountForSlot(guiKey, slot);
    int currentTotal = ItemAmountValidator.getTotalItemCount(inventory, List.of(slot));
    if (currentTotal >= maxAmount) {
      event.setCancelled(true);
      player.sendMessage("This slot cannot hold more items.");
      return;
    }
    double payAmount = this.guiBuilder.getSlotPayMap(guiKey).getOrDefault(slot, 0.0D);
    if (payAmount > 0.0D) {
      if (!EconomyHandler.hasEnoughBalance(player, payAmount)) {
        player.sendMessage("You don't have enough balance to perform this action.");
        event.setCancelled(true);
        return;
      }
      EconomyHandler.withdrawBalance(player, payAmount);
    }
    cursorItem.setAmount(0);
    inventory.setItem(slot, null);
    player.sendMessage("Item '" + this.itemRegistry.getItemTag(cursorItem) + "' has been consumed.");
  }

  private boolean isSpecialSlot(String buttonKey) {
    return buttonKey.endsWith("_slot");
  }
}

