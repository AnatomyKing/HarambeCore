package net.anatomyworld.harambefd.guis.Krobus;

import net.anatomyworld.harambefd.GuiBuilder;
import org.bukkit.entity.Player;

public class KrobusGui {

    private final GuiBuilder guiBuilder;

    public KrobusGui(GuiBuilder guiBuilder) {
        this.guiBuilder = guiBuilder;
    }

    public void open(Player player) {
        guiBuilder.createAndOpenGui("krobus", player);  // No need to pass config, already managed in GuiBuilder
    }
}
