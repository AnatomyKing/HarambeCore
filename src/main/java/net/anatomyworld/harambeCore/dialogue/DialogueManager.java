package net.anatomyworld.harambeCore.dialogue;

import org.bukkit.entity.Player;

import java.util.*;

public class DialogueManager {
    private static final Map<UUID, BossBarTypingSession> sessions = new HashMap<>();

    private static final List<List<String>> LOG_PAGES = List.of(
            // Page 1: outputcut/1.png to outputcut/16.png + 16-a and 16-b
            List.of(
                    "끘\uF80C\uF80B\uF801뀺\uF80C",  // outputcut/1.png
                    "끙\uF80C\uF80B\uF801뀻\uF80C",  // outputcut/2.png
                    "끘\uF80C\uF80B\uF801뀼\uF80C",  // outputcut/3.png
                    "끙\uF80C\uF80B\uF801뀽\uF80C",  // outputcut/4.png
                    "끘\uF80C\uF80B\uF801뀾\uF80C",  // outputcut/5.png
                    "끙\uF80C\uF80B\uF801뀿\uF80C",  // outputcut/6.png
                    "끘\uF80C\uF80B\uF801끀\uF80C",  // outputcut/7.png
                    "끙\uF80C\uF80B\uF801끁\uF80C",  // outputcut/8.png
                    "끘\uF80C\uF80B\uF801끂\uF80C",  // outputcut/9.png
                    "끙\uF80C\uF80B\uF801끃\uF80C",  // outputcut/10.png
                    "끘\uF80C\uF80B\uF801끄\uF80C",  // outputcut/11.png
                    "끙\uF80C\uF80B\uF801끅\uF80C",  // outputcut/12.png
                    "끘\uF80C\uF80B\uF801끆\uF80C",  // outputcut/13.png
                    "끙\uF80C\uF80B\uF801끇\uF80C",  // outputcut/14.png
                    "끘\uF80C\uF80B\uF801끈\uF80C",  // outputcut/15.png
                    "끙\uF80C\uF80B\uF801끉\uF80C",  // outputcut/16.png
                    "끘\uF80C\uF80B\uF801끊\uF80C",  // outputcut/16-a.png
                    "끚\uF80C\uF80B\uF801끋\uF80C"   // outputcut/16-b.png
            ),

            // Page 2: outputcut/17.png to outputcut/26.png + 26-a and 26-b
            List.of(
                    "끘\uF80C\uF80B\uF801끌\uF80C",  // outputcut/17.png
                    "끙\uF80C\uF80B\uF801끍\uF80C",  // outputcut/18.png
                    "끘\uF80C\uF80B\uF801끎\uF80C",  // outputcut/19.png
                    "끙\uF80C\uF80B\uF801끏\uF80C",  // outputcut/20.png
                    "끘\uF80C\uF80B\uF801끐\uF80C",  // outputcut/21.png
                    "끙\uF80C\uF80B\uF801끑\uF80C",  // outputcut/22.png
                    "끘\uF80C\uF80B\uF801끒\uF80C",  // outputcut/23.png
                    "끙\uF80C\uF80B\uF801끓\uF80C",  // outputcut/24.png
                    "끘\uF80C\uF80B\uF801끔\uF80C",  // outputcut/25.png
                    "끙\uF80C\uF80B\uF801끕\uF80C",  // outputcut/26.png
                    "끘\uF80C\uF80B\uF801끖\uF80C",  // outputcut/26-a.png
                    "끚\uF80C\uF80B\uF801끗\uF80C"   // outputcut/26-b.png
            )
    );


    private static final long DEFAULT_TICK_SPEED = 5L;

    public static void startDialogue(Player player) {
        if (sessions.containsKey(player.getUniqueId())) return;
        BossBarTypingSession session = new BossBarTypingSession(player, LOG_PAGES, DEFAULT_TICK_SPEED);
        sessions.put(player.getUniqueId(), session);
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