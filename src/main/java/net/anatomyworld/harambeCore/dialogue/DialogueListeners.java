// DialogueListeners.java
package net.anatomyworld.harambeCore.dialogue;

import io.lumine.mythic.api.items.ItemManager;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class DialogueListeners implements Listener {
    private final DialogueModule module;
    private final ItemManager mythic;

    public DialogueListeners(DialogueModule module) {
        this.module = module;
        this.mythic = MythicBukkit.inst().getItemManager();
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack stack = event.getItem().getItemStack();
        if (!mythic.isMythicItem(stack)) return;

        String key = mythic.getMythicTypeFromItem(stack);
        List<List<String>> pages = module.getPages(key);
        if (pages == null) return;

        if (!LuckPermsDialogueUtil.hasSeenDialogue(player, key)) {
            DialogueManager.startDialogue(player, pages, module.getTickDelay());
            LuckPermsDialogueUtil.markDialogueSeen(player, key);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!DialogueManager.isDialogueActive(player)) return;

        event.setCancelled(true);
        if (DialogueManager.isLooping(player)) {
            DialogueManager.nextLine(player);
        }
    }
}