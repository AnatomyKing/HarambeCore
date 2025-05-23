package net.anatomyworld.harambeCore.death;

import com.onarandombox.multiverseinventories.MultiverseInventories;
import com.onarandombox.multiverseinventories.WorldGroup;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/** Utility – resolves the Multiverse-Inventories world-group a world belongs to. */
public final class WorldGroupHelper {

    private WorldGroupHelper() { }  // static-only

    /** @return group name, or "ungrouped" if MV-Inventories absent / no match */
    public static String getGroupName(World world) {
        MultiverseInventories mvi =
                (MultiverseInventories) Bukkit.getPluginManager()
                        .getPlugin("Multiverse-Inventories");
        if (mvi == null || !mvi.isEnabled()) {
            return "ungrouped";
        }

        for (WorldGroup grp : mvi.getGroupManager()
                .getGroupsForWorld(world.getName())) {
            return grp.getName();           // first match is fine
        }
        return "ungrouped";
    }


    static void broadcastDeathKey(Player victim, String mvGroup) {

        /* ---------- resolve Multiverse alias ---------- */
        String alias = mvGroup;                       // sensible fallback
        var raw = Bukkit.getPluginManager().getPlugin("Multiverse-Core");

        if (raw instanceof com.onarandombox.MultiverseCore.MultiverseCore mvCore) {
            var mvw = mvCore.getMVWorldManager().getMVWorld(victim.getWorld());
            if (mvw != null && !mvw.getAlias().isEmpty()) {
                alias = mvw.getAlias();
            }
        }

        /* ---------- build component ---------- */
        Location loc = victim.getLocation();
        Component message = Component.text()
                .append(Component.text("[DeathKey] ", NamedTextColor.GOLD))
                .append(Component.text("Death key spawned for ", NamedTextColor.YELLOW))
                .append(Component.text(victim.getName(), NamedTextColor.RED))
                .append(Component.text(" at ", NamedTextColor.YELLOW))
                .append(Component.text(alias, NamedTextColor.AQUA))
                .append(Component.text(" (", NamedTextColor.GRAY))
                .append(Component.text(loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ(),
                        NamedTextColor.LIGHT_PURPLE))
                .append(Component.text(")", NamedTextColor.GRAY))
                .build();

        /* ---------- broadcast ---------- */
        Bukkit.getServer().broadcast(message);
    }
}
