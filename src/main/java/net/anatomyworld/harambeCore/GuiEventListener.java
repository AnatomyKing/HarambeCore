package net.anatomyworld.harambeCore;

import net.anatomyworld.harambeCore.GuiBuilder.SlotType;
import net.anatomyworld.harambeCore.item.ItemRegistry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class GuiEventListener implements Listener {

    private final GuiBuilder guiBuilder;

    public GuiEventListener(GuiBuilder guiBuilder, ItemRegistry itemRegistry) {
        this.guiBuilder = guiBuilder;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) return;

        String guiKey = guiBuilder.getGuiKeyByInventory(player, event.getInventory());
        if (guiKey == null) return; // Not our GUI

        int rawSlot = event.getRawSlot();
        int clickedSlot = event.getSlot();

        // Allow interaction with player inventory
        if (rawSlot >= event.getInventory().getSize()) return;

        Map<Integer, SlotType> slotTypes = guiBuilder.getGuiSlotTypes().get(guiKey);
        if (slotTypes == null) return;

        SlotType slotType = slotTypes.get(clickedSlot);
        if (slotType == null) return;

        switch (slotType) {
            case BUTTON -> {
                event.setCancelled(true);
                guiBuilder.handleButtonClick(player, guiKey, clickedSlot);
            }
            case STORAGE_SLOT -> {
                // Allow storage interaction
            }
            case FILLER -> {
                event.setCancelled(true);
            }
        }
    }
}