package net.anatomyworld.harambeCore.death;

import com.onarandombox.multiverseinventories.MultiverseInventories;
import com.onarandombox.multiverseinventories.WorldGroup;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class WorldGroupHelper {

    private WorldGroupHelper() {}

    /* ------------ Multiverse-Inventories group resolver ------------ */
    public static String getGroupName(World world) {
        var mvi = (MultiverseInventories) Bukkit.getPluginManager()
                .getPlugin("Multiverse-Inventories");
        if (mvi == null || !mvi.isEnabled()) return "ungrouped";

        for (WorldGroup g : mvi.getGroupManager().getGroupsForWorld(world.getName()))
            return g.getName();

        return "ungrouped";
    }

    /* ------------ broadcast helper (YML-driven) -------------------- */
    static void broadcastDeathKey(Player victim,
                                  String mvGroup,
                                  DeathChestModule cfg) {

        if (!cfg.broadcast().getBoolean("enabled", true)) return;

        /* pretty alias via Multiverse-Core */
        String alias = mvGroup;
        var coreRaw = Bukkit.getPluginManager().getPlugin("Multiverse-Core");
        if (coreRaw instanceof com.onarandombox.MultiverseCore.MultiverseCore core) {
            var mvw = core.getMVWorldManager().getMVWorld(victim.getWorld());
            if (mvw != null && !mvw.getAlias().isEmpty()) alias = mvw.getAlias();
        }

        Location l = victim.getLocation();
        String raw = cfg.broadcast().getString(
                "message",
                "&6[DeathKey] &eDeath key spawned for &c%player% &eat &b%world_alias% &7(&d%x%, %y%, %z%&7)"
        );

        String txt = raw.replace("%player%", victim.getName())
                .replace("%world_alias%", alias)
                .replace("%x%", String.valueOf(l.getBlockX()))
                .replace("%y%", String.valueOf(l.getBlockY()))
                .replace("%z%", String.valueOf(l.getBlockZ()));

        Component msg = LegacyComponentSerializer.legacyAmpersand().deserialize(txt);
        Bukkit.getServer().broadcast(msg);
    }
}
