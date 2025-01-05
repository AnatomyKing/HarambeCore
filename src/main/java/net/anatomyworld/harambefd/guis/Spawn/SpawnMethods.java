package net.anatomyworld.harambefd.guis.Spawn;

import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class SpawnMethods {

    public static void handleButtonClick(Player player, String buttonKey, Map<Integer, ItemStack> consumedItems) {
        if (buttonKey.equals("pig_button")) {
            spawnPig(player);
        } else {
            player.sendMessage("Unknown button clicked.");
        }
    }

    private static void spawnPig(Player player) {
        Pig pig = (Pig) player.getWorld().spawn(player.getLocation(), Pig.class);
        player.sendMessage("You spawned a pig!");
    }
}
