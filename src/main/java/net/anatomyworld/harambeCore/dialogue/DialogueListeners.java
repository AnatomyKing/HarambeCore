package net.anatomyworld.harambeCore.dialogue;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class DialogueListeners implements Listener {

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Material type = event.getItem().getItemStack().getType();
        if (type.name().endsWith("_LOG")) {
            DialogueManager.startDialogue(player);
        }
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (DialogueManager.isDialogueActive(player)) {
            DialogueManager.nextLine(player);
            event.setCancelled(true);
        }
    }
}