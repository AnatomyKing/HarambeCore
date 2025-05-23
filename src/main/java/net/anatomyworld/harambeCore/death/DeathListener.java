package net.anatomyworld.harambeCore.death;

import net.anatomyworld.harambeCore.rewards.RewardHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
 * Saves drops into “death-&lt;group&gt;-&lt;victimUuid&gt;”
 * and drops a PAPER key storing owner + group.
 */
public final class DeathListener implements Listener {

    /* ─── PersistentData keys ───────────────────────────────────── */
    private static final NamespacedKey KEY_OWNER =
            new NamespacedKey("harambe", "death_owner");
    private static final NamespacedKey KEY_GROUP =
            new NamespacedKey("harambe", "death_group");

    private final JavaPlugin   plugin;
    private final RewardHandler rewards;

    public DeathListener(JavaPlugin plugin, RewardHandler rewards) {
        this.plugin  = plugin;
        this.rewards = rewards;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /* ───────────────────────── Public helpers ──────────────────── */

    /** Parsed contents of a death key */
    public record KeyInfo(UUID owner, String group) {}

    /** @return info if valid paper key, else null */
    public static KeyInfo readKey(ItemStack stack) {
        if (stack == null || stack.getType() != Material.PAPER) return null;
        ItemMeta meta = stack.getItemMeta();  if (meta == null) return null;

        String rawOwner = meta.getPersistentDataContainer()
                .get(KEY_OWNER,  PersistentDataType.STRING);
        String rawGroup = meta.getPersistentDataContainer()
                .get(KEY_GROUP,  PersistentDataType.STRING);
        if (rawOwner == null || rawGroup == null) return null;

        try { return new KeyInfo(UUID.fromString(rawOwner), rawGroup); }
        catch (IllegalArgumentException ex) { return null; }
    }

    /* ───────────────────────── Event hook ───────────────────────── */

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        UUID vid = victim.getUniqueId();

        // Skip if keepInventory is true in this world
        Boolean keepInv = victim.getWorld().getGameRuleValue(GameRule.KEEP_INVENTORY);
        if (Boolean.TRUE.equals(keepInv)) {
            return;
        }

        String mvGroup = WorldGroupHelper.getGroupName(victim.getWorld());
        String rewardGroup = "death-" + mvGroup + "-" + vid;

        boolean queueExists = !rewards.playerData().getStackRewards(vid, rewardGroup).isEmpty();

        if (queueExists && !rewards.playerData().isExpired(vid, rewardGroup)) {
            return; // Let vanilla drops occur if not expired
        }

        if (queueExists) {
            rewards.playerData().removeGroup(vid, rewardGroup);
        }

        List<ItemStack> drops = List.copyOf(e.getDrops());
        if (drops.isEmpty()) return;

        drops.forEach(d -> rewards.playerData().addStackReward(vid, rewardGroup, d));
        rewards.playerData().setExpiry(vid, rewardGroup);

        e.getDrops().clear();

        victim.getWorld().dropItemNaturally(
                victim.getLocation(),
                createKey(victim, mvGroup)
        );

        WorldGroupHelper.broadcastDeathKey(victim, mvGroup);
    }




    /* ───────────────────────── Internals ───────────────────────── */

    private ItemStack createKey(Player owner, String mvGroup) {
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta  meta  = paper.getItemMeta();

        meta.displayName(Component.text(
                "Death Key ▶ " + owner.getName() + " [" + mvGroup + ']',
                NamedTextColor.RED));

        meta.getPersistentDataContainer().set(KEY_OWNER,
                PersistentDataType.STRING, owner.getUniqueId().toString());
        meta.getPersistentDataContainer().set(KEY_GROUP,
                PersistentDataType.STRING, mvGroup);

        paper.setItemMeta(meta);
        return paper;
    }
}
