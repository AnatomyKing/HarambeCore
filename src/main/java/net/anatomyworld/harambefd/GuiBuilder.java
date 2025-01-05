package net.anatomyworld.harambefd;

import net.anatomyworld.harambefd.guis.Krobus.KrobusMethods;
import net.anatomyworld.harambefd.guis.Spawn.SpawnMethods;
import net.anatomyworld.harambefd.guis.anygui.InterfaceMethods;
import net.anatomyworld.harambefd.guis.enderlink.EnderlinkMethods;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.*;
import java.util.stream.Collectors;

public class GuiBuilder {

    private FileConfiguration config;  // Configuration passed from the main plugin class
    private final Map<UUID, Map<String, Inventory>> playerGuis = new HashMap<>();
    private final Map<String, Map<Integer, String>> buttonKeyCache = new HashMap<>();
    private final Map<String, Map<Integer, String>> slotItemNameCache = new HashMap<>();
    private final Map<String, Map<String, List<Integer>>> guiButtonToSlotsMap = new HashMap<>();
    private final Map<String, Map<Integer, Boolean>> slotConsumeOnPlaceMap = new HashMap<>();
    private final Map<String, Map<Integer, Double>> slotPayMap = new HashMap<>();  // New map to store pay amounts
    private final Map<String, Map<Integer, Integer>> slotItemAmountMap = new HashMap<>(); // New map for item_amount
    private ItemStack cachedFillerItem;

    public GuiBuilder(FileConfiguration config) {
        this.config = config;
        this.cachedFillerItem = createFillerItem();
    }

    public void updateConfig(FileConfiguration config) {
        this.config = config;
        playerGuis.clear();
        buttonKeyCache.clear();
        slotItemNameCache.clear();
        guiButtonToSlotsMap.clear();
        slotConsumeOnPlaceMap.clear();
        slotPayMap.clear();  // Clear pay map on config reload
        slotItemAmountMap.clear();
        this.cachedFillerItem = createFillerItem();
    }

    public void createAndOpenGui(String guiKey, Player player) {
        UUID playerId = player.getUniqueId();
        Map<String, Inventory> guis = playerGuis.computeIfAbsent(playerId, k -> new HashMap<>());

        if (!guis.containsKey(guiKey)) {
            Inventory gui = generateGui(guiKey);
            if (gui != null) {
                guis.put(guiKey, gui);
            } else {
                player.sendMessage("Invalid GUI configuration!");
                return;
            }
        }
        player.openInventory(guis.get(guiKey));
    }

    private Inventory generateGui(String guiKey) {
        ConfigurationSection guiSection = config.getConfigurationSection("gui." + guiKey);
        if (guiSection == null) {
            return null;
        }

        String titleString = guiSection.getString("title", "Default Title");
        Component titleComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(titleString);
        int size = guiSection.getInt("size", 54);

        Inventory gui = Bukkit.createInventory(null, size, titleComponent);
        Map<Integer, String> buttonKeyMap = new HashMap<>();
        Map<Integer, String> itemNameMap = new HashMap<>();
        Map<String, List<Integer>> buttonToSlotsMap = new HashMap<>();
        Map<Integer, Boolean> consumeOnPlaceMap = new HashMap<>();
        Map<Integer, Double> payMap = new HashMap<>();  // Store pay amounts for each slot
        Map<Integer, Integer> itemAmountMap = new HashMap<>(); // New map to store item_amount

        ConfigurationSection buttonsSection = guiSection.getConfigurationSection("buttons");
        if (buttonsSection != null) {
            for (String buttonKey : buttonsSection.getKeys(false)) {
                ConfigurationSection buttonConfig = buttonsSection.getConfigurationSection(buttonKey);

                if (buttonConfig != null) {
                    boolean isSpecialSlot = buttonKey.endsWith("_slot");
                    List<Integer> slots = buttonConfig.getIntegerList("slot");

                    double payAmount = buttonConfig.getDouble("pay", 0.0); // Get the pay amount from config
                    int itemAmount = buttonConfig.getInt("item_amount", 0); // Get item_amount from config

                    if (!isSpecialSlot) {
                        Material material = Material.matchMaterial(Objects.requireNonNull(buttonConfig.getString("material")));
                        String name = buttonConfig.getString("name", "Default Button");
                        int customModelData = buttonConfig.getInt("custom_model_data", 0);

                        assert material != null;
                        ItemStack item = new ItemStack(material);
                        ItemMeta meta = item.getItemMeta();
                        meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(name));
                        if (customModelData > 0) {
                            meta.setCustomModelData(customModelData);
                        }
                        item.setItemMeta(meta);

                        for (int slot : slots) {
                            gui.setItem(slot, item);
                            buttonKeyMap.put(slot, buttonKey);
                            payMap.put(slot, payAmount);  // Store pay amount for slot
                        }
                    } else {
                        String itemName = buttonConfig.getString("item_name");
                        String button = buttonConfig.getString("button", null);
                        boolean consumeOnPlace = buttonConfig.getBoolean("consume", true);

                        for (int slot : slots) {
                            buttonKeyMap.put(slot, buttonKey);
                            itemNameMap.put(slot, itemName);
                            consumeOnPlaceMap.put(slot, consumeOnPlace);
                            payMap.put(slot, payAmount);  // Store pay amount for special slots
                            itemAmountMap.put(slot, itemAmount); // Add item_amount for special slots

                            if (button != null) {
                                buttonToSlotsMap.computeIfAbsent(button, k -> new ArrayList<>()).add(slot);
                            }
                        }
                    }
                }
            }
        }

