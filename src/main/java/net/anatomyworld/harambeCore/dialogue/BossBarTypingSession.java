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
    private final long tickDelay;
    private int pageIndex = 0, frameIndex = 0;
    private BossBar bossBar;
    private BukkitRunnable task;
    private boolean loopDirection = true, loopStarted = false;

    public BossBarTypingSession(Player player, List<List<String>> pages, long tickDelay) {
        this.player = player;
        this.pages = pages;
        this.tickDelay = tickDelay;
    }

    public void start() {
        bossBar = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
        bossBar.addViewer(player);
        displayNextPage();
    }

    private void displayNextPage() {
        if (pageIndex >= pages.size()) {
            stop();
            DialogueManager.clear(player); // ✅ Ensure session is removed
            return;
        }

        List<String> frames = pages.get(pageIndex);
        frameIndex = 0;
        loopDirection = true;
        loopStarted = false;

        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (frameIndex < frames.size()) {
                    bossBar.name(Component.text(frames.get(frameIndex++)));
                    if (frameIndex == frames.size()) loopStarted = true;
                } else if (frames.size() >= 2) {
                    frameIndex = frames.size() - (loopDirection ? 2 : 1);
                    loopDirection = !loopDirection;
                    bossBar.name(Component.text(frames.get(frameIndex)));
                }
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

    public boolean isLooping() {
        return loopStarted;
    }
}
