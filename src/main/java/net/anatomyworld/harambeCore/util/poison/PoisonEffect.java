package net.anatomyworld.harambeCore.util.poison;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PoisonEffect implements Listener {

    private final String poisonWorld;
    private final BlockData poisonBlockData;
    private final JavaPlugin plugin;
    private final Set<UUID> recentlyPoisoned = new HashSet<>();

    private static final int POISON_DURATION_TICKS = 200; // 10 seconds
    private static final int CUSTOM_AMPLIFIER = 7;

    public PoisonEffect(JavaPlugin plugin, String poisonWorld, BlockData poisonBlockData) {
        this.plugin = plugin;
        this.poisonWorld = poisonWorld;
        this.poisonBlockData = poisonBlockData;

        startPoisonCheckTask();
    }

    private void startPoisonCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld(poisonWorld);
                if (world == null) return;

                for (Player player : world.getPlayers()) {
                    Block block = player.getLocation().getBlock();
                    if (block.getType() == Material.TRIPWIRE && block.getBlockData().matches(poisonBlockData)) {
                        PotionEffect effect = player.getPotionEffect(PotionEffectType.POISON);
                        if (effect == null || effect.getAmplifier() != CUSTOM_AMPLIFIER || effect.getDuration() < 20) {
                            player.addPotionEffect(new PotionEffect(
                                    PotionEffectType.POISON,
                                    POISON_DURATION_TICKS,
                                    CUSTOM_AMPLIFIER,
                                    true, false
                            ));
                            recentlyPoisoned.add(player.getUniqueId());
                        }

                        if (player.getHealth() <= 1.0) {
                            player.setHealth(0.0);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // check every 1 second
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID id = player.getUniqueId();
        EntityDamageEvent cause = player.getLastDamageCause();

        if (cause != null && cause.getCause() == EntityDamageEvent.DamageCause.POISON) {
            if (recentlyPoisoned.remove(id)) {
                event.deathMessage(Component.text(player.getName() + " melted in acid."));
            }
        }
    }
}
