package net.anatomyworld.harambeCore.util.poison;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PoisonEffect implements Listener {

    private final String poisonWorld;
    private final BlockData poisonBlockData;
    private static final int INFINITE_DURATION = 1000000;  // 1,000,000 ticks, roughly 13.8 hours

    public PoisonEffect(String poisonWorld, BlockData poisonBlockData) {
        this.poisonWorld = poisonWorld;
        this.poisonBlockData = poisonBlockData;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();

        // Only check if the player is in the correct world
        if (!world.getName().equals(poisonWorld)) {
            return;
        }

        // Check the block the player is currently standing on
        Block block = player.getLocation().getBlock();

        // Check if the player is on the tripwire block
        if (block.getType() == Material.TRIPWIRE && block.getBlockData().matches(poisonBlockData)) {
            // Check if the player already has a poison effect with long duration
            PotionEffect currentEffect = player.getPotionEffect(PotionEffectType.POISON);
            if (currentEffect == null || currentEffect.getDuration() < INFINITE_DURATION) {
                // Apply a long-duration poison effect (1,000,000 ticks)
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, INFINITE_DURATION, 7, true, false));  // Long poison
            }

            // Lethal poison: Kill the player if their health drops to 1 or below
            if (player.getHealth() <= 1.0) {
                player.setHealth(0.0);  // Kill the player
            }

        } else {
            // Player is no longer on the tripwire block, remove poison effect if they have it
            if (player.hasPotionEffect(PotionEffectType.POISON)) {
                player.removePotionEffect(PotionEffectType.POISON);
            }
        }
    }

    // Event listener for customizing death message using Components
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        // Check if the player died from poison
        if (player.getLastDamageCause() != null && player.getLastDamageCause().getCause().equals(org.bukkit.event.entity.EntityDamageEvent.DamageCause.POISON)) {
            // Create the custom death message using Adventure Components
            Component deathMessage = Component.text(player.getName() + " Melted in Acid.");
            event.deathMessage(deathMessage); // Set the custom death message
        }
    }
}
