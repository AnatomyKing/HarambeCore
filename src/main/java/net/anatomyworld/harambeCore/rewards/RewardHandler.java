package net.anatomyworld.harambeCore.rewards;

import io.lumine.mythic.api.items.ItemManager;
import io.lumine.mythic.bukkit.MythicBukkit;
import net.anatomyworld.harambeCore.item.ItemRegistry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages queuing and granting rewards.
 * Exposes only modern accessors: playerData(), groupMgr(), mythic().
 */
public class RewardHandler {

    private final RewardGroupManager groups;
    private final PlayerRewardData   data;
    private final ItemManager        mythic;

    public RewardHandler(RewardGroupManager groups, PlayerRewardData data) {
        this.groups = groups;
        this.data   = data;
        this.mythic = MythicBukkit.inst().getItemManager();
    }

    /* ------------------------------------------------------------------ */
    /*  Modern getters                                                    */
    /* ------------------------------------------------------------------ */

    /** The per-player reward queue storage. */
    public PlayerRewardData playerData() {
        return data;
    }

    /** The in-memory mapping of “group → (itemKey → rewardId)”. */
    public RewardGroupManager groupMgr() {
        return groups;
    }

    /** MythicBukkit’s item manager for resolving “mythic:” keys. */
    public ItemManager mythic() {
        return mythic;
    }

    /* ------------------------------------------------------------------ */
    /*  Queue / give rewards                                              */
    /* ------------------------------------------------------------------ */

    /** Queue by ItemStack (resolves to key automatically). */
    public void queueReward(UUID uuid, ItemStack stack) {
        String key = resolveKey(stack);
        if (key == null) return;
        queueReward(uuid, key);
    }

    /** Queue by resolved key. */
    public boolean queueReward(UUID uuid, String key) {
        var entry = groups.getEntryForItem(key);
        if (entry == null) return false;
        data.addReward(uuid, entry.groupName(), entry.rewardId());
        return true;
    }

    /** Give one reward (first in queue or specified). */
    public boolean giveReward(Player p, String group, String id, ItemRegistry reg) {

        Map<String, Integer> map = data.getAllRewards(p.getUniqueId(), group);
        if (map.isEmpty()) return false;

        // choose specific or first
        String rewardId = (id != null && map.containsKey(id))
                ? id
                : map.keySet().iterator().next();

        int queued = map.get(rewardId);
        ItemStack reward = reg.getItem(rewardId);
        if (reward == null) return false;

        // hand out one normal-sized stack
        int give = Math.min(queued, reward.getMaxStackSize());
        reward.setAmount(give);
        p.getInventory().addItem(reward);

        data.removeReward(p.getUniqueId(), group, rewardId, give);
        p.sendMessage("§aYou received §e" + give + "× " + rewardId);
        return true;
    }

    /** Give *all* queued rewards in a group. */
    public void giveAllRewards(Player p, String group, ItemRegistry reg) {

        Map<String, Integer> map = data.getAllRewards(p.getUniqueId(), group);
        if (map.isEmpty()) {
            p.sendMessage("§cNo rewards."); return;
        }

        map.forEach((id, qty) -> {
            ItemStack proto = reg.getItem(id);
            if (proto == null) return;

            int max = proto.getMaxStackSize();
            int left = qty;
            while (left > 0) {
                ItemStack stack = proto.clone();
                int give = Math.min(left, max);
                stack.setAmount(give);
                p.getInventory().addItem(stack);
                left -= give;
            }
            data.removeReward(p.getUniqueId(), group, id, qty);
        });
        p.sendMessage("§aAll rewards claimed!");
    }

    /* ------------------------------------------------------------------ */
    /*  Internal helper                                                   */
    /* ------------------------------------------------------------------ */

    private String resolveKey(ItemStack s) {
        return mythic.isMythicItem(s)
                ? mythic.getMythicTypeFromItem(s)
                : s.getType().name();
    }
}
