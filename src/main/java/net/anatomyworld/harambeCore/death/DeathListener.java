package net.anatomyworld.harambeCore.death;

import net.anatomyworld.harambeCore.rewards.RewardHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
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
 * Saves death-drops into “death-&lt;victimUuid&gt;” reward group
 * and drops a PAPER that stores the owner UUID.
 */
public final class DeathListener implements Listener {

    private final JavaPlugin plugin;
    private final RewardHandler rewards;
    private static final NamespacedKey KEY =
            new NamespacedKey("harambe", "death_owner");

    public DeathListener(JavaPlugin plugin, RewardHandler rewards) {
        this.plugin  = plugin;
        this.rewards = rewards;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /* ------------------------------------------------------------------ */
    /*  Public helper - parse a death-key                                 */
    /* ------------------------------------------------------------------ */

    public static UUID getOwner(ItemStack stack) {
        if (stack == null || stack.getType() != Material.PAPER) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return null;

        String raw = meta.getPersistentDataContainer()
                .get(KEY, PersistentDataType.STRING);
        try { return raw == null ? null : UUID.fromString(raw); }
        catch (IllegalArgumentException ex) { return null; }
    }

    /* ------------------------------------------------------------------ */
    /*  Event - on death                                                  */
    /* ------------------------------------------------------------------ */

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        UUID    vid   = victim.getUniqueId();
        String  group = "death-" + vid;

        /* 1) stash every vanilla drop into the victim’s personal group */
        for (ItemStack drop : List.copyOf(e.getDrops())) {
            rewards.playerData().addStackReward(vid, group, drop);
        }
        e.getDrops().clear();   // nothing falls except key

        /* 2) drop the key */
        victim.getWorld().dropItemNaturally(
                victim.getLocation(), createKey(victim));
    }

    /* ------------------------------------------------------------------ */
    /*  Helper - craft the key                                            */
    /* ------------------------------------------------------------------ */

    private ItemStack createKey(Player owner) {
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta  meta  = paper.getItemMeta();

        meta.displayName(Component.text("Death Key ▶ " + owner.getName(),
                NamedTextColor.RED));
        meta.getPersistentDataContainer()
                .set(KEY, PersistentDataType.STRING, owner.getUniqueId().toString());

        paper.setItemMeta(meta);
        return paper;
    }
}
