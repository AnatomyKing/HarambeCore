package net.anatomyworld.harambeCore.dialogue;

import net.anatomyworld.harambeCore.HarambeCore;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class BossBarTypingSession {
    private final Player player;
    private final List<List<String>> pages;
    private final long tickDelay; // delay between frames
    private int pageIndex = 0;
    private int frameIndex = 0;
    private BossBar bossBar;
    private BukkitRunnable task;

    public BossBarTypingSession(Player player, List<List<String>> pages, long tickDelay) {
        this.player = player;
        this.pages = pages;
        this.tickDelay = tickDelay;
    }

    public void start() {
        this.bossBar = BossBar.bossBar(Component.empty(), 1.0f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
        bossBar.addViewer(player);
        displayNextPage();
    }

    private void displayNextPage() {
        if (pageIndex >= pages.size()) {
            stop();
            return;
        }

        List<String> frames = pages.get(pageIndex);
        frameIndex = 0;

        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (frameIndex >= frames.size()) {
                    cancel();
                    return;
                }

                String frame = frames.get(frameIndex++);
                bossBar.name(Component.text(frame));
            }
        };
        task.runTaskTimer(HarambeCore.getPlugin(HarambeCore.class), 0L, tickDelay);
    }

    public void next() {
        if (task != null) task.cancel();
        pageIndex++;
        displayNextPage();
    }

    public void stop() {
        if (task != null) task.cancel();
        if (bossBar != null) bossBar.removeViewer(player);
    }
}