package net.anatomyworld.harambeCore.dialogue;

import org.bukkit.entity.Player;

import java.util.*;

public class DialogueManager {
    private static final Map<UUID, BossBarTypingSession> sessions = new HashMap<>();

    private static final List<List<String>> LOG_PAGES = List.of(
            // Page 1: outputcut/1.png to outputcut/16.png
            List.of(
                    "뀺", // outputcut/1.png
                    "뀻", // outputcut/2.png
                    "뀼", // outputcut/3.png
                    "뀽", // outputcut/4.png
                    "뀾", // outputcut/5.png
                    "뀿", // outputcut/6.png
                    "끀", // outputcut/7.png
                    "끁", // outputcut/8.png
                    "끂", // outputcut/9.png
                    "끃", // outputcut/10.png
                    "끄", // outputcut/11.png
                    "끅", // outputcut/12.png
                    "끆", // outputcut/13.png
                    "끇", // outputcut/14.png
                    "끈", // outputcut/15.png
                    "끉"  // outputcut/16.png
            ),

            // Page 2: outputcut/17.png to outputcut/27.png
            List.of(
                    "끊", // outputcut/17.png
                    "끋", // outputcut/18.png
                    "끌", // outputcut/19.png
                    "끍", // outputcut/20.png
                    "끎", // outputcut/21.png
                    "끏", // outputcut/22.png
                    "끐", // outputcut/23.png
                    "끑", // outputcut/24.png
                    "끒", // outputcut/25.png
                    "끓", // outputcut/26.png
                    "끔"  // outputcut/27.png
            )
    );


    private static final long DEFAULT_TICK_SPEED = 3L;

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
}