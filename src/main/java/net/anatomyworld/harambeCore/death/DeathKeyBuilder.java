package net.anatomyworld.harambeCore.death;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.UUID;

/** Static helper – creates a paper Death Key exactly as YAML defines it. */
final class DeathKeyBuilder {

    private DeathKeyBuilder() {}

    static ItemStack build(UUID ownerId,
                           String ownerName,
                           String mvGroup,
                           DeathChestModule cfg) {

        var c = cfg.key();           // may be null

        Material mat = Material.matchMaterial(
                c != null ? c.getString("material", "PAPER") : "PAPER");
        if (mat == null) mat = Material.PAPER;

        ItemStack key = new ItemStack(mat);
        ItemMeta  meta = key.getItemMeta();

        String rawName = c != null
                ? c.getString("name", "&cDeath Key &7▶ &f%player%")
                : "&cDeath Key &7▶ &f%player%";

        meta.displayName(LegacyComponentSerializer.legacyAmpersand()
                .deserialize(rawName
                        .replace("%player%", ownerName)
                        .replace("%world_alias%", mvGroup)));

        if (c != null && c.isList("lore")) {
            List<TextComponent> lore = c.getStringList("lore").stream()
                    .map(s -> s.replace("%player%", ownerName)
                            .replace("%world_alias%", mvGroup))
                    .map(s -> LegacyComponentSerializer.legacyAmpersand()
                            .deserialize(s))
                    .toList();
            meta.lore(lore);
        }

        if (c != null) {
            int cmd = c.getInt("custom_model_data", 0);
            if (cmd > 0) meta.setCustomModelData(cmd);
        }

        meta.getPersistentDataContainer().set(DeathListener.KEY_OWNER,
                PersistentDataType.STRING, ownerId.toString());
        meta.getPersistentDataContainer().set(DeathListener.KEY_GROUP,
                PersistentDataType.STRING, mvGroup);

        key.setItemMeta(meta);
        return key;
    }
}
