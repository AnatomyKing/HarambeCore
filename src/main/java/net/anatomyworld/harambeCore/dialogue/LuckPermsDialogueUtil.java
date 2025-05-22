// LuckPermsDialogueUtil.java
package net.anatomyworld.harambeCore.dialogue;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeEqualityPredicate;
import net.luckperms.api.util.Tristate;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Utility for checking and marking dialogues as seen via LuckPerms API.
 * Uses UserManager#modifyUser to handle loading, modification, and saving asynchronously.
 */
public final class LuckPermsDialogueUtil {
    private static LuckPerms luckPerms;
    private static UserManager userManager;

    private LuckPermsDialogueUtil() {
        // static utility
    }

    /**
     * Initialize LuckPerms API references. Call once in your plugin's onEnable().
     */
    public static void init(JavaPlugin plugin) {
        try {
            luckPerms = LuckPermsProvider.get();
            userManager = luckPerms.getUserManager();
        } catch (IllegalStateException e) {
            plugin.getLogger().warning(
                    "LuckPerms not found: dialogue-seen permissions will not persist.");
            luckPerms = null;
        }
    }

    /**
     * @return true if player has already seen dialogue for this key
     */
    public static boolean hasSeenDialogue(Player player, String key) {
        String perm = "harambecore.dialogue.seen." + key;
        return player.hasPermission(perm);
    }

    /**
     * Marks dialogue seen by adding a permission node via LP's modifyUser API.
     */
    public static void markDialogueSeen(Player player, String key) {
        if (luckPerms == null) return;
        String perm = "harambecore.dialogue.seen." + key;
        Node node = Node.builder(perm).value(true).build();

        userManager.modifyUser(player.getUniqueId(), user -> {
            Tristate seen = user.data().contains(node, NodeEqualityPredicate.EXACT);
            if (seen != Tristate.TRUE) {
                user.data().add(node);
            }
        });
    }
}