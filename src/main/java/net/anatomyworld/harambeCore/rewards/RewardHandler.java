package net.anatomyworld.harambeCore.rewards;

import io.lumine.mythic.api.items.ItemManager;
import io.lumine.mythic.bukkit.MythicBukkit;
import net.anatomyworld.harambeCore.item.ItemRegistry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
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
        String rewardId = id == null
                ? data.getAllRewards(p.getUniqueId(), group).stream().findFirst().orElse(null)
                : id;
        if (rewardId == null) return false;

        var reward = reg.getItem(rewardId);
        if (reward == null) return false;

        p.getInventory().addItem(reward);
        p.sendMessage("§aYou received your reward: " + rewardId);
        data.removeReward(p.getUniqueId(), group, rewardId);
        return true;
    }

    /** Give *all* queued rewards in a group. */
    public void giveAllRewards(Player p, String group, ItemRegistry reg) {
        List<String> list = data.getAllRewards(p.getUniqueId(), group);
        for (String rid : list) {
            var it = reg.getItem(rid);
            if (it != null) p.getInventory().addItem(it);
        }
        data.removeGroup(p.getUniqueId(), group);
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
