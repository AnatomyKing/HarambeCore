package net.anatomyworld.harambeCore.item;

import org.bukkit.inventory.ItemStack;

public interface ItemRegistry {
    ItemStack getItem(String namespacedId);
    boolean isItemAvailable(String namespacedId);
}
