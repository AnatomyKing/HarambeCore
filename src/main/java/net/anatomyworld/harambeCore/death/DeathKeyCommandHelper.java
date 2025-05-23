package net.anatomyworld.harambeCore.death;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Helper for the /harambe give deathkey command.
 */
public final class DeathKeyCommandHelper {

    private DeathKeyCommandHelper() {}

    /**
     * Builds a death key for the given owner and group, and gives it to target.
     *
     * @param target    the player to receive the key
     * @param ownerName the name of the original owner (must have joined the server before)
     * @param group     the world/reward group (pass null or empty for "ungrouped")
     * @param cfg       the DeathChestModule for config
     */
    public static void giveKeyToPlayer(Player target,
                                       String ownerName,
                                       String group,
                                       DeathChestModule cfg) {
        if (group == null || group.isBlank()) {
            group = "ungrouped";
        }

        // Resolve owner UUID/name
        OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerName);
        UUID ownerId = owner.getUniqueId();
        String resolvedOwnerName = owner.getName() != null ? owner.getName() : ownerName;

        // Build the key
        ItemStack key = DeathKeyBuilder.build(ownerId, resolvedOwnerName, group, cfg);

        // Give to target
        target.getInventory().addItem(key);
    }
}
