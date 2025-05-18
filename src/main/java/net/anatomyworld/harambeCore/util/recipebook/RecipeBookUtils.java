package net.anatomyworld.harambeCore.util.recipebook;

import net.minecraft.network.protocol.game.ClientboundRecipeBookSettingsPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.stats.RecipeBookSettings;
import net.minecraft.world.inventory.RecipeBookType;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class RecipeBookUtils {

    /**
     * Sends a packet telling the client that its CRAFTING recipe book is closed.
     */
    public static void forceCloseClientRecipeBook(Player player) {
        if (!(player instanceof CraftPlayer craftPlayer)) return;

        ServerGamePacketListenerImpl connection = craftPlayer.getHandle().connection;

        RecipeBookSettings forcedClosedSettings = new RecipeBookSettings();
        forcedClosedSettings.setOpen(RecipeBookType.CRAFTING, false);
        forcedClosedSettings.setFiltering(RecipeBookType.CRAFTING, false);

        ClientboundRecipeBookSettingsPacket closePacket =
                new ClientboundRecipeBookSettingsPacket(forcedClosedSettings);

        connection.send(closePacket);
    }
}
