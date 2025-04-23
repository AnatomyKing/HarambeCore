package net.anatomyworld.harambeCore.item;

import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.items.MythicItem;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class MythicMobsRegistry implements ItemRegistry {

    @Override
    public ItemStack getItem(String namespacedId) {
        Optional<MythicItem> mythicItem = MythicBukkit.inst().getItemManager().getItem(namespacedId);
        return mythicItem
                .map(item -> BukkitAdapter.adapt(item.generateItemStack(1))) // convert AbstractItemStack to Bukkit ItemStack
                .orElse(null);
    }

    @Override
    public boolean isItemAvailable(String namespacedId) {
        return MythicBukkit.inst().getItemManager().getItem(namespacedId).isPresent();
    }
}
