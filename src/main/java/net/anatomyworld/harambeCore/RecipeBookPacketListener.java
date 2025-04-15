package net.anatomyworld.harambeCore;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import net.minecraft.network.protocol.game.ClientboundRecipeBookSettingsPacket;
import net.minecraft.network.protocol.game.ServerboundRecipeBookChangeSettingsPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.stats.RecipeBookSettings;
import net.minecraft.world.inventory.RecipeBookType;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer; // Adjust import for your MC version
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public class RecipeBookPacketListener implements Listener {

    private final Plugin plugin;
    private final String recipeBookCommand;  // The configured command

    public RecipeBookPacketListener(Plugin plugin, String recipeBookCommand) {
        this.plugin = plugin;
        this.recipeBookCommand = recipeBookCommand;

        // Inject all currently online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            inject(player);
        }

        // Register event listeners
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        inject(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        uninject(event.getPlayer());
    }

    public void shutdown() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            uninject(player);
        }
    }

    private void inject(Player player) {
        if (!(player instanceof CraftPlayer craftPlayer)) {
            return;
        }

        ServerGamePacketListenerImpl connection = craftPlayer.getHandle().connection;
        ChannelPipeline pipeline = connection.connection.channel.pipeline();

        // Only inject once
        if (pipeline.get("recipe_book_toggle_handler") != null) {
            return;
        }

        pipeline.addBefore("packet_handler", "recipe_book_toggle_handler", new ChannelDuplexHandler() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                if (msg instanceof ServerboundRecipeBookChangeSettingsPacket packet) {
                    // Check if it's the crafting recipe book
                    if (packet.getBookType() == RecipeBookType.CRAFTING) {
                        // Also verify they're in their normal inventory, if you want
                        if (player.getOpenInventory().getType() == InventoryType.CRAFTING) {
                            // Run your custom logic on the main thread
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                // 1) Perform your command from config
                                player.performCommand(recipeBookCommand);

                                // 2) Force the client to close the *crafting* recipe book
                                forceCloseClientRecipeBook(player);

                                // 3) Optionally refresh their inventory
                                player.updateInventory();
                            });

                            // Stop further processing
                            return;
                        }
                    }
                }

                // Otherwise, pass it on normally
                super.channelRead(ctx, msg);
            }
        });
    }

    private void uninject(Player player) {
        if (!(player instanceof CraftPlayer craftPlayer)) {
            return;
        }

        ServerGamePacketListenerImpl connection = craftPlayer.getHandle().connection;
        ChannelPipeline pipeline = connection.connection.channel.pipeline();

        if (pipeline.get("recipe_book_toggle_handler") != null) {
            pipeline.remove("recipe_book_toggle_handler");
        }
    }

    /**
     * Sends a packet telling the client that its CRAFTING recipe book is closed.
     */
    private void forceCloseClientRecipeBook(Player player) {
        if (!(player instanceof CraftPlayer craftPlayer)) return;

        ServerGamePacketListenerImpl connection = craftPlayer.getHandle().connection;

        // Create recipe settings with CRAFTING set to "closed"
        RecipeBookSettings forcedClosedSettings = new RecipeBookSettings();
        forcedClosedSettings.setOpen(RecipeBookType.CRAFTING, false);
        forcedClosedSettings.setFiltering(RecipeBookType.CRAFTING, false);

        // Build the packet
        ClientboundRecipeBookSettingsPacket closePacket =
                new ClientboundRecipeBookSettingsPacket(forcedClosedSettings);

        // Send to the client
        connection.send(closePacket);
    }
}
