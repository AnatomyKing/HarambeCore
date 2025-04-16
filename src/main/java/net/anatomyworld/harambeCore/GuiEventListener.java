package net.anatomyworld.harambeCore;

import net.anatomyworld.harambeCore.GuiBuilder.SlotType;
import net.anatomyworld.harambeCore.item.ItemRegistry;
import net.anatomyworld.harambeCore.util.RecipeBookUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

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
        if (guiKey == null) return;

        int rawSlot = event.getRawSlot();
        int clickedSlot = event.getSlot();

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
            case INPUT_SLOT -> {
                // Allow interaction
            }
            case FILLER -> {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        if (event.getInventory().getType() == InventoryType.WORKBENCH) {
            RecipeBookUtils.forceCloseClientRecipeBook(player);
        }
    }
}

