package net.anatomyworld.harambeCore;

import net.anatomyworld.harambeCore.GuiBuilder.InputActionType;
import net.anatomyworld.harambeCore.GuiBuilder.SlotType;
import net.anatomyworld.harambeCore.death.DeathListener;
import net.anatomyworld.harambeCore.item.ItemRegistry;
import net.anatomyworld.harambeCore.rewards.RewardHandler;
import net.anatomyworld.harambeCore.util.EconomyHandler;
import net.anatomyworld.harambeCore.util.recipebook.RecipeBookUtils;
import org.bukkit.Material;
import net.anatomyworld.harambeCore.death.DeathListener.KeyInfo;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import java.util.UUID;

import java.util.*;

public class GuiEventListener implements Listener {

    private final GuiBuilder    guiBuilder;
    private final RewardHandler rewardHandler;
    private final ItemRegistry  itemRegistry;

    public GuiEventListener(GuiBuilder guiBuilder,
                            RewardHandler rewardHandler,
                            ItemRegistry itemRegistry) {
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

        groupMap.values().stream().distinct()
                .forEach(group -> paintRetrieval(
                        e.getInventory(),
                        slotTypes,
                        groupMap,
                        group,
                        rewardHandler.playerData().getAllRewards(p.getUniqueId(), group)
                ));
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
        if (e.getClickedInventory() == null)        return;

        String guiKey = guiBuilder.getGuiKeyByInventory(p, e.getInventory());
        if (guiKey == null) return;

        if (e.getClick() == ClickType.SHIFT_LEFT ||
                e.getClick() == ClickType.SHIFT_RIGHT) {
            e.setCancelled(true);
            p.sendMessage("§cShift-clicking is disabled in this GUI.");
            return;
        }

        int clicked = e.getSlot();
        if (e.getRawSlot() >= e.getInventory().getSize()) return;



        Map<Integer, SlotType>        slotTypes = guiBuilder.getGuiSlotTypes().get(guiKey);
        SlotType                      st        = slotTypes.get(clicked);
        if (st == null) return;

        Map<Integer, String>         perms      = guiBuilder.getSlotPermissions(guiKey);

        /* ────────────────── PERMISSION GATE ────────────────── */
        String needed = perms.get(clicked);
        if (needed != null && !p.hasPermission(needed)) {
            e.setCancelled(true);
            p.sendMessage("§cYou don’t have permission to use this.");
            return;
        }

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
        Map<Integer, Boolean>        copyMap    = guiBuilder.getCopyItems(guiKey);
        Map<Integer,String>          logicMap = guiBuilder.getButtonLogic(guiKey);
        Map<Integer, Boolean>        wholeMap   = guiBuilder.getWholeStack(guiKey);


        int page = guiBuilder.getPage(p.getUniqueId(), guiKey);

        switch (st) {

            /* ---------------- BUTTON ---------------- */
            case BUTTON, HUSKHOME_BUTTON -> {
                Map<Integer,String>  outputs = guiBuilder.getOutputItems(guiKey);
                Map<Integer,Integer> pays    = guiBuilder.getPayoutAmounts(guiKey);
                Map<Integer,Boolean> scaleOk = guiBuilder.getScaleWithOutput(guiKey);

                double  fee   = costs.getOrDefault(clicked, 0.0);
                boolean give  = costPay.getOrDefault(clicked, false);
                e.setCancelled(true);  // always cancel vanilla handling

                // ── PAGE buttons: parse whatever integer follows "page:"
                if (logicMap != null) {
                    String tag = logicMap.get(clicked);
                    if (tag != null && tag.startsWith("page:")) {
                        try {
                            int delta = Integer.parseInt(tag.substring("page:".length()));
                            switchPage(p, guiKey, page + delta);
                        } catch (NumberFormatException ignored) {
                            // not a valid page:<number> tag
                        }
                        return;
                    }
                }

                /* ---------- DEPOSIT-style buttons (payout: true) ---------- */
                if (give) {
                    if (fee > 0) EconomyHandler.depositBalance(p, fee);
                    guiBuilder.handleButtonClick(p, guiKey, clicked);
                    return;
                }

                /* ----------- WITHDRAW buttons (payout: false) ------------- */
                if (fee <= 0) {  // freebies
                    guiBuilder.handleButtonClick(p, guiKey, clicked);
                    return;
                }

                double bal = EconomyHandler.getBalance(p);

                // enough money: do normal transaction
                if (bal >= fee) {
                    if (!EconomyHandler.withdrawBalance(p, fee)) {
                        p.sendMessage("§cTransaction failed.");
                        return;
                    }
                    guiBuilder.handleButtonClick(p, guiKey, clicked);
                    return;
                }

                // not enough → can we scale?
                if (!scaleOk.getOrDefault(clicked, false) || !outputs.containsKey(clicked)) {
                    p.sendMessage("§cYou need " + fee);
                    return;
                }

                /* ------------- partial withdrawal logic ------------------ */
                int    baseAmt   = pays.getOrDefault(clicked, 1);
                String raw       = outputs.get(clicked);
                int    scaledAmt = (int) Math.floor((bal / fee) * baseAmt);

                if (scaledAmt < 1) {
                    p.sendMessage("§cInsufficient funds.");
                    return;
                }

                double scaledCost = (fee / baseAmt) * scaledAmt;
                if (!EconomyHandler.withdrawBalance(p, scaledCost)) {
                    p.sendMessage("§cTransaction failed.");
                    return;
                }

                // give the scaled items
                if (raw.toUpperCase(Locale.ROOT).startsWith("MYTHIC:")) {
                    String id = raw.substring("MYTHIC:".length());
                    ItemStack it = itemRegistry.getItem(id);
                    if (it != null) {
                        it.setAmount(scaledAmt);
                        p.getInventory().addItem(it);
                    }
                } else {
                    Material mat = Material.matchMaterial(raw);
                    if (mat != null) p.getInventory().addItem(new ItemStack(mat, scaledAmt));
                }

                p.sendMessage("§aWithdrew §e" + scaledAmt + " §afor §e" + scaledCost);
            }


            /* ---------------- INPUT_SLOT ------------ */
            case INPUT_SLOT -> {
                ItemStack cur = e.getCursor();
                if (cur.getType().isAir()) return;

                /* ---------- 1. Mythic / material whitelist ---------- */
                if (accepted.containsKey(clicked)) {
                    String expected = accepted.get(clicked);
                    if (expected.startsWith("MYTHIC:")) {
                        String id = expected.substring("MYTHIC:".length());
                        if (!rewardHandler.mythic().isMythicItem(cur) ||
                                !id.equalsIgnoreCase(rewardHandler.mythic().getMythicTypeFromItem(cur))) {
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

                /* ---------- 2. Reward-group filter (skip if copy_item:true) ---------- */
                if (groupMap.containsKey(clicked)) {
                    // check-button this input belongs to (if any)
                    Integer chkSlot = reverse.get(clicked);
                    boolean copyFlag = chkSlot != null && copyMap.getOrDefault(chkSlot, false);

                    if (!copyFlag) {                   // enforce group only when NOT copying
                        String expectedGroup = groupMap.get(clicked);
                        String key           = resolveKey(cur);
                        var entry            = rewardHandler.groupMgr().getEntryForItem(key);

                        if (entry == null || !entry.groupName().equals(expectedGroup)) {
                            e.setCancelled(true);
                            p.sendMessage("§cThis item not allowed here.");
                            return;
                        }
                    }
                }

                /* ---------- 3. Exact-amount check ---------- */
                if (amounts.containsKey(clicked) && cur.getAmount() != amounts.get(clicked)) {
                    e.setCancelled(true);
                    p.sendMessage("§cYou must place exactly " + amounts.get(clicked));
                    return;
                }

                /* ---------- 4. Cost + queue logic ---------- */
                InputActionType ia = actions.getOrDefault(clicked, InputActionType.NONE);
                boolean defer      = reverse.containsKey(clicked);   // consumed later by CHECK_BUTTON

                if (ia == InputActionType.CONSUME && !defer) {
                    double unit = costs.getOrDefault(clicked, 0.0);
                    int mul     = perStack.getOrDefault(clicked, false) ? cur.getAmount() : 1;
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
                    else                        cur.setAmount(cur.getAmount() - mul);

                    e.setCancelled(true);
                    p.sendMessage("§aDeposited!");
                }
            }

            /* ---------------- CHECK_BUTTON ---------- */
            case CHECK_BUTTON -> {
                e.setCancelled(true);

                List<Integer> targets = conn.get(clicked);      // linked INPUT_SLOTs
                String tag = logicMap.get(clicked);             // custom tag

/* ============================================================
   A.  Death-key submission / validation (“DEATH_ITEMS”)
   ============================================================ */
                if ("DEATH_ITEMS".equalsIgnoreCase(tag)) {

                    if (targets == null || targets.isEmpty()) {
                        p.sendMessage("§cPlace a death key first.");
                        return;
                    }

                    ItemStack keyStack = e.getInventory().getItem(targets.get(0));
                    KeyInfo info = DeathListener.readKey(keyStack);
                    if (info == null) {                         // invalid or wrong item
                        p.sendMessage("§cInvalid death key."); return;
                    }

                    /* ✦ NEW ✦  do not allow redemption in a different MV-Inv group */
                    if (!isInMvGroup(p, info.group())) {
                        p.sendMessage("§cThis death key belongs to another world-group.");
                        return;
                    }

                    /* death-<mvGroup>-<ownerUuid> */
                    String deathGroup = "death-" + info.group() + "-" + info.owner();

                    /* ─── (C) bail out if that chest has expired ─── */
                    if (rewardHandler.playerData().isExpired(info.owner(), deathGroup)) {
                        p.sendMessage("§cThat death chest decayed after 1 hour.");
                        rewardHandler.playerData().removeGroup(info.owner(), deathGroup);
                        return;
                    }

                    /* ─── (D) chest empty?  (NEW) ─────────────────── */
                    boolean noStacks = rewardHandler.playerData()
                            .getStackRewards(info.owner(), deathGroup).isEmpty();
                    boolean noIds = rewardHandler.playerData()
                            .getAllRewards(info.owner(), deathGroup).isEmpty();
                    if (noStacks && noIds) {
                        p.sendMessage("§cNo death chest found for that key.");
                        return;                     // ↞ key *not* consumed
                    }

                    /* 1. remember mapping so OUTPUT_SLOTs know what to show */
                    Map<Integer,String> globalGroups = guiBuilder.getRewardGroups(guiKey);

                    for (Map.Entry<Integer,SlotType> entry : slotTypes.entrySet()) {
                        int slot = entry.getKey();
                        if (entry.getValue() == SlotType.OUTPUT_SLOT &&
                                "DEATH_GET".equalsIgnoreCase(logicMap.get(slot))) {
                            globalGroups.put(slot, deathGroup);
                            groupMap.put(slot, deathGroup);
                        }
                    }

                    /* 2. repaint loot immediately */
                    paintRetrieval(e.getInventory(),
                            slotTypes,
                            groupMap,
                            deathGroup,
                            rewardHandler.playerData()
                                    .getAllRewards(info.owner(), deathGroup));

                    /* 3. consume one key  (only reached if chest exists) */
                    keyStack.setAmount(keyStack.getAmount() - 1);
                    if (keyStack.getAmount() <= 0) {
                        e.getInventory().setItem(targets.get(0), null);
                    }
                    return;  // ✔ done – skip normal flow
                }

/* ============================================================
   B.  Normal reward-group CHECK_BUTTON logic (unchanged)
   ============================================================ */
                if (targets == null || targets.isEmpty()) {
                    p.sendMessage("§cNothing to check."); return;
                }
                String group = checkItems.get(clicked);
                if (group == null) {
                    p.sendMessage("§cNo reward group configured."); return;
                }

                boolean copyFlag = copyMap.getOrDefault(clicked, false);
                boolean wholeFlag = wholeMap.getOrDefault(clicked, false);


                /* validate items (unless copy_item:true) */
                boolean hasAny = false;
                boolean valid  = true;

                for (int s : targets) {
                    ItemStack it = e.getInventory().getItem(s);
                    if (it == null || it.getType() == Material.AIR) {
                        if (!copyFlag) { valid = false; }     // empty slot only matters if we DO validate
                        continue;
                    }

                    hasAny = true;

                    if (!copyFlag) {                          // full reward-group validation
                        var entry = rewardHandler.groupMgr().getEntryForItem(resolveKey(it));
                        if (entry == null || !entry.groupName().equals(group)) {
                            valid = false; break;
                        }
                    }
                }

                if (!hasAny) { p.sendMessage("§cPut at least one item into the linked slots."); return; }
                if (!valid)   { p.sendMessage("§cInvalid item(s) for this submission.");        return; }

                /* ----- fee handling ----- */
                double fee = costs.getOrDefault(clicked, 0.0);
                if (fee > 0) {
                    if (costPay.getOrDefault(clicked, false))
                        EconomyHandler.depositBalance(p, fee);
                    else if (!EconomyHandler.withdrawBalance(p, fee)) {
                        p.sendMessage("§cYou need " + fee); return;
                    }
                }

                /* ----- queue / copy / consume ----- */
                for (int s : targets) {
                    ItemStack it = e.getInventory().getItem(s);
                    if (it == null) continue;

                    /* 1) add rewards – whole stack or single item */
                    int rewardAmt = wholeFlag ? it.getAmount() : 1;
                    var entry = rewardHandler.groupMgr().getEntryForItem(resolveKey(it));
                    if (entry != null) {
                        rewardHandler.playerData()
                                .addReward(p.getUniqueId(),
                                        entry.groupName(),
                                        entry.rewardId(),
                                        rewardAmt);
                    }

                    /* 2) optional copy of the whole stack */
                    if (copyFlag) {
                        rewardHandler.playerData()
                                .addStackReward(p.getUniqueId(), group, it.clone());
                    }

                    /* 3) consume */
                    if (actions.getOrDefault(s, InputActionType.NONE) == InputActionType.CONSUME) {
                        if (wholeFlag) {
                            e.getInventory().setItem(s, null);          // eat everything
                        } else {
                            int left = it.getAmount() - 1;              // eat one
                            if (left > 0) it.setAmount(left);
                            else          e.getInventory().setItem(s, null);
                        }
                    }
                }
                guiBuilder.handleButtonClick(p, guiKey, clicked);

                paintRetrieval(e.getInventory(),
                        slotTypes,
                        groupMap,
                        group,
                        rewardHandler.playerData()
                                .getAllRewards(p.getUniqueId(), group));
            }



            /* ---------------- OUTPUT_SLOT ---------- */
            case OUTPUT_SLOT -> {
                e.setCancelled(true);

                String group = groupMap.get(clicked);   // may be null
                String logic = logicMap.get(clicked);   // e.g. DEATH_GET

/* ────────────────────────────────────────────────
   A.  Death-chest retrieval      (logic == DEATH_GET)
   ──────────────────────────────────────────────── */
                if ("DEATH_GET".equalsIgnoreCase(logic)) {

                    if (group == null || !group.startsWith("death-")) {
                        p.sendMessage("§cNo death chest loaded.");
                        return;
                    }

                    /* format: death-<mvGroup>-<victimUuid> */
                    String[] parts = group.split("-", 3);
                    if (parts.length < 3) {
                        p.sendMessage("§cCorrupted chest reference.");
                        return;
                    }

                    String mvGroup = parts[1];
                    if (!isInMvGroup(p, mvGroup)) {
                        p.sendMessage("§cYou must be in that world-group to loot this chest.");
                        return;
                    }

                    UUID victim = UUID.fromString(parts[2]);

                    /* ─── (B) abort if the loot expired ─── */
                    if (rewardHandler.playerData().isExpired(victim, group)) {
                        p.sendMessage("§cThis death chest decayed after 1 hour.");
                        rewardHandler.playerData().removeGroup(victim, group);
                        return;
                    }

                    /* 1) concrete ItemStacks first */
                    List<ItemStack> queue =
                            rewardHandler.playerData().getStackRewards(victim, group);

                    if (!queue.isEmpty()) {
                        ItemStack give = queue.get(0).clone();
                        p.getInventory().addItem(give);
                        rewardHandler.playerData().popFirstStackReward(victim, group);

                        paintRetrieval(e.getInventory(), slotTypes, groupMap, group,
                                rewardHandler.playerData().getAllRewards(victim, group));
                        return;
                    }
                    p.sendMessage("§cChest is empty.");
                    return;
                }

/* ────────────────────────────────────────────────
   B.  Normal reward OUTPUT_SLOT logic (unchanged)
   ──────────────────────────────────────────────── */
                if (group == null) { p.sendMessage("§cNo rewards."); return; }

                /* 1) queued concrete stacks */
                List<ItemStack> stackRewards =
                        rewardHandler.playerData().getStackRewards(p.getUniqueId(), group);

                if (!stackRewards.isEmpty()) {
                    ItemStack give = stackRewards.get(0).clone();
                    p.getInventory().addItem(give);
                    rewardHandler.playerData().popFirstStackReward(p.getUniqueId(), group);

                    paintRetrieval(e.getInventory(), slotTypes, groupMap, group,
                            rewardHandler.playerData().getAllRewards(p.getUniqueId(), group));
                    return;
                }

                /* 2) id→item rewards */
                List<Integer> outs = outputSlotsForGroup(slotTypes, groupMap, group);
                Map<String,Integer> map =
                        rewardHandler.playerData().getAllRewards(p.getUniqueId(), group);

                if (map.isEmpty()) { p.sendMessage("§cNo rewards."); return; }

                List<String> keys = new ArrayList<>(map.keySet());
                int idx = outs.indexOf(clicked);
                if (idx < 0 || idx >= keys.size()) {           // clicked empty cell
                    e.getInventory().setItem(clicked, null); return;
                }

                String id  = keys.get(idx);
                int    qty = map.get(id);

                ItemStack proto = itemRegistry.getItem(id);
                if (proto == null) return;

                int giveAmt = Math.min(qty, proto.getMaxStackSize());
                ItemStack give = proto.clone();
                give.setAmount(giveAmt);
                p.getInventory().addItem(give);

                rewardHandler.playerData().removeReward(p.getUniqueId(), group, id, giveAmt);

                paintRetrieval(e.getInventory(), slotTypes, groupMap, group,
                        rewardHandler.playerData().getAllRewards(p.getUniqueId(), group));
            }

            /* ---------------- FILLER ---------------- */
            case FILLER -> e.setCancelled(true);
        }
    }

    /* ---------------- helper methods ---------------- */

    private String resolveKey(ItemStack stack) {
        if (rewardHandler.mythic().isMythicItem(stack)) {
            return rewardHandler.mythic().getMythicTypeFromItem(stack);
        }
        return stack.getType().name();
    }

    private void paintRetrieval(Inventory inv,
                                Map<Integer, GuiBuilder.SlotType> slotTypes,
                                Map<Integer, String>              groupMap,
                                String                            group,
                                Map<String, Integer>              rewards) {

        /* ───── determine whose rewards we’re showing (viewer or victim) ───── */
        UUID dataUuid;
        if (group != null && group.startsWith("death-")) {
            try {
                // Extract full UUID from the end of the group string (always 36 characters)
                String uuidStr = group.substring(group.length() - 36);
                dataUuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException ex) {
                dataUuid = inv.getViewers().isEmpty()
                        ? new UUID(0, 0)
                        : inv.getViewers().get(0).getUniqueId();
            }
        } else {
            dataUuid = inv.getViewers().isEmpty()
                    ? new UUID(0, 0)
                    : inv.getViewers().get(0).getUniqueId();
        }

        /* ─── (A) auto-purge and abort if this death-queue expired ─── */
        if (group != null && group.startsWith("death-") &&
                rewardHandler.playerData().isExpired(dataUuid, group)) {
            rewardHandler.playerData().removeGroup(dataUuid, group);
            return;                   // nothing to show – it decayed
        }

        /* ------------------------------------------------------------------ */
        List<Integer> outs = outputSlotsForGroup(slotTypes, groupMap, group);

        /* 1) concrete ItemStacks ------------------------------------------- */
        List<ItemStack> stackRewards =
                rewardHandler.playerData().getStackRewards(dataUuid, group);

        int pos = 0;
        for (; pos < outs.size() && pos < stackRewards.size(); pos++) {
            inv.setItem(outs.get(pos), stackRewards.get(pos));
        }

        /* 2) id→item reward queue ------------------------------------------ */
        List<String> ids = new ArrayList<>(rewards.keySet());
        for (int i = 0; pos < outs.size(); pos++, i++) {
            int slot = outs.get(pos);

            if (i >= ids.size()) {
                inv.setItem(slot, null);
                continue;
            }

            String id  = ids.get(i);
            int    qty = rewards.get(id);

            ItemStack proto = itemRegistry.getItem(id);
            if (proto == null) {
                inv.setItem(slot, null);
                continue;
            }

            ItemStack stack = proto.clone();
            stack.setAmount(Math.min(qty, stack.getMaxStackSize()));
            inv.setItem(slot, stack);
        }
    }



    private boolean isInMvGroup(Player p, String groupName) {
        var mgr = com.onarandombox.multiverseinventories.MultiverseInventories
                .getPlugin().getGroupManager();

        return mgr.getGroupsForWorld(p.getWorld().getName())
                .stream()
                .anyMatch(g -> g.getName().equalsIgnoreCase(groupName));
    }

    private List<Integer> outputSlotsForGroup(Map<Integer, SlotType> slotTypes,
                                              Map<Integer, String>   groupMap,
                                              String                 group) {
        List<Integer> list = new ArrayList<>();
        groupMap.forEach((slot, grp) -> {
            if (group.equals(grp) &&
                    slotTypes.getOrDefault(slot, SlotType.FILLER) == SlotType.OUTPUT_SLOT) {
                list.add(slot);
            }
        });
        Collections.sort(list);
        return list;
    }

    private void switchPage(Player p, String guiKey, int target){
        int max = guiBuilder.getMaxPages(guiKey);
        if (target < 0 || target >= max) return;

        UUID id = p.getUniqueId();
        guiBuilder.handleGuiClose(p, p.getOpenInventory().getTopInventory()); // save current
        guiBuilder.setPage(id, guiKey, target);

        Inventory fresh = guiBuilder.generateGui(guiKey, id, target);
        guiBuilder.playerGuis.get(id).put(guiKey, fresh);
        p.openInventory(fresh);
    }
}
