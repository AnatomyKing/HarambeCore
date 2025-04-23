package net.anatomyworld.harambeCore.item;

import org.bukkit.inventory.ItemStack;

public interface ItemRegistry {
    ItemStack getItem(String namespacedId); // e.g., mythicmob:item1
    boolean isItemAvailable(String namespacedId);
}
