package net.anatomyworld.harambefd.guis.anygui;

import net.anatomyworld.harambefd.GuiBuilder;
import org.bukkit.entity.Player;

public class InterfaceGui {

    private final GuiBuilder guiBuilder;

    public InterfaceGui(GuiBuilder guiBuilder) {
        this.guiBuilder = guiBuilder;
    }

    public void open(Player player) {
        guiBuilder.createAndOpenGui("interface", player);  // No need to pass config, already managed in GuiBuilder
    }
}
