package net.anatomyworld.harambeCore.util;

import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.AdventureModePredicate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class AdventureModeHandler implements Listener {

    private final Plugin plugin;

    public AdventureModeHandler(Plugin plugin) {
        this.plugin = plugin;
        startInterceptTask();
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.ADVENTURE) return;

        Block block = event.getBlock();
        Material tool = player.getInventory().getItemInMainHand().getType();

        if (block.getType() == Material.NOTE_BLOCK &&
                block.getBlockData().getAsString().equals("minecraft:note_block[instrument=harp,note=6,powered=false]") &&
                isPickaxe(tool)) {
            block.breakNaturally(player.getInventory().getItemInMainHand());
            event.setCancelled(true);
        } else {
            event.setCancelled(true);
        }
    }

    private boolean isPickaxe(Material material) {
        return material.name().endsWith("_PICKAXE");
    }

    private void startInterceptTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    ServerPlayer nms = ((CraftPlayer) player).getHandle();
                    ItemStack held = nms.getMainHandItem();

                    if (isPickaxe(held.getBukkitStack().getType())) {
                        ItemStack patched = injectCanBreak(held);
                        int slot = 36 + nms.getInventory().selected;
                        nms.connection.send(new ClientboundContainerSetSlotPacket(
                                0, nms.containerMenu.getStateId(), slot, patched
                        ));
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // every second
    }

    private ItemStack injectCanBreak(ItemStack original) {
        ItemStack copy = original.copy();

        // Access property references
        EnumProperty<NoteBlockInstrument> instrument = NoteBlock.INSTRUMENT;
        IntegerProperty note = NoteBlock.NOTE;
        BooleanProperty powered = NoteBlock.POWERED;

        // Build a state predicate
        StatePropertiesPredicate.Builder state = StatePropertiesPredicate.Builder.properties()
                .hasProperty(instrument, NoteBlockInstrument.HARP)
                .hasProperty(note, 6)
                .hasProperty(powered, false);

        // Build block predicate
        BlockPredicate predicate = BlockPredicate.Builder.block()
                .of(BuiltInRegistries.BLOCK, Blocks.NOTE_BLOCK)
                .setProperties(state)
                .build();

        // Apply can_break tag
        AdventureModePredicate canBreak = new AdventureModePredicate(List.of(predicate), false);
        copy.set(DataComponents.CAN_BREAK, canBreak);

        return copy;
    }
}
