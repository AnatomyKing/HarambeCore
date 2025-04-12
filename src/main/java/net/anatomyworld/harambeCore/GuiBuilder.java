package net.anatomyworld.harambeCore;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class GuiBuilder {

    private FileConfiguration config;
    private final Map<UUID, Map<String, Inventory>> playerGuis = new HashMap<>();
    private final Map<String, Map<Integer, String>> buttonKeyCache = new HashMap<>();
    private final Set<Integer> slotPositions = new HashSet<>();
    private ItemStack cachedFillerItem;

    public GuiBuilder(JavaPlugin plugin, FileConfiguration config) {
        this.config = config;
        this.cachedFillerItem = createFillerItem();
    }

    public void updateConfig(FileConfiguration config) {
        this.config = config;
        playerGuis.clear();
        buttonKeyCache.clear();
        this.cachedFillerItem = createFillerItem();
    }

    public Set<String> getGuiKeys() {
        ConfigurationSection guiSection = config.getConfigurationSection("gui");
        if (guiSection != null) {
            return guiSection.getKeys(false);
        }
        return Collections.emptySet();
    }

    public void createAndOpenGui(String guiKey, Player player) {
        UUID playerId = player.getUniqueId();
        Map<String, Inventory> guis = playerGuis.computeIfAbsent(playerId, k -> new HashMap<>());

        Inventory gui = guis.computeIfAbsent(guiKey, k -> generateGui(guiKey));
        if (gui != null) {
            player.openInventory(gui);
        } else {
            player.sendMessage("§cInvalid GUI configuration for '" + guiKey + "'!");
        }
    }

    private Inventory generateGui(String guiKey) {
        ConfigurationSection guiSection = config.getConfigurationSection("gui." + guiKey);
        if (guiSection == null) return null;

        String title = guiSection.getString("title", "&cUnnamed GUI");
        int size = guiSection.getInt("size", 54);
        Component titleComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(title);

        Inventory gui = Bukkit.createInventory(null, size, titleComponent);
        Map<Integer, String> buttonKeyMap = new HashMap<>();
        slotPositions.clear();

        ConfigurationSection buttonsSection = guiSection.getConfigurationSection("buttons");
        if (buttonsSection != null) {
            for (String buttonKey : buttonsSection.getKeys(false)) {
                ConfigurationSection buttonConfig = buttonsSection.getConfigurationSection(buttonKey);
                if (buttonConfig == null) continue;

                List<Integer> slots = buttonConfig.getIntegerList("slot");

                if (buttonKey.endsWith("_slot")) {
                    // Register slots to skip filler later
                    slotPositions.addAll(slots);
                    continue; // Don't place items for *_slot
                }

                // Process *_button normally
                String materialName = buttonConfig.getString("material");
                String itemName = buttonConfig.getString("name", "&fButton");
                int customModelData = buttonConfig.getInt("custom_model_data", 0);

                ItemStack item = createButtonItem(materialName, itemName, customModelData);
                if (item == null) continue;

                for (int slot : slots) {
                    gui.setItem(slot, item);
                    buttonKeyMap.put(slot, buttonKey);
                    slotPositions.add(slot); // Register slot position as used
                }
            }
        }

        buttonKeyCache.put(guiKey, buttonKeyMap);

        // Fill unused slots with filler item
        for (int i = 0; i < size; i++) {
            if (!slotPositions.contains(i) && gui.getItem(i) == null) {
                gui.setItem(i, cachedFillerItem);
            }
        }

        return gui;
    }

    private ItemStack createButtonItem(String materialName, String itemName, int customModelData) {
        if (materialName == null) return null;
        Material material = Material.matchMaterial(materialName);
        if (material == null) return null;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(itemName));
        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createFillerItem() {
        String materialName = config.getString("filler.material", "BLACK_STAINED_GLASS_PANE");
        Material material = Material.matchMaterial(materialName);
        if (material == null) material = Material.BLACK_STAINED_GLASS_PANE;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(config.getString("filler.name", "&8")));
        int customModelData = config.getInt("filler.custom_model_data", 0);
        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }
        item.setItemMeta(meta);
        return item;
    }

    public void handleButtonClick(Player player, String guiKey, int slot) {
        Map<Integer, String> buttonMap = buttonKeyCache.get(guiKey);
        if (buttonMap == null) return;

        String buttonKey = buttonMap.get(slot);
        if (buttonKey == null) return;

        ConfigurationSection buttonConfig = config.getConfigurationSection("gui." + guiKey + ".buttons." + buttonKey);
        if (buttonConfig == null) return;

        String logicCommand = buttonConfig.getString("logic");
        if (logicCommand != null && !logicCommand.isEmpty()) {
            executeLogic(player, logicCommand);
        }
    }

    private void executeLogic(Player player, String command) {
        String parsedCommand = command.replace("%player%", player.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCommand);
    }

    public String getGuiKeyByInventory(Player player, Inventory inventory) {
        Map<String, Inventory> guis = playerGuis.get(player.getUniqueId());
        if (guis == null) return null;

        return guis.entrySet().stream()
                .filter(entry -> entry.getValue().equals(inventory))
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
    }
}
