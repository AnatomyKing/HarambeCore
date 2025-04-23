package net.anatomyworld.harambeCore;

import net.anatomyworld.harambeCore.GuiBuilder.SlotType;
import net.anatomyworld.harambeCore.item.ItemRegistry;
import net.anatomyworld.harambeCore.util.EconomyHandler;
import net.anatomyworld.harambeCore.util.RecipeBookUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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

        Map<Integer, String> acceptedItems = guiBuilder.getAcceptedItems(guiKey);
        Map<Integer, Integer> acceptedAmounts = guiBuilder.getAcceptedAmounts(guiKey);
        Map<Integer, Double> ecoCosts = guiBuilder.getSlotCosts(guiKey);
        Map<Integer, Boolean> perStackFlags = guiBuilder.getCostPerStack(guiKey);
        Map<Integer, GuiBuilder.InputActionType> inputActions = guiBuilder.getInputActions(guiKey);
        Map<Integer, List<Integer>> slotConnections = guiBuilder.getSlotConnections(guiKey);

        switch (slotType) {
            case BUTTON -> {
                double cost = ecoCosts.getOrDefault(clickedSlot, 0.0);
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
                boolean isLinkedToCheck = slotConnections.values().stream()
                        .anyMatch(list -> list.contains(clickedSlot));

                GuiBuilder.InputActionType action = inputActions.getOrDefault(clickedSlot, GuiBuilder.InputActionType.NONE);
                ItemStack cursor = event.getCursor();

                if (acceptedItems.containsKey(clickedSlot) && cursor != null && !cursor.getType().isAir()) {
                    String expectedMaterial = acceptedItems.get(clickedSlot);
                    Material mat = Material.matchMaterial(expectedMaterial);
                    if (mat == null || !cursor.getType().equals(mat)) {
                        event.setCancelled(true);
                        player.sendMessage("§cOnly " + expectedMaterial + " is allowed in this slot.");
                        return;
                    }

                    if (acceptedAmounts.containsKey(clickedSlot)) {
                        int required = acceptedAmounts.get(clickedSlot);
                        if (cursor.getAmount() != required) {
                            event.setCancelled(true);
                            player.sendMessage("§cYou must place exactly " + required + " items.");
                            return;
                        }
                    }
                }

                if (action == GuiBuilder.InputActionType.CONSUME && !isLinkedToCheck) {
                    double costPerUnit = ecoCosts.getOrDefault(clickedSlot, 0.0);
                    boolean perStack = perStackFlags.getOrDefault(clickedSlot, false);
                    int amount = cursor != null ? cursor.getAmount() : 1;
                    double totalCost = perStack ? (amount * costPerUnit) : costPerUnit;

                    if (!EconomyHandler.hasEnoughBalance(player, totalCost)) {
                        event.setCancelled(true);
                        player.sendMessage("§cYou need " + totalCost + " to place that item.");
                        return;
                    }

                    if (!EconomyHandler.withdrawBalance(player, totalCost)) {
                        event.setCancelled(true);
                        player.sendMessage("§cPayment failed.");
                        return;
                    }

                    if (cursor != null && amount > 0) {
                        int toConsume = perStack ? amount : 1;
                        int remaining = cursor.getAmount() - toConsume;

                        ItemStack newCursor = (remaining > 0) ? new ItemStack(cursor.getType(), remaining) : null;
                        Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(HarambeCore.class),
                                () -> player.setItemOnCursor(newCursor));

                        player.sendMessage("§eConsumed " + toConsume + " " +
                                cursor.getType().name().toLowerCase(Locale.ROOT).replace("_", " ") + ".");
                    }

                    event.setCancelled(true);
                }
            }

            case CHECK_BUTTON -> {
                double cost = ecoCosts.getOrDefault(clickedSlot, 0.0);
                event.setCancelled(true);
                if (cost > 0 && !EconomyHandler.hasEnoughBalance(player, cost)) {
                    player.sendMessage("§cYou need " + cost + " to use this.");
                    return;
                }
                if (cost > 0 && !EconomyHandler.withdrawBalance(player, cost)) {
                    player.sendMessage("§cPayment failed.");
                    return;
                }

                String requiredMaterial = guiBuilder.getCheckItems(guiKey).get(clickedSlot);
                List<Integer> checkSlots = slotConnections.get(clickedSlot);

                boolean match = checkSlots != null && !checkSlots.isEmpty() && checkSlots.stream().allMatch(slot -> {
                    ItemStack item = event.getInventory().getItem(slot);
                    return item != null && item.getType().name().equalsIgnoreCase(requiredMaterial);
                });

                if (match) {
                    for (int slot : checkSlots) {
                        GuiBuilder.InputActionType action = inputActions.getOrDefault(slot, GuiBuilder.InputActionType.NONE);
                        if (action == GuiBuilder.InputActionType.CONSUME) {
                            ItemStack item = event.getInventory().getItem(slot);
                            if (item != null) {
                                int newAmount = item.getAmount() - 1;
                                if (newAmount > 0) {
                                    item.setAmount(newAmount);
                                } else {
                                    event.getInventory().setItem(slot, null);
                                }
                            }
                        }
                    }

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
