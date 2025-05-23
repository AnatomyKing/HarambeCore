package net.anatomyworld.harambeCore.death;

import io.lumine.mythiccrucible.MythicCrucible;
import io.lumine.mythiccrucible.events.MythicFurnitureRemoveEvent;
import io.lumine.mythiccrucible.items.CrucibleItem;
import io.lumine.mythiccrucible.items.furniture.Furniture;
import io.lumine.mythiccrucible.items.furniture.FurnitureItemContext;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Spawns & tracks Mythic-Crucible death-chests, handles expiry
 * and drops a Death-Key on manual break.
 */
public final class DeathChestManager implements Listener {

    private final String crucibleId;
    private final JavaPlugin plugin;
    private final DeathChestModule cfg;
    private final Map<UUID, BukkitTask> expiryTasks = new HashMap<>();
    private final Set<UUID> expiredNaturally = new HashSet<>(); // Tracks auto-expired furniture

    public DeathChestManager(JavaPlugin plugin, DeathChestModule cfg) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.crucibleId = cfg.furnitureItemId();

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Place the configured Crucible furniture chest (or just drop the key if the item is missing).
     */
    public void spawnChest(Player owner, String mvGroup, String rewardGroup) {
        Optional<CrucibleItem> opt = MythicCrucible.inst()
                .getItemManager()
                .getItem(crucibleId);

        if (opt.isEmpty() || opt.get().getFurnitureData() == null) {
            owner.getWorld().dropItemNaturally(
                    owner.getLocation(),
                    DeathKeyBuilder.build(owner.getUniqueId(),
                            owner.getName(), mvGroup, cfg)
            );
            return;
        }

        FurnitureItemContext fic = opt.get().getFurnitureData();
        Block base = owner.getLocation().getBlock();

        Furniture furniture = fic.placeForced(
                base,
                BlockFace.UP,
                owner.getLocation().getYaw(),
                null
        );

        if (furniture == null) {
            owner.getWorld().dropItemNaturally(
                    owner.getLocation(),
                    DeathKeyBuilder.build(owner.getUniqueId(),
                            owner.getName(), mvGroup, cfg)
            );
            return;
        }

        // Tag ownership
        furniture.getFrame().getPersistentDataContainer().set(
                DeathListener.KEY_OWNER,
                PersistentDataType.STRING,
                owner.getUniqueId().toString());

        furniture.getFrame().getPersistentDataContainer().set(
                DeathListener.KEY_GROUP,
                PersistentDataType.STRING,
                mvGroup);

        // Schedule expiry
        long ticks = cfg.expiryMinutes() * 60L * 20L;
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (furniture.getFrame().isValid()) {
                expiredNaturally.add(furniture.getFrame().getUniqueId());
                fic.remove(furniture, null, false, true); // silent
            }
        }, ticks);

        expiryTasks.put(furniture.getFrame().getUniqueId(), task);
    }

    /**
     * Listener: Handles all Crucible furniture removals and drops the key — unless it expired.
     */
    @EventHandler
    public void onFurnitureRemove(MythicFurnitureRemoveEvent e) {
        FurnitureItemContext ctx = e.getFurnitureItemContext();
        if (!ctx.getItem().getMythicItem()
                .getInternalName()
                .equalsIgnoreCase(crucibleId)) {
            return;
        }

        var frame = e.getFurniture().getFrame();
        UUID uid = frame.getUniqueId();

        // If expired naturally, do NOT drop a key
        if (expiredNaturally.remove(uid)) return;

        String rawOwner = frame.getPersistentDataContainer()
                .get(DeathListener.KEY_OWNER, PersistentDataType.STRING);
        String mvGroup = frame.getPersistentDataContainer()
                .get(DeathListener.KEY_GROUP, PersistentDataType.STRING);

        if (rawOwner != null && mvGroup != null) {
            UUID ownerId = UUID.fromString(rawOwner);
            String ownerName = Bukkit.getOfflinePlayer(ownerId).getName();
            frame.getWorld().dropItemNaturally(
                    frame.getLocation(),
                    DeathKeyBuilder.build(ownerId,
                            ownerName == null ? "?" : ownerName,
                            mvGroup, cfg)
            );
        }

        // Cancel expiry task if active
        BukkitTask task = expiryTasks.remove(uid);
        if (task != null) task.cancel();
    }
}
