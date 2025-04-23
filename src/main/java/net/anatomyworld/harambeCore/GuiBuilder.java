// GuiBuilder.java

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
        FILLER,
        CHECK_BUTTON
    }

    public enum ActionType {
        COMMAND,
        GIVE
    }

    public enum InputActionType {
        NONE,
        CONSUME
    }

    private final Map<UUID, Map<String, Inventory>> playerGuis = new HashMap<>();
    private final Map<String, Map<Integer, SlotType>> guiSlotTypes = new HashMap<>();
    private final Map<String, Map<Integer, String>> buttonLogicCache = new HashMap<>();
    private final Map<String, Map<Integer, Double>> guiSlotCosts = new HashMap<>();
    private final Map<String, Map<Integer, String>> guiAcceptedItems = new HashMap<>();
    private final Map<String, Map<Integer, String>> guiOutputItems = new HashMap<>();
    private final Map<String, Map<Integer, Integer>> guiPayoutAmounts = new HashMap<>();
    private final Map<String, Map<Integer, InputActionType>> guiInputActions = new HashMap<>();
    private final Map<String, Map<Integer, List<Integer>>> guiSlotConnections = new HashMap<>();
    private final Map<String, Map<Integer, String>> guiCheckItems = new HashMap<>();

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

    public Map<Integer, String> getAcceptedItems(String guiKey) {
        return guiAcceptedItems.getOrDefault(guiKey, Collections.emptyMap());
    }

    public Map<Integer, String> getOutputItems(String guiKey) {
        return guiOutputItems.getOrDefault(guiKey, Collections.emptyMap());
    }

    public Map<Integer, Integer> getPayoutAmounts(String guiKey) {
        return guiPayoutAmounts.getOrDefault(guiKey, Collections.emptyMap());
    }

    public Map<Integer, InputActionType> getInputActions(String guiKey) {
        return guiInputActions.getOrDefault(guiKey, Collections.emptyMap());
    }

    public Map<Integer, List<Integer>> getSlotConnections(String guiKey) {
        return guiSlotConnections.getOrDefault(guiKey, Collections.emptyMap());
    }

    public Map<Integer, String> getCheckItems(String guiKey) {
        return guiCheckItems.getOrDefault(guiKey, Collections.emptyMap());
    }

    public void updateConfig(FileConfiguration config) {
        this.config = config;
        playerGuis.clear();
        guiSlotTypes.clear();
        buttonLogicCache.clear();
        guiSlotCosts.clear();
        guiAcceptedItems.clear();
        guiOutputItems.clear();
        guiPayoutAmounts.clear();
        guiInputActions.clear();
        guiSlotConnections.clear();
        guiCheckItems.clear();
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
        Component titleComponent = LegacyComponentSerializer.legacySection().deserialize(title);

        Inventory gui = Bukkit.createInventory(null, size, titleComponent);
        Map<Integer, SlotType> slotTypes = new HashMap<>();
        Map<Integer, String> buttonLogics = new HashMap<>();
        Map<Integer, Double> slotCosts = new HashMap<>();
        Map<Integer, String> acceptedItems = new HashMap<>();
        Map<Integer, String> outputItems = new HashMap<>();
        Map<Integer, Integer> payoutAmounts = new HashMap<>();
        Map<Integer, InputActionType> inputActions = new HashMap<>();
        Map<Integer, List<Integer>> slotConnections = new HashMap<>();
        Map<Integer, String> checkItems = new HashMap<>();

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
                        String accepted = buttonConfig.getString("accepted_item");
                        InputActionType inputAction = InputActionType.valueOf(buttonConfig.getString("action", "NONE").toUpperCase(Locale.ROOT));
                        for (int slot : slots) {
                            slotTypes.put(slot, SlotType.INPUT_SLOT);
                            if (cost > 0) slotCosts.put(slot, cost);
                            if (accepted != null && !accepted.isEmpty()) acceptedItems.put(slot, accepted.toUpperCase(Locale.ROOT));
                            inputActions.put(slot, inputAction);
                        }
                    }

                    case BUTTON, CHECK_BUTTON -> {
                        ActionType actionType = ActionType.valueOf(buttonConfig.getString("action", "COMMAND").toUpperCase(Locale.ROOT));
                        String logicCommand = buttonConfig.getString("logic");
                        String outputItem = buttonConfig.getString("output_item");
                        int payoutAmount = buttonConfig.getInt("payout_amount", 1);
                        String checkItem = buttonConfig.getString("check_item");
                        String connectionKey = buttonConfig.getString("slot_connection");

                        List<Integer> connectedSlots = new ArrayList<>();
                        if (connectionKey != null && buttonsSection.isConfigurationSection(connectionKey)) {
                            connectedSlots = Objects.requireNonNull(buttonsSection.getConfigurationSection(connectionKey)).getIntegerList("slot");
                        }

                        ConfigurationSection design = buttonConfig.getConfigurationSection("design");
                        if (design == null) continue;

                        String materialName = design.getString("material");
                        String itemName = design.getString("name", "&fButton");
                        int customModelData = design.getInt("custom_model_data", 0);

                        ItemStack item = createButtonItem(materialName, itemName, customModelData);
                        if (item == null) continue;

                        for (int slot : slots) {
                            gui.setItem(slot, item);
                            slotTypes.put(slot, slotType);
                            if (cost > 0) slotCosts.put(slot, cost);

                            if (actionType == ActionType.COMMAND && logicCommand != null) {
                                buttonLogics.put(slot, logicCommand);
                            }
                            if (actionType == ActionType.GIVE && outputItem != null) {
                                outputItems.put(slot, outputItem.toUpperCase(Locale.ROOT));
                                payoutAmounts.put(slot, Math.max(1, payoutAmount));
                            }

                            if (slotType == SlotType.CHECK_BUTTON) {
                                if (checkItem != null) checkItems.put(slot, checkItem.toUpperCase(Locale.ROOT));
                                if (!connectedSlots.isEmpty()) slotConnections.put(slot, connectedSlots);
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
        guiAcceptedItems.put(guiKey, acceptedItems);
        guiOutputItems.put(guiKey, outputItems);
        guiPayoutAmounts.put(guiKey, payoutAmounts);
        guiInputActions.put(guiKey, inputActions);
        guiCheckItems.put(guiKey, checkItems);
        guiSlotConnections.put(guiKey, slotConnections);

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
        Map<Integer, String> logics = buttonLogicCache.getOrDefault(guiKey, Collections.emptyMap());
        Map<Integer, String> outputs = guiOutputItems.getOrDefault(guiKey, Collections.emptyMap());
        Map<Integer, Integer> payouts = guiPayoutAmounts.getOrDefault(guiKey, Collections.emptyMap());

        if (logics.containsKey(slot)) {
            String command = logics.get(slot);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
        } else if (outputs.containsKey(slot)) {
            Material mat = Material.matchMaterial(outputs.get(slot));
            if (mat != null) {
                player.getInventory().addItem(new ItemStack(mat, payouts.getOrDefault(slot, 1)));
            }
        }
    }

    public String getGuiKeyByInventory(Player player, Inventory inventory) {
        Map<String, Inventory> guis = playerGuis.get(player.getUniqueId());
        if (guis == null) return null;
        return guis.entrySet().stream().filter(entry -> entry.getValue().equals(inventory)).map(Map.Entry::getKey).findFirst().orElse(null);
    }
}
