package net.anatomyworld.harambeCore.death;

import com.onarandombox.multiverseinventories.MultiverseInventories;
import com.onarandombox.multiverseinventories.WorldGroup;
import org.bukkit.Bukkit;
import org.bukkit.World;

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
}