        buttonKeyCache.put(guiKey, buttonKeyMap);
        slotItemNameCache.put(guiKey, itemNameMap);
        guiButtonToSlotsMap.put(guiKey, buttonToSlotsMap);
        slotConsumeOnPlaceMap.put(guiKey, consumeOnPlaceMap);
        slotPayMap.put(guiKey, payMap);  // Cache the pay amounts
        slotItemAmountMap.put(guiKey, itemAmountMap); // Cache the item amounts


        // ============== FILLER LOGIC FOR ENDERLINK ==============
        if (guiKey.equalsIgnoreCase("enderlink")) {
            // We define filler in top row (0..8) & bottom row (45..53).
            // Also, we treat them as "button" slots => we put them in buttonKeyMap as well
            // so the code won't let players remove them and won't save them to .yml.
            for (int i = 0; i <= 8; i++) {
                if (!buttonKeyMap.containsKey(i)) {
                    gui.setItem(i, cachedFillerItem);
                    buttonKeyMap.put(i, "filler_slot");  // Mark it so we skip it in EnderlinkMethods
                }
            }
            for (int i = 45; i < size; i++) {
                if (!buttonKeyMap.containsKey(i)) {
                    gui.setItem(i, cachedFillerItem);
                    buttonKeyMap.put(i, "filler_slot");
                }
            }
        } else {
            // For other GUIs, fill all empty slots with filler
            for (int i = 0; i < size; i++) {
                if (!buttonKeyMap.containsKey(i) && gui.getItem(i) == null) {
                    gui.setItem(i, cachedFillerItem);
                }
            }
        }
        return gui;
    }


    private ItemStack createFillerItem() {
        Material fillerMaterial = Material.matchMaterial(config.getString("filler.material", "BLACK_STAINED_GLASS_PANE"));
        String fillerName = config.getString("filler.name", "&8");
        int customModelData = config.getInt("filler.custom_model_data", 0);

        assert fillerMaterial != null;
        ItemStack fillerItem = new ItemStack(fillerMaterial);
        ItemMeta meta = fillerItem.getItemMeta();
        meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(fillerName));

        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }

        fillerItem.setItemMeta(meta);
        return fillerItem;
    }


    public Map<Integer, Double> getSlotPayMap(String guiKey) {
        return slotPayMap.getOrDefault(guiKey, new HashMap<>());
    }

    public Map<Integer, String> getButtonKeyMap(String guiKey) {
        return buttonKeyCache.get(guiKey);
    }

    public int getItemAmountForSlot(String guiKey, int slot) {
        return slotItemAmountMap.getOrDefault(guiKey, new HashMap<>()).getOrDefault(slot, 0);
    }

    public String getGuiKeyByInventory(Player player, Inventory inventory) {
        UUID playerId = player.getUniqueId();
        Map<String, Inventory> guis = playerGuis.get(playerId);
        if (guis == null) return null;

        for (Map.Entry<String, Inventory> entry : guis.entrySet()) {
            if (entry.getValue().equals(inventory)) {
                return entry.getKey();
            }
        }

        return null;
    }

    public String getItemNameForSlot(String guiKey, int slot) {
        Map<Integer, String> itemNameMap = slotItemNameCache.get(guiKey);
        return (itemNameMap != null) ? itemNameMap.get(slot) : null;
    }

    public boolean shouldConsumeOnPlace(String guiKey, int slot) {
        Map<Integer, Boolean> consumeOnPlaceMap = slotConsumeOnPlaceMap.get(guiKey);
        return consumeOnPlaceMap != null && consumeOnPlaceMap.getOrDefault(slot, true);
    }

    public List<Integer> getAllowedSlots(String guiKey, String itemName) {
        Map<Integer, String> itemNameMap = slotItemNameCache.get(guiKey);
        if (itemNameMap == null) return Collections.emptyList();

        return itemNameMap.entrySet().stream()
                .filter(entry -> itemName.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public List<Integer> getSlotsForButton(String guiKey, String buttonKey) {
        Map<String, List<Integer>> buttonToSlotsMap = guiButtonToSlotsMap.get(guiKey);
        if (buttonToSlotsMap != null) {
            return buttonToSlotsMap.getOrDefault(buttonKey, Collections.emptyList());
        } else {
            return Collections.emptyList();
        }
    }

    public void handleButtonClick(Player player, String guiKey, String buttonKey, Map<Integer, ItemStack> consumedItems) {
        if (buttonKey.endsWith("_slot")) {
            return;
        }

        switch (guiKey) {
            case "krobus":
                KrobusMethods.handleButtonClick(player, buttonKey, consumedItems, getSlotPayMap(guiKey)); // Pass the pay map
                break;
            case "interface":
                InterfaceMethods.handleButtonClick(player, buttonKey, consumedItems, getSlotPayMap(guiKey)); // Pass the pay map
                break;
            case "spawn":
                SpawnMethods.handleButtonClick(player, buttonKey, consumedItems);
                break;
            case "enderlink":
                EnderlinkMethods.handleButtonClick(player, buttonKey, this);
                break;
            default:
                player.sendMessage("Unknown button action.");
        }
    }
}
