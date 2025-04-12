package net.anatomyworld.harambeCore;

import net.anatomyworld.harambeCore.harambemethods.ItemRegistry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GuiEventListener implements Listener {

    private final GuiBuilder guiBuilder;

    public GuiEventListener(GuiBuilder guiBuilder, ItemRegistry itemRegistry) {
        this.guiBuilder = guiBuilder;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String guiKey = guiBuilder.getGuiKeyByInventory(player, event.getInventory());
        if (guiKey == null) return;

        event.setCancelled(true);
        int slot = event.getRawSlot();
        guiBuilder.handleButtonClick(player, guiKey, slot);
    }
}
