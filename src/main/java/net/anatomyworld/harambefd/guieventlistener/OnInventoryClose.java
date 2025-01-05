package net.anatomyworld.harambefd.guieventlistener;

import net.anatomyworld.harambefd.GuiBuilder;
import net.anatomyworld.harambefd.guis.enderlink.EnderlinkMethods;
import net.anatomyworld.harambefd.harambemethods.ItemRegistry;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

public class OnInventoryClose implements Listener {
  private final GuiBuilder guiBuilder;
  
  private final ItemRegistry itemRegistry;
  
  public OnInventoryClose(GuiBuilder guiBuilder, ItemRegistry itemRegistry) {
    this.guiBuilder = guiBuilder;
    this.itemRegistry = itemRegistry;
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onInventoryClose(InventoryCloseEvent event) {
    if (!(event.getPlayer() instanceof Player player)) return;
    Inventory topInventory = event.getView().getTopInventory();

    String guiKey = guiBuilder.getGuiKeyByInventory(player, topInventory);
    if (guiKey == null) return;

    if ("enderlink".equalsIgnoreCase(guiKey)) {
      EnderlinkMethods.saveCurrentPage(player, guiBuilder);
    }
  }
}
