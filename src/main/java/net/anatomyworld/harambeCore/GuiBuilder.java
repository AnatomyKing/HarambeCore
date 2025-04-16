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

    public enum SlotType {
        BUTTON,
        INPUT_SLOT,
        FILLER
    }

    public enum ActionType {
        COMMAND
    }

    private final Map<UUID, Map<String, Inventory>> playerGuis = new HashMap<>();
    private final Map<String, Map<Integer, SlotType>> guiSlotTypes = new HashMap<>();
    private final Map<String, Map<Integer, String>> buttonLogicCache = new HashMap<>();
    private final Map<String, Map<Integer, Double>> guiSlotCosts = new HashMap<>();

    private FileConfiguration config;
    private ItemStack cachedFillerItem;

    public GuiBuilder(JavaPlugin plugin, FileConfiguration config) {
        this.config = config;
        this.cachedFillerItem = createFillerItem();
    }

    public Map<String, Map<Integer, SlotType>> getGuiSlotTypes() {
        return guiSlotTypes;
    }

    public Map<Integer, Double> getSlotCosts(String guiKey) {
        return guiSlotCosts.getOrDefault(guiKey, Collections.emptyMap());
    }

    public void updateConfig(FileConfiguration config) {
        this.config = config;
        playerGuis.clear();
        guiSlotTypes.clear();
        buttonLogicCache.clear();
        guiSlotCosts.clear();
        this.cachedFillerItem = createFillerItem();
    }

    public Set<String> getGuiKeys() {
        ConfigurationSection guiSection = config.getConfigurationSection("gui");
        return guiSection != null ? guiSection.getKeys(false) : Collections.emptySet();
    }

    public void createAndOpenGui(String guiKey, Player player) {
        Inventory gui = playerGuis
                .computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .computeIfAbsent(guiKey, k -> generateGui(guiKey));

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

        Component titleComponent = title.contains("§") || title.contains("&")
                ? LegacyComponentSerializer.legacySection().deserialize(title)
                : Component.text(title);

        Inventory gui = Bukkit.createInventory(null, size, titleComponent);
        Map<Integer, SlotType> slotTypes = new HashMap<>();
        Map<Integer, String> buttonLogics = new HashMap<>();
        Map<Integer, Double> slotCosts = new HashMap<>();

        ConfigurationSection buttonsSection = guiSection.getConfigurationSection("buttons");
        if (buttonsSection != null) {
            for (String buttonKey : buttonsSection.getKeys(false)) {
                ConfigurationSection buttonConfig = buttonsSection.getConfigurationSection(buttonKey);
                if (buttonConfig == null) continue;

                List<Integer> slots = buttonConfig.getIntegerList("slot");
                String type = buttonConfig.getString("type", "FILLER").toUpperCase(Locale.ROOT);
                double cost = buttonConfig.getDouble("cost", 0.0);

                SlotType slotType;
                try {
                    slotType = SlotType.valueOf(type);
                } catch (IllegalArgumentException e) {
                    continue;
                }

                switch (slotType) {
                    case INPUT_SLOT -> {
                        for (int slot : slots) {
                            slotTypes.put(slot, SlotType.INPUT_SLOT);
                            if (cost > 0) slotCosts.put(slot, cost);
                        }
                    }
                    case BUTTON -> {
                        ActionType actionType;
                        try {
                            actionType = ActionType.valueOf(buttonConfig.getString("action", "COMMAND").toUpperCase());
                        } catch (IllegalArgumentException e) {
                            continue;
                        }

                        String logicCommand = buttonConfig.getString("logic");
                        ConfigurationSection design = buttonConfig.getConfigurationSection("design");
                        if (design == null) continue;

                        String materialName = design.getString("material");
                        String itemName = design.getString("name", "&fButton");
                        int customModelData = design.getInt("custom_model_data", 0);

                        ItemStack item = createButtonItem(materialName, itemName, customModelData);
                        if (item == null) continue;

                        for (int slot : slots) {
                            gui.setItem(slot, item);
                            slotTypes.put(slot, SlotType.BUTTON);
                            if (cost > 0) slotCosts.put(slot, cost);
                            if (actionType == ActionType.COMMAND && logicCommand != null && !logicCommand.isEmpty()) {
                                buttonLogics.put(slot, logicCommand);
                            }
                        }
                    }
                    default -> {}
                }
            }
        }

        for (int i = 0; i < size; i++) {
            if (!slotTypes.containsKey(i)) {
                gui.setItem(i, cachedFillerItem);
                slotTypes.put(i, SlotType.FILLER);
            }
        }

        guiSlotTypes.put(guiKey, slotTypes);
        buttonLogicCache.put(guiKey, buttonLogics);
        guiSlotCosts.put(guiKey, slotCosts);

        return gui;
    }

    private ItemStack createButtonItem(String materialName, String itemName, int customModelData) {
        if (materialName == null) return null;
        Material material = Material.matchMaterial(materialName);
        if (material == null) return null;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(itemName));
        if (customModelData > 0) meta.setCustomModelData(customModelData);
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
        if (customModelData > 0) meta.setCustomModelData(customModelData);
        item.setItemMeta(meta);
        return item;
    }

    public void handleButtonClick(Player player, String guiKey, int slot) {
        String command = buttonLogicCache.getOrDefault(guiKey, Collections.emptyMap()).get(slot);
        if (command != null && !command.isEmpty()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
        }
    }

    public String getGuiKeyByInventory(Player player, Inventory inventory) {
        Map<String, Inventory> guis = playerGuis.get(player.getUniqueId());
        if (guis == null) return null;
        return guis.entrySet().stream().filter(entry -> entry.getValue().equals(inventory)).map(Map.Entry::getKey).findFirst().orElse(null);
    }
}
