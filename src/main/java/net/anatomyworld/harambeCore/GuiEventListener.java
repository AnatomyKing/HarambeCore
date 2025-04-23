package net.anatomyworld.harambeCore;

import net.anatomyworld.harambeCore.GuiBuilder.InputActionType;
import net.anatomyworld.harambeCore.GuiBuilder.SlotType;
import net.anatomyworld.harambeCore.item.ItemRegistry;
import net.anatomyworld.harambeCore.item.RewardHandler;
import net.anatomyworld.harambeCore.item.RewardGroupManager;
import net.anatomyworld.harambeCore.util.EconomyHandler;
import net.anatomyworld.harambeCore.util.RecipeBookUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class GuiEventListener implements Listener {

    private final GuiBuilder guiBuilder;
    private final RewardHandler rewardHandler;
    private final ItemRegistry itemRegistry;

    public GuiEventListener(GuiBuilder guiBuilder, RewardHandler rewardHandler, ItemRegistry itemRegistry) {
        this.guiBuilder = guiBuilder;
        this.rewardHandler = rewardHandler;
        this.itemRegistry = itemRegistry;
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        String guiKey = guiBuilder.getGuiKeyByInventory(p, e.getInventory());
        if (guiKey == null) return;

        Map<Integer, SlotType> slotTypes = guiBuilder.getGuiSlotTypes().get(guiKey);
        Map<Integer, String> groupMap = guiBuilder.getRewardGroups(guiKey);

        groupMap.values().stream().distinct().forEach(group ->
                paintRetrieval(e.getInventory(), slotTypes, groupMap, group,
                        rewardHandler.getPlayerRewardData().getAllRewards(p.getUniqueId(), group)));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() == null) return;
        String guiKey = guiBuilder.getGuiKeyByInventory(p, e.getInventory());
        if (guiKey == null) return;

        if (e.getClick() == ClickType.SHIFT_LEFT || e.getClick() == ClickType.SHIFT_RIGHT) {
            e.setCancelled(true);
            p.sendMessage("§cShift-clicking is disabled in this GUI.");
            return;
        }

        int slot = e.getSlot();
        if (e.getRawSlot() >= e.getInventory().getSize()) return;

        Map<Integer, SlotType> slotTypes = guiBuilder.getGuiSlotTypes().get(guiKey);
        SlotType st = slotTypes.get(slot);
        if (st == null) return;

        Map<Integer, String> accepted = guiBuilder.getAcceptedItems(guiKey);
        Map<Integer, Integer> amounts = guiBuilder.getAcceptedAmounts(guiKey);
        Map<Integer, Double> costs = guiBuilder.getSlotCosts(guiKey);
        Map<Integer, Boolean> perStack = guiBuilder.getCostPerStack(guiKey);
        Map<Integer, InputActionType> actions = guiBuilder.getInputActions(guiKey);
        Map<Integer, List<Integer>> conn = guiBuilder.getSlotConnections(guiKey);
        Map<Integer, String> groupMap = guiBuilder.getRewardGroups(guiKey);
        Map<Integer, String> checkItems = guiBuilder.getCheckItems(guiKey);

        switch (st) {
            case BUTTON -> {
                double fee = costs.getOrDefault(slot, 0.0);
                e.setCancelled(true);
                if (fee > 0 && !EconomyHandler.withdrawBalance(p, fee)) {
                    p.sendMessage("§cYou need " + fee);
                    return;
                }
                guiBuilder.handleButtonClick(p, guiKey, slot);
            }

            case INPUT_SLOT -> {
                ItemStack cur = e.getCursor();
                if (cur == null || cur.getType().isAir()) return;

                if (accepted.containsKey(slot)) {
                    Material m = Material.matchMaterial(accepted.get(slot));
                    if (m == null || cur.getType() != m) {
                        e.setCancelled(true);
                        p.sendMessage("§cOnly " + accepted.get(slot));
                        return;
                    }
                }

                if (groupMap.containsKey(slot)) {
                    String expectedGroup = groupMap.get(slot);
                    String key = resolveKey(cur);
                    RewardGroupManager.RewardEntry entry = rewardHandler.getRewardGroupManager().getEntryForItem(key);
                    if (entry == null || !entry.groupName().equals(expectedGroup)) {
                        e.setCancelled(true);
                        p.sendMessage("§cThis item is not allowed in this slot.");
                        return;
                    }
                }

                if (amounts.containsKey(slot) && cur.getAmount() != amounts.get(slot)) {
                    e.setCancelled(true);
                    p.sendMessage("§cYou must place exactly " + amounts.get(slot));
                    return;
                }

                // No queuing or consuming here!
            }

            case CHECK_BUTTON -> {
                e.setCancelled(true);

                List<Integer> connected = conn.get(slot);
                if (connected == null || connected.isEmpty()) {
                    p.sendMessage("§cNothing to check.");
                    return;
                }

                String group = checkItems.get(slot);
                if (group == null) {
                    p.sendMessage("§cNo reward group configured.");
                    return;
                }

                boolean allValid = true;
                for (int s : connected) {
                    ItemStack it = e.getInventory().getItem(s);
                    if (it == null) {
                        allValid = false;
                        break;
                    }
                    String key = resolveKey(it);
                    RewardGroupManager.RewardEntry entry = rewardHandler.getRewardGroupManager().getEntryForItem(key);
                    if (entry == null || !entry.groupName().equals(group)) {
                        allValid = false;
                        break;
                    }
                }

                if (!allValid) {
                    p.sendMessage("§cInvalid item(s) or wrong group.");
                    return;
                }


                double fee = costs.getOrDefault(slot, 0.0);
                if (fee > 0 && !EconomyHandler.withdrawBalance(p, fee)) {
                    p.sendMessage("§cYou need " + fee);
                    return;
                }

                for (int s : connected) {
                    ItemStack it = e.getInventory().getItem(s);
                    if (it != null) rewardHandler.queueReward(p.getUniqueId(), it);

                    if (actions.getOrDefault(s, InputActionType.NONE) == InputActionType.CONSUME) {
                        if (it != null) {
                            int amtToRemove = perStack.getOrDefault(s, false) ? it.getAmount() : 1;
                            int newAmt = it.getAmount() - amtToRemove;
                            if (newAmt > 0) it.setAmount(newAmt);
                            else e.getInventory().setItem(s, null);
                        }
                    }
                }

                guiBuilder.handleButtonClick(p, guiKey, slot);

                paintRetrieval(e.getInventory(), slotTypes, groupMap, group,
                        rewardHandler.getPlayerRewardData().getAllRewards(p.getUniqueId(), group));
            }

            case OUTPUT_SLOT -> {
                e.setCancelled(true);
                String group = groupMap.get(slot);
                if (group == null) {
                    p.sendMessage("§cNo group.");
                    return;
                }

                List<String> list = rewardHandler.getPlayerRewardData().getAllRewards(p.getUniqueId(), group);
                if (list.isEmpty()) {
                    p.sendMessage("§cNo rewards.");
                    return;
                }

                List<Integer> outs = outputSlotsForGroup(slotTypes, groupMap, group);
                int idx = outs.indexOf(slot);
                if (idx >= list.size()) {
                    e.getInventory().setItem(slot, null);
                    return;
                }

                String id = list.get(idx);
                ItemStack item = itemRegistry.getItem(id);
                if (item == null) return;
                p.getInventory().addItem(item);
                rewardHandler.getPlayerRewardData().removeReward(p.getUniqueId(), group, id);

                paintRetrieval(e.getInventory(), slotTypes, groupMap, group,
                        rewardHandler.getPlayerRewardData().getAllRewards(p.getUniqueId(), group));
            }

            case FILLER -> e.setCancelled(true);
        }
    }

    private String resolveKey(ItemStack stack) {
        if (rewardHandler.getMythic().isMythicItem(stack)) {
            return rewardHandler.getMythic().getMythicTypeFromItem(stack);
        }
        return stack.getType().name();
    }

    private void paintRetrieval(Inventory inv,
                                Map<Integer, SlotType> slotTypes,
                                Map<Integer, String> groupMap,
                                String group, List<String> rewards) {

        List<Integer> outs = outputSlotsForGroup(slotTypes, groupMap, group);
        for (int i = 0; i < outs.size(); i++) {
            int s = outs.get(i);
            if (i < rewards.size()) inv.setItem(s, itemRegistry.getItem(rewards.get(i)));
            else inv.setItem(s, null);
        }
    }

    private List<Integer> outputSlotsForGroup(Map<Integer, SlotType> slotTypes,
                                              Map<Integer, String> groupMap,
                                              String group) {
        List<Integer> list = new ArrayList<>();
        groupMap.forEach((k, v) -> {
            if (group.equals(v) && slotTypes.getOrDefault(k, SlotType.FILLER) == SlotType.OUTPUT_SLOT) list.add(k);
        });
        Collections.sort(list);
        return list;
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (guiBuilder.getGuiKeyByInventory(p, e.getInventory()) == null) return;
        e.setCancelled(true);
        p.sendMessage("§cDragging is disabled in this GUI.");
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        if (e.getInventory().getType() == InventoryType.WORKBENCH)
            RecipeBookUtils.forceCloseClientRecipeBook(p);
    }
}
