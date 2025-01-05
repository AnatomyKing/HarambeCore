package net.anatomyworld.harambefd.guis.Spawn;

import net.anatomyworld.harambefd.GuiBuilder;
import org.bukkit.entity.Player;

public class SpawnGui {

    private final GuiBuilder guiBuilder;

    public SpawnGui(GuiBuilder guiBuilder) {
        this.guiBuilder = guiBuilder;
    }

    public void open(Player player) {
        guiBuilder.createAndOpenGui("spawn", player);
    }
}
