package net.anatomyworld.harambeCore;

import net.anatomyworld.harambeCore.GuiBuilder.SlotType;
import net.anatomyworld.harambeCore.item.ItemRegistry;
import net.anatomyworld.harambeCore.util.EconomyHandler;
import net.anatomyworld.harambeCore.util.RecipeBookUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;
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

        double cost = guiBuilder.getSlotCosts(guiKey).getOrDefault(clickedSlot, 0.0);

        switch (slotType) {
            case BUTTON -> {
                event.setCancelled(true);
                if (cost > 0 && !EconomyHandler.hasEnoughBalance(player, cost)) {
                    player.sendMessage("§cYou need " + cost + " to click this.");
                    return;
                }
                if (cost > 0 && !EconomyHandler.withdrawBalance(player, cost)) {
                    player.sendMessage("§cPayment failed.");
                    return;
                }
                guiBuilder.handleButtonClick(player, guiKey, clickedSlot);
            }

            case INPUT_SLOT -> {
                if (cost > 0 && !EconomyHandler.hasEnoughBalance(player, cost)) {
                    event.setCancelled(true);
                    player.sendMessage("§cYou need " + cost + " to place an item here.");
                    return;
                }

                if (cost > 0 && !EconomyHandler.withdrawBalance(player, cost)) {
                    event.setCancelled(true);
                    player.sendMessage("§cPayment failed.");
                    return;
                }

                Map<Integer, String> acceptedItems = guiBuilder.getAcceptedItems(guiKey);
                String acceptedItem = acceptedItems.get(clickedSlot);

                if (acceptedItem != null && !acceptedItem.isEmpty()) {
                    ItemStack cursorItem = event.getCursor();
                    if (cursorItem == null || !cursorItem.getType().name().equalsIgnoreCase(acceptedItem)) {
                        event.setCancelled(true);
                        player.sendMessage("§cOnly " + acceptedItem + " is allowed in this slot.");
                        return;
                    }
                }

                Map<Integer, GuiBuilder.InputActionType> inputActions = guiBuilder.getInputActions(guiKey);
                GuiBuilder.InputActionType action = inputActions.getOrDefault(clickedSlot, GuiBuilder.InputActionType.NONE);

                if (action == GuiBuilder.InputActionType.CONSUME) {
                    ItemStack cursorItem = event.getCursor();
                    if (cursorItem != null && cursorItem.getAmount() > 0) {
                        cursorItem.setAmount(cursorItem.getAmount() - 1);
                        ItemStack newCursor = cursorItem.getAmount() > 0 ? cursorItem : null;

                        Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(HarambeCore.class), () -> player.setItemOnCursor(newCursor));
                        player.sendMessage("§eConsumed one " + cursorItem.getType().name().toLowerCase(Locale.ROOT).replace("_", " ") + ".");
                    }

                    event.setCancelled(true);
                }
            }

            case CHECK_BUTTON -> {
                event.setCancelled(true);
                if (cost > 0 && !EconomyHandler.hasEnoughBalance(player, cost)) {
                    player.sendMessage("§cYou need " + cost + " to use this.");
                    return;
                }
                if (cost > 0 && !EconomyHandler.withdrawBalance(player, cost)) {
                    player.sendMessage("§cPayment failed.");
                    return;
                }

                Map<Integer, String> checkItems = guiBuilder.getCheckItems(guiKey);
                Map<Integer, List<Integer>> slotConnections = guiBuilder.getSlotConnections(guiKey);

                String requiredMaterial = checkItems.get(clickedSlot);
                List<Integer> checkSlots = slotConnections.get(clickedSlot);

                boolean match = false;
                if (requiredMaterial != null && checkSlots != null && !checkSlots.isEmpty()) {
                    match = checkSlots.stream().allMatch(slot -> {
                        ItemStack item = event.getInventory().getItem(slot);
                        return item != null && item.getType().name().equalsIgnoreCase(requiredMaterial);
                    });
                }

                if (match) {
                    guiBuilder.handleButtonClick(player, guiKey, clickedSlot);
                } else {
                    player.sendMessage("§cCheck failed. Required item not found in input slots.");
                }
            }

            case FILLER -> event.setCancelled(true);
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
