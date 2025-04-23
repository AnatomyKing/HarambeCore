package net.anatomyworld.harambeCore.item;

import io.lumine.mythic.api.items.ItemManager;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class RewardHandler {

    private final RewardGroupManager rewardGroupManager;
    private final PlayerRewardData   playerRewardData;
    private final ItemManager        mythic;

    public RewardHandler(RewardGroupManager groups, PlayerRewardData data) {
        this.rewardGroupManager = groups;
        this.playerRewardData   = data;
        this.mythic             = MythicBukkit.inst().getItemManager();
    }

    /* ---------------------------------------------------------------------- */
    /*  Public helpers                                                        */
    /* ---------------------------------------------------------------------- */

    public PlayerRewardData getPlayerRewardData()     { return playerRewardData; }
    public RewardGroupManager getRewardGroupManager() { return rewardGroupManager; }

    /** Called by GUI code – resolves key from ItemStack */
    public boolean queueReward(UUID player, ItemStack stack) {
        String key = resolveKey(stack);
        if (key == null) return false;
        return queueReward(player, key);
    }

    /** Called by commands – key is already known (Mythic item id or material) */
    public boolean queueReward(UUID player, String key) {
        RewardGroupManager.RewardEntry entry = rewardGroupManager.getEntryForItem(key);
        if (entry == null) return false;
        playerRewardData.addReward(player, entry.groupName(), entry.reward());
        return true;
    }

    public boolean giveReward(Player player, String group, String itemId, ItemRegistry reg) {
        String rewardId = itemId == null
                ? playerRewardData.getAllRewards(player.getUniqueId(), group).stream().findFirst().orElse(null)
                : itemId;
        if (rewardId == null) return false;
        ItemStack reward = reg.getItem(rewardId);
        if (reward == null) return false;
        player.getInventory().addItem(reward);
        player.sendMessage("§aYou received your reward: " + rewardId);
        playerRewardData.removeReward(player.getUniqueId(), group, rewardId);
        return true;
    }

    public void giveAllRewards(Player player, String group, ItemRegistry reg) {
        List<String> list = playerRewardData.getAllRewards(player.getUniqueId(), group);
        for (String id : list) {
            ItemStack it = reg.getItem(id);
            if (it != null) player.getInventory().addItem(it);
        }
        playerRewardData.removeGroup(player.getUniqueId(), group);
    }

    /* ---------------------------------------------------------------------- */
    /*  Internal helpers                                                      */
    /* ---------------------------------------------------------------------- */

    private String resolveKey(ItemStack stack) {
        if (mythic.isMythicItem(stack)) return mythic.getMythicTypeFromItem(stack);
        return stack.getType().name();
    }
}
