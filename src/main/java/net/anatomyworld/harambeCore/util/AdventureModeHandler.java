package net.anatomyworld.harambeCore.util;

import net.anatomyworld.harambeCore.config.YamlConfigLoader;
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
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class AdventureModeHandler implements Listener {
    private final JavaPlugin plugin;
    private final List<BlockPredicate> allowedPredicates = new ArrayList<>();
    private final long tickDelay;

    public AdventureModeHandler(JavaPlugin plugin) {
        this.plugin = plugin;

        // 1️⃣ Load config
        FileConfiguration cfg = YamlConfigLoader.load(plugin, "util/adventure-breakblock.yml");
        this.tickDelay = cfg.getLong("tick-delay", 20L);

        ConfigurationSection sec = cfg.getConfigurationSection("note-blocks");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                ConfigurationSection entry = sec.getConfigurationSection(key);
                if (entry == null) continue;

                try {
                    NoteBlockInstrument inst = NoteBlockInstrument.valueOf(entry.getString("instrument", "").toUpperCase());
                    int note  = entry.getInt("note");
                    boolean powered = entry.getBoolean("powered", false);
                    allowedPredicates.add(makePredicate(inst, note, powered));
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("[AdventureBreak] invalid entry '" + key + "': " + ex.getMessage());
                }
            }
        }

        plugin.getLogger().info("[AdventureBreak] allowing " + allowedPredicates.size() + " note-block variants");
        startInterceptTask();
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.ADVENTURE) return;

        Block block = event.getBlock();
        if (block.getType() == Material.NOTE_BLOCK) {
            // let through, vanilla checks our CAN_BREAK tag
            event.setExpToDrop(0);
            return;
        }
        event.setCancelled(true);
    }

    private boolean isPickaxe(Material mat) {
        return mat.name().endsWith("_PICKAXE");
    }

    private void startInterceptTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    ServerPlayer nms = ((CraftPlayer) p).getHandle();
                    ItemStack held = nms.getMainHandItem();
                    if (!isPickaxe(held.getBukkitStack().getType())) continue;

                    ItemStack patched = injectCanBreak(held);
                    if (!ItemStack.matches(held, patched)) {
                        nms.setItemInHand(InteractionHand.MAIN_HAND, patched);
                    }

                    int slot = 36 + nms.getInventory().selected;
                    nms.connection.send(new ClientboundContainerSetSlotPacket(
                            0, nms.containerMenu.getStateId(), slot, patched));
                }
            }
        }.runTaskTimer(plugin, 20L, tickDelay);
    }

    private ItemStack injectCanBreak(ItemStack original) {
        if (original.get(DataComponents.CAN_BREAK) != null) return original;

        if (allowedPredicates.isEmpty()) {
            return original; // ✅ Don't inject empty CAN_BREAK list
        }

        ItemStack copy = original.copy();
        AdventureModePredicate amp = new AdventureModePredicate(allowedPredicates, false);
        copy.set(DataComponents.CAN_BREAK, amp);
        return copy;
    }

    private BlockPredicate makePredicate(NoteBlockInstrument inst, int noteVal, boolean powered) {
        EnumProperty<NoteBlockInstrument> instrProp = NoteBlock.INSTRUMENT;
        IntegerProperty noteProp               = NoteBlock.NOTE;
        BooleanProperty poweredProp            = NoteBlock.POWERED;

        StatePropertiesPredicate.Builder state = StatePropertiesPredicate.Builder.properties()
                .hasProperty(instrProp, inst)
                .hasProperty(noteProp, noteVal)
                .hasProperty(poweredProp, powered);

        return BlockPredicate.Builder.block()
                .of(BuiltInRegistries.BLOCK, Blocks.NOTE_BLOCK)
                .setProperties(state)
                .build();
    }
}
