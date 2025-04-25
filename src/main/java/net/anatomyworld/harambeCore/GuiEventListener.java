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

    private final GuiBuilder    guiBuilder;
    private final RewardHandler rewardHandler;
    private final ItemRegistry  itemRegistry;

    public GuiEventListener(GuiBuilder guiBuilder, RewardHandler rewardHandler, ItemRegistry itemRegistry) {
        this.guiBuilder    = guiBuilder;
        this.rewardHandler = rewardHandler;
        this.itemRegistry  = itemRegistry;
    }

    /* ---------------- open / close / drag ---------------- */

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        String guiKey = guiBuilder.getGuiKeyByInventory(p, e.getInventory());
        if (guiKey == null) return;

        Map<Integer, SlotType> slotTypes = guiBuilder.getGuiSlotTypes().get(guiKey);
        Map<Integer, String>   groupMap  = guiBuilder.getRewardGroups(guiKey);

        groupMap.values().stream().distinct().forEach(group ->
                paintRetrieval(e.getInventory(), slotTypes, groupMap, group,
                        rewardHandler.getPlayerRewardData().getAllRewards(p.getUniqueId(), group)));
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
        guiBuilder.handleGuiClose(p, e.getInventory());

        if (e.getInventory().getType() == InventoryType.WORKBENCH)
            RecipeBookUtils.forceCloseClientRecipeBook(p);
    }

    /* ---------------- central click handler --------------- */

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

        int clicked = e.getSlot();
        if (e.getRawSlot() >= e.getInventory().getSize()) return;

        Map<Integer, SlotType> slotTypes   = guiBuilder.getGuiSlotTypes().get(guiKey);
        SlotType st = slotTypes.get(clicked);
        if (st == null) return;

        Map<Integer, String>         accepted   = guiBuilder.getAcceptedItems(guiKey);
        Map<Integer, Integer>        amounts    = guiBuilder.getAcceptedAmounts(guiKey);
        Map<Integer, Double>         costs      = guiBuilder.getSlotCosts(guiKey);
        Map<Integer, Boolean>        perStack   = guiBuilder.getCostPerStack(guiKey);
        Map<Integer, Boolean>        costPay    = guiBuilder.getCostIsPayout(guiKey);
        Map<Integer, InputActionType>actions    = guiBuilder.getInputActions(guiKey);
        Map<Integer, List<Integer>>  conn       = guiBuilder.getSlotConnections(guiKey);
        Map<Integer, Integer>        reverse    = guiBuilder.getReverseSlotConnections(guiKey);
        Map<Integer, String>         groupMap   = guiBuilder.getRewardGroups(guiKey);
        Map<Integer, String>         checkItems = guiBuilder.getCheckItems(guiKey);

        switch (st) {

            /* ---------------- BUTTON ---------------- */
            case BUTTON -> {
                double fee   = costs.getOrDefault(clicked, 0.0);
                boolean give = costPay.getOrDefault(clicked, false);

                e.setCancelled(true);
                if (fee > 0) {
                    if (give) {
                        EconomyHandler.depositBalance(p, fee);
                    } else if (!EconomyHandler.withdrawBalance(p, fee)) {
                        p.sendMessage("§cYou need " + fee);
                        return;
                    }
                }
                guiBuilder.handleButtonClick(p, guiKey, clicked);
            }

            /* ---------------- INPUT_SLOT ------------ */
            case INPUT_SLOT -> {
                ItemStack cur = e.getCursor();
                if (cur.getType().isAir()) return;

                /* --- accepted item / mythic check --- */
                if (accepted.containsKey(clicked)) {
                    String expected = accepted.get(clicked);
                    if (expected.toUpperCase(Locale.ROOT).startsWith("MYTHIC:")) {
                        String id = expected.substring("MYTHIC:".length());
                        if (!rewardHandler.getMythic().isMythicItem(cur) ||
                                !id.equalsIgnoreCase(rewardHandler.getMythic().getMythicTypeFromItem(cur))) {
                            e.setCancelled(true);
                            p.sendMessage("§cOnly Mythic item: " + id);
                            return;
                        }
                    } else {
                        Material mat = Material.matchMaterial(expected);
                        if (mat == null || cur.getType() != mat) {
                            e.setCancelled(true);
                            p.sendMessage("§cOnly " + expected);
                            return;
                        }
                    }
                }

                /* --- reward group filter --- */
                if (groupMap.containsKey(clicked)) {
                    String expectedGroup = groupMap.get(clicked);
                    String key = resolveKey(cur);
                    RewardGroupManager.RewardEntry entry =
                            rewardHandler.getRewardGroupManager().getEntryForItem(key);
                    if (entry == null || !entry.groupName().equals(expectedGroup)) {
                        e.setCancelled(true);
                        p.sendMessage("§cThis item is not allowed in this slot.");
                        return;
                    }
                }

                /* --- exact amount check --- */
                if (amounts.containsKey(clicked) && cur.getAmount() != amounts.get(clicked)) {
                    e.setCancelled(true);
                    p.sendMessage("§cYou must place exactly " + amounts.get(clicked));
                    return;
                }

                InputActionType ia      = actions.getOrDefault(clicked, InputActionType.NONE);
                boolean deferredConsume = reverse.containsKey(clicked);

                /* --- immediate consume branch --- */
                if (ia == InputActionType.CONSUME && !deferredConsume) {
                    double unit = costs.getOrDefault(clicked, 0.0);
                    int    mul  = perStack.getOrDefault(clicked, false) ? cur.getAmount() : 1;
                    double sum  = unit * mul;

                    if (sum > 0) {
                        if (costPay.getOrDefault(clicked, false))
                            EconomyHandler.depositBalance(p, sum);
                        else if (!EconomyHandler.withdrawBalance(p, sum)) {
                            e.setCancelled(true);
                            p.sendMessage("§cYou need " + sum);
                            return;
                        }
                    }

                    rewardHandler.queueReward(p.getUniqueId(), cur.clone());

                    if (mul >= cur.getAmount()) e.getView().setCursor(null);
                    else cur.setAmount(cur.getAmount() - mul);

                    e.setCancelled(true);
                    p.sendMessage("§aDeposited!");
                }
            }

            /* ---------------- CHECK_BUTTON ---------- */
            case CHECK_BUTTON -> {
                e.setCancelled(true);

                List<Integer> targets = conn.get(clicked);
                if (targets == null || targets.isEmpty()) {
                    p.sendMessage("§cNothing to check.");
                    return;
                }

                String group = checkItems.get(clicked);
                if (group == null) {
                    p.sendMessage("§cNo reward group configured.");
                    return;
                }

                boolean allValid = true;
                for (int s : targets) {
                    ItemStack it = e.getInventory().getItem(s);
                    if (it == null) { allValid = false; break; }
                    String key = resolveKey(it);
                    RewardGroupManager.RewardEntry entry =
                            rewardHandler.getRewardGroupManager().getEntryForItem(key);
                    if (entry == null || !entry.groupName().equals(group)) { allValid = false; break; }
                }

                if (!allValid) {
                    p.sendMessage("§cInvalid item(s) or wrong group.");
                    return;
                }

                double fee = costs.getOrDefault(clicked, 0.0);
                if (fee > 0) {
                    if (costPay.getOrDefault(clicked, false))
                        EconomyHandler.depositBalance(p, fee);
                    else if (!EconomyHandler.withdrawBalance(p, fee)) {
                        p.sendMessage("§cYou need " + fee);
                        return;
                    }
                }

                for (int s : targets) {
                    ItemStack it = e.getInventory().getItem(s);
                    if (it != null) rewardHandler.queueReward(p.getUniqueId(), it);

                    if (actions.getOrDefault(s, InputActionType.NONE) == InputActionType.CONSUME) {
                        int rem;
                        if (perStack.getOrDefault(s, false)) {
                            assert it != null;
                            rem = it.getAmount();
                        } else {
                            rem = 1;
                        }
                        assert it != null;
                        int left = it.getAmount() - rem;
                        if (left > 0) it.setAmount(left);
                        else e.getInventory().setItem(s, null);
                    }
                }

                guiBuilder.handleButtonClick(p, guiKey, clicked);
                paintRetrieval(e.getInventory(), slotTypes, groupMap, group,
                        rewardHandler.getPlayerRewardData().getAllRewards(p.getUniqueId(), group));
            }

            /* ---------------- OUTPUT_SLOT ---------- */
            case OUTPUT_SLOT -> {
                e.setCancelled(true);
                String group = groupMap.get(clicked);
                if (group == null) { p.sendMessage("§cNo group."); return; }

                List<String> rewards = rewardHandler.getPlayerRewardData().getAllRewards(p.getUniqueId(), group);
                if (rewards.isEmpty()) { p.sendMessage("§cNo rewards."); return; }

                List<Integer> outs = outputSlotsForGroup(slotTypes, groupMap, group);
                int idx = outs.indexOf(clicked);
                if (idx >= rewards.size()) { e.getInventory().setItem(clicked, null); return; }

                String id = rewards.get(idx);
                ItemStack rewardItem = itemRegistry.getItem(id);
                if (rewardItem == null) return;

                p.getInventory().addItem(rewardItem);
                rewardHandler.getPlayerRewardData().removeReward(p.getUniqueId(), group, id);

                paintRetrieval(e.getInventory(), slotTypes, groupMap, group,
                        rewardHandler.getPlayerRewardData().getAllRewards(p.getUniqueId(), group));
            }

            case FILLER -> e.setCancelled(true);
        }
    }

    /* ---------------- helper methods ---------------- */

    private String resolveKey(ItemStack stack) {
        if (rewardHandler.getMythic().isMythicItem(stack))
            return rewardHandler.getMythic().getMythicTypeFromItem(stack);
        return stack.getType().name();
    }

    private void paintRetrieval(Inventory inv,
                                Map<Integer, SlotType> slotTypes,
                                Map<Integer, String> groupMap,
                                String group,
                                List<String> rewards) {
        List<Integer> outs = outputSlotsForGroup(slotTypes, groupMap, group);
        for (int i = 0; i < outs.size(); i++) {
            int s = outs.get(i);
            if (i < rewards.size()) inv.setItem(s, itemRegistry.getItem(rewards.get(i)));
            else inv.setItem(s, null);
        }
    }

    private List<Integer> outputSlotsForGroup(Map<Integer, SlotType> slotTypes,
                                              Map<Integer, String>  groupMap,
                                              String group) {
        List<Integer> list = new ArrayList<>();
        groupMap.forEach((slot, grp) -> {
            if (group.equals(grp) && slotTypes.getOrDefault(slot, SlotType.FILLER) == SlotType.OUTPUT_SLOT)
                list.add(slot);
        });
        Collections.sort(list);
        return list;
    }


}
