package net.anatomyworld.harambeCore.util;

import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
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

    /* ------------------------------------------------------------------
       Event: allow the specific Note Block to break, block everything else
       ------------------------------------------------------------------ */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.ADVENTURE) return;

        Block block = event.getBlock();

        // We only ever allow our special note block
        if (block.getType() == Material.NOTE_BLOCK) {
            // vanilla has already checked the CanBreak tag → let it proceed
            event.setExpToDrop(0);      // optional: suppress duplicated XP
            return;                     // DO NOT cancel
        }

        event.setCancelled(true);       // everything else stays protected
    }

    private boolean isPickaxe(Material mat) {
        return mat.name().endsWith("_PICKAXE");
    }

    /* ------------------------------------------------------------------
       Task: every second add the CanBreak tag to held pickaxes
       ------------------------------------------------------------------ */
    private void startInterceptTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    ServerPlayer nms = ((CraftPlayer) p).getHandle();
                    ItemStack held = nms.getMainHandItem();

                    if (!isPickaxe(held.getBukkitStack().getType())) continue;

                    ItemStack patched = injectCanBreak(held);

                    /* 1. Update server-side inventory copy (crucial) */
                    if (!ItemStack.matches(held, patched)) {
                        nms.setItemInHand(InteractionHand.MAIN_HAND, patched);
                    }

                    /* 2. Sync the hot-bar slot to the client */
                    int slot = 36 + nms.getInventory().selected;
                    nms.connection.send(new ClientboundContainerSetSlotPacket(
                            0, nms.containerMenu.getStateId(), slot, patched));
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);    // start after 1 s, repeat every 1 s
    }

    /* ------------------------------------------------------------------
       Helper: build CanBreak tag for note_block[instrument=harp,note=6,powered=false]
       ------------------------------------------------------------------ */
    private ItemStack injectCanBreak(ItemStack original) {
        // Skip if already tagged (cuts NBT spam)
        if (original.get(DataComponents.CAN_BREAK) != null) return original;

        ItemStack copy = original.copy();

        /* --- Build the state predicate --- */
        EnumProperty<NoteBlockInstrument> instrument = NoteBlock.INSTRUMENT;
        IntegerProperty note                    = NoteBlock.NOTE;
        BooleanProperty powered                 = NoteBlock.POWERED;

        StatePropertiesPredicate.Builder state = StatePropertiesPredicate.Builder.properties()
                .hasProperty(instrument, NoteBlockInstrument.BASEDRUM)
                .hasProperty(note, 6)
                .hasProperty(powered, false);

        /* --- Wrap it in a BlockPredicate --- */
        BlockPredicate predicate = BlockPredicate.Builder.block()
                .of(BuiltInRegistries.BLOCK, Blocks.NOTE_BLOCK)
                .setProperties(state)
                .build();

        AdventureModePredicate canBreak =
                new AdventureModePredicate(List.of(predicate), false);

        copy.set(DataComponents.CAN_BREAK, canBreak);
        return copy;
    }
}
