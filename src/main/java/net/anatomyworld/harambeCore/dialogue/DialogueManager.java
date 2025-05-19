package net.anatomyworld.harambeCore.dialogue;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DialogueManager {
    private static final Map<UUID, BossBarTypingSession> sessions = new HashMap<>();

    public static void startDialogue(Player player, java.util.List<java.util.List<String>> pages, long tickDelay) {
        UUID uid = player.getUniqueId();
        if (sessions.containsKey(uid)) return;
        BossBarTypingSession session = new BossBarTypingSession(player, pages, tickDelay);
        sessions.put(uid, session);
        session.start();
    }

    public static void nextLine(Player player) {
        BossBarTypingSession session = sessions.get(player.getUniqueId());
        if (session != null) session.next();
    }

    public static void clear(Player player) {
        BossBarTypingSession session = sessions.remove(player.getUniqueId());
        if (session != null) session.stop();
    }

    public static boolean isDialogueActive(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public static boolean isLooping(Player player) {
        BossBarTypingSession session = sessions.get(player.getUniqueId());
        return session != null && session.isLooping();
    }
}

