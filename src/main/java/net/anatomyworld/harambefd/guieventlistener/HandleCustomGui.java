package net.anatomyworld.harambefd.guieventlistener;

import net.anatomyworld.harambefd.GuiBuilder;
import net.anatomyworld.harambefd.harambemethods.ItemRegistry;
import net.anatomyworld.harambefd.guieventlistener.GuiUtils;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class HandleCustomGui implements Listener {
  private final GuiBuilder guiBuilder;
  private final ItemRegistry itemRegistry;

  public HandleCustomGui(GuiBuilder guiBuilder, ItemRegistry itemRegistry) {
    this.guiBuilder = guiBuilder;
    this.itemRegistry = itemRegistry;
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void handleCustomGuiClick(InventoryClickEvent event) {
    HumanEntity entity = event.getWhoClicked();
    if (!(entity instanceof Player player)) return;

    Inventory topInventory = event.getView().getTopInventory();
    String guiKey = guiBuilder.getGuiKeyByInventory(player, topInventory);
    if (guiKey == null) return;

    Inventory clickedInventory = event.getClickedInventory();
    if (clickedInventory == null || !clickedInventory.equals(topInventory)) return;

    int slot = event.getSlot();
    Map<Integer, String> buttonKeyMap = guiBuilder.getButtonKeyMap(guiKey);

    if (buttonKeyMap != null && buttonKeyMap.containsKey(slot)) {
      String buttonKey = buttonKeyMap.get(slot);

      if ("button".equalsIgnoreCase(buttonKey) || "filler".equalsIgnoreCase(buttonKey)) {
        event.setCancelled(true);
        return;
      }

      if (GuiUtils.isSpecialSlot(buttonKey)) {
        handleSpecialSlot(event, player, guiKey, topInventory, slot, buttonKey);
      }
    }
  }

  private void handleSpecialSlot(InventoryClickEvent event, Player player, String guiKey, Inventory inventory, int slot, String buttonKey) {
    String requiredItemName = guiBuilder.getItemNameForSlot(guiKey, slot);

    ItemStack clickedItem = event.getCurrentItem();
    if (clickedItem != null && !clickedItem.getType().isAir()) {
      String itemName = itemRegistry.getItemTag(clickedItem);
      if (itemName != null && itemName.equals(requiredItemName)) {
        return;
      }
    }

    ItemStack cursorItem = event.getCursor();
    if (!GuiUtils.validateItemPlacement(player, cursorItem, requiredItemName, itemRegistry)) {
      event.setCancelled(true);
      return;
    }

    boolean consumeOnPlace = guiBuilder.shouldConsumeOnPlace(guiKey, slot);
    if (consumeOnPlace) {
      GuiUtils.consumeItems(event, player, inventory, slot, cursorItem, guiKey, guiBuilder, itemRegistry);
    }
  }
}
