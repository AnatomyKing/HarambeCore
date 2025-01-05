package net.anatomyworld.harambefd.guis.enderlink;

import net.anatomyworld.harambefd.GuiBuilder;
import org.bukkit.entity.Player;

import java.util.UUID;

public class EnderlinkGui {

    private final GuiBuilder guiBuilder;

    public EnderlinkGui(GuiBuilder guiBuilder) {
        this.guiBuilder = guiBuilder;
    }

    public void open(Player player) {
        String guiKey = "enderlink";
        guiBuilder.createAndOpenGui(guiKey, player);

        UUID playerId = player.getUniqueId();
        int page = EnderlinkMethods.getCurrentPage(playerId);
        EnderlinkMethods.loadPage(player, page, guiBuilder);
    }
}
