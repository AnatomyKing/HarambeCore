/* net.anatomyworld.harambeCore.death.DeathListener */
package net.anatomyworld.harambeCore.death;

import net.anatomyworld.harambeCore.rewards.RewardHandler;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;

/**
 * Queues drops, spawns Mythic-Crucible death-chest furniture and broadcasts
 * the location.  If the furniture is manually broken, DeathChestManager
 * drops a key that others can steal.
 */
public final class DeathListener implements Listener {

    /* ─── PDC keys (shared with DeathKeyBuilder & DeathChestManager) ── */
    public static final NamespacedKey KEY_OWNER =
            new NamespacedKey("harambe", "death_owner");
    public static final NamespacedKey KEY_GROUP =
            new NamespacedKey("harambe", "death_group");

    private final JavaPlugin        plugin;
    private final RewardHandler     rewards;
    private final DeathChestModule  cfg;
    private final DeathChestManager chestMgr;

    public DeathListener(JavaPlugin plugin,
                         RewardHandler rewards,
                         DeathChestModule cfg,
                         DeathChestManager chestMgr) {

        this.plugin   = plugin;
        this.rewards  = rewards;
        this.cfg      = cfg;
        this.chestMgr = chestMgr;

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /* ────────────────────── key-parsing helper ────────────────────── */
    public record KeyInfo(UUID owner, String group) { }

    public static KeyInfo readKey(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return null;
        ItemMeta meta = stack.getItemMeta();  if (meta == null) return null;

        String rawOwner = meta.getPersistentDataContainer()
                .get(KEY_OWNER,  PersistentDataType.STRING);
        String rawGroup = meta.getPersistentDataContainer()
                .get(KEY_GROUP,  PersistentDataType.STRING);
        if (rawOwner == null || rawGroup == null) return null;

        try { return new KeyInfo(UUID.fromString(rawOwner), rawGroup); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    /* ────────────────────── main event hook ──────────────────────── */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {

        Player victim = e.getEntity();

        // honour keepInventory
        if (Boolean.TRUE.equals(
                victim.getWorld().getGameRuleValue(GameRule.KEEP_INVENTORY)))
            return;

        UUID   vid         = victim.getUniqueId();
        String mvGroup     = WorldGroupHelper.getGroupName(victim.getWorld());
        String rewardGroup = "death-" + mvGroup + "-" + vid;

        /* queue already exists and is still valid → let vanilla drops through */
        if (!rewards.playerData().getStackRewards(vid, rewardGroup).isEmpty()
                && !rewards.playerData().isExpired(vid, rewardGroup))
            return;

        /* purge an expired queue */
        if (rewards.playerData().isExpired(vid, rewardGroup))
            rewards.playerData().removeGroup(vid, rewardGroup);

        /* capture the fresh drops */
        List<ItemStack> drops = List.copyOf(e.getDrops());
        if (drops.isEmpty()) return;

        drops.forEach(d -> rewards.playerData().addStackReward(vid, rewardGroup, d));
        rewards.playerData().setExpiry(vid, rewardGroup);
        e.getDrops().clear();

        /* spawn Mythic-Crucible furniture */
        chestMgr.spawnChest(victim, mvGroup, rewardGroup);

        /* broadcast */
        WorldGroupHelper.broadcastDeathKey(victim, mvGroup, cfg);
    }
}
