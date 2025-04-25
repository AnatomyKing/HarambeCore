package net.anatomyworld.harambeCore;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class GuiBuilder {

    public enum SlotType {BUTTON, INPUT_SLOT, CHECK_BUTTON, OUTPUT_SLOT, FILLER}
    public enum ActionType {COMMAND, GIVE, REWARD_GET}
    public enum InputActionType {NONE, CONSUME}

    private final Map<UUID, Map<String, Inventory>> playerGuis = new HashMap<>();
    private final Map<String, Map<Integer, SlotType>> guiSlotTypes = new HashMap<>();
    private final Map<String, Map<Integer, String>> buttonLogicCache = new HashMap<>();
    private final Map<String, Map<Integer, Double>> guiSlotCosts = new HashMap<>();
    private final Map<String, Map<Integer, Boolean>> guiCostPerStack = new HashMap<>();
    private final Map<String, Map<Integer, String>> guiAcceptedItems = new HashMap<>();
    private final Map<String, Map<Integer, Integer>> guiAcceptedAmounts = new HashMap<>();
    private final Map<String, Map<Integer, String>> guiOutputItems = new HashMap<>();
    private final Map<String, Map<Integer, Integer>> guiPayoutAmounts = new HashMap<>();
    private final Map<String, Map<Integer, InputActionType>> guiInputActions = new HashMap<>();
    private final Map<String, Map<Integer, List<Integer>>> guiSlotConnections = new HashMap<>();
    private final Map<String, Map<Integer, String>> guiCheckItems = new HashMap<>();
    private final Map<String, Map<Integer, String>> guiRewardGroups = new HashMap<>();

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

    public Map<Integer, Boolean> getCostPerStack(String guiKey) {
        return guiCostPerStack.getOrDefault(guiKey, Collections.emptyMap());
    }

    public Map<Integer, String> getAcceptedItems(String guiKey) {
        return guiAcceptedItems.getOrDefault(guiKey, Collections.emptyMap());
    }

    public Map<Integer, Integer> getAcceptedAmounts(String guiKey) {
        return guiAcceptedAmounts.getOrDefault(guiKey, Collections.emptyMap());
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

    public Map<Integer, String> getRewardGroups(String guiKey) {
        return guiRewardGroups.getOrDefault(guiKey, Collections.emptyMap());
    }

    public void updateConfig(FileConfiguration config) {
        this.config = config;
        playerGuis.clear();
        guiSlotTypes.clear();
        buttonLogicCache.clear();
        guiSlotCosts.clear();
        guiCostPerStack.clear();
        guiAcceptedItems.clear();
        guiAcceptedAmounts.clear();
        guiOutputItems.clear();
        guiPayoutAmounts.clear();
        guiInputActions.clear();
        guiSlotConnections.clear();
        guiCheckItems.clear();
        guiRewardGroups.clear();
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

        if (gui != null) player.openInventory(gui);
        else player.sendMessage("§cInvalid GUI configuration for '" + guiKey + "'!");
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
        Map<Integer, Boolean> perStackMap = new HashMap<>();
        Map<Integer, String> acceptedItems = new HashMap<>();
        Map<Integer, Integer> acceptedAmounts = new HashMap<>();
        Map<Integer, String> outputItems = new HashMap<>();
        Map<Integer, Integer> payoutAmounts = new HashMap<>();
        Map<Integer, InputActionType> inputActions = new HashMap<>();
        Map<Integer, List<Integer>> slotConnections = new HashMap<>();
        Map<Integer, String> checkItems = new HashMap<>();
        Map<Integer, String> rewardGroups = new HashMap<>();

        ConfigurationSection buttonsSection = guiSection.getConfigurationSection("buttons");
        if (buttonsSection != null) {
            for (String buttonKey : buttonsSection.getKeys(false)) {
                ConfigurationSection bc = buttonsSection.getConfigurationSection(buttonKey);
                if (bc == null) continue;

                List<Integer> slots = bc.getIntegerList("slot");
                SlotType slotType;
                try { slotType = SlotType.valueOf(bc.getString("type", "FILLER").toUpperCase(Locale.ROOT)); }
                catch (IllegalArgumentException ex) { continue; }

                double ecoCost = 0.0;
                boolean perStack = false;
                Object rc = bc.get("cost");
                if (rc instanceof Number n) ecoCost = n.doubleValue();
                else if (rc instanceof ConfigurationSection cs) {
                    ecoCost = cs.getDouble("eco", 0.0);
                    perStack = cs.getBoolean("per_stack", false);
                }

                switch (slotType) {
                    case INPUT_SLOT -> {
                        InputActionType ia = InputActionType.valueOf(bc.getString("action", "NONE").toUpperCase(Locale.ROOT));
                        String rGroup = extractRewardGroup(bc);

                        if (bc.isString("accepted_item")) {
                            String mat = bc.getString("accepted_item");
                            for (int s : slots) {
                                assert mat != null;
                                acceptedItems.put(s, mat.toUpperCase(Locale.ROOT));
                            }
                        } else if (bc.isConfigurationSection("accepted_item")) {
                            ConfigurationSection ai = bc.getConfigurationSection("accepted_item");
                            assert ai != null;

                            String mat = ai.getString("material");
                            String mythic = ai.getString("mythic");
                            int amount = ai.getInt("amount", -1);
                            String rGroupInner = ai.getString("reward_group", null);

                            for (int s : slots) {
                                if (mat != null) acceptedItems.put(s, mat.toUpperCase(Locale.ROOT));
                                if (mythic != null) acceptedItems.put(s, "MYTHIC:" + mythic);
                                if (amount > 0) acceptedAmounts.put(s, amount);
                                if (rGroupInner != null) rewardGroups.put(s, rGroupInner);
                            }
                        }

                        for (int s : slots) {
                            slotTypes.put(s, slotType);
                            inputActions.put(s, ia);
                            if (ecoCost > 0) slotCosts.put(s, ecoCost);
                            if (perStack) perStackMap.put(s, true);
                            if (rGroup != null) rewardGroups.put(s, rGroup);
                        }
                    }

                    case OUTPUT_SLOT -> {
                        ActionType at = ActionType.valueOf(bc.getString("action", "REWARD_GET").toUpperCase(Locale.ROOT));
                        String rGroup = extractRewardGroup(bc);
                        for (int s : slots) {
                            slotTypes.put(s, slotType);
                            if (ecoCost > 0) slotCosts.put(s, ecoCost);
                            if (perStack) perStackMap.put(s, true);
                            if (rGroup != null && at == ActionType.REWARD_GET) rewardGroups.put(s, rGroup);
                        }
                    }

                    case BUTTON, CHECK_BUTTON -> {
                        ActionType at = ActionType.valueOf(bc.getString("action", "COMMAND").toUpperCase(Locale.ROOT));
                        String logic = bc.getString("logic");
                        String outItem = bc.getString("output_item");
                        int payAmt = bc.getInt("payout_amount", 1);
                        String connectKey = bc.getString("slot_connection");

                        String rGroup = extractRewardGroup(bc);
                        List<Integer> connected = new ArrayList<>();
                        if (connectKey != null && buttonsSection.isConfigurationSection(connectKey))
                            connected = Objects.requireNonNull(buttonsSection.getConfigurationSection(connectKey)).getIntegerList("slot");

                        ConfigurationSection d = bc.getConfigurationSection("design");
                        String matName = d != null ? d.getString("material") : "STONE";
                        String itemName = d != null ? d.getString("name", "&fButton") : "&fButton";
                        int cmd = d != null ? d.getInt("custom_model_data", 0) : 0;
                        ItemStack btnItem = createButtonItem(matName, itemName, cmd);

                        for (int s : slots) {
                            gui.setItem(s, btnItem);
                            slotTypes.put(s, slotType);
                            if (ecoCost > 0) slotCosts.put(s, ecoCost);
                            if (perStack) perStackMap.put(s, true);
                            if (at == ActionType.COMMAND && logic != null) buttonLogics.put(s, logic);
                            if (at == ActionType.GIVE && outItem != null) {
                                outputItems.put(s, outItem.toUpperCase(Locale.ROOT));
                                payoutAmounts.put(s, payAmt);
                            }
                            if (rGroup != null) checkItems.put(s, rGroup);
                            if (!connected.isEmpty()) slotConnections.put(s, connected);
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
        guiCostPerStack.put(guiKey, perStackMap);
        guiAcceptedItems.put(guiKey, acceptedItems);
        guiAcceptedAmounts.put(guiKey, acceptedAmounts);
        guiOutputItems.put(guiKey, outputItems);
        guiPayoutAmounts.put(guiKey, payoutAmounts);
        guiInputActions.put(guiKey, inputActions);
        guiCheckItems.put(guiKey, checkItems);
        guiSlotConnections.put(guiKey, slotConnections);
        guiRewardGroups.put(guiKey, rewardGroups);
        return gui;
    }

    private String extractRewardGroup(ConfigurationSection section) {
        if (section.isConfigurationSection("accepted_item"))
            return Objects.requireNonNull(section.getConfigurationSection("accepted_item")).getString("reward_group", null);
        if (section.isConfigurationSection("check_item"))
            return Objects.requireNonNull(section.getConfigurationSection("check_item")).getString("reward_group", null);
        return section.getString("reward_group");
    }

    private ItemStack createButtonItem(String materialName, String itemName, int cmd) {
        Material mat = Material.matchMaterial(materialName);
        if (mat == null) return null;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(itemName));
        if (cmd > 0) meta.setCustomModelData(cmd);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createFillerItem() {
        String m = config.getString("filler.material", "BLACK_STAINED_GLASS_PANE");
        Material mat = Material.matchMaterial(m);
        if (mat == null) mat = Material.BLACK_STAINED_GLASS_PANE;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(config.getString("filler.name", "&8")));
        int cmd = config.getInt("filler.custom_model_data", 0);
        if (cmd > 0) meta.setCustomModelData(cmd);
        item.setItemMeta(meta);
        return item;
    }

    public void handleButtonClick(Player player, String guiKey, int slot) {
        Map<Integer, String> logics = buttonLogicCache.getOrDefault(guiKey, Collections.emptyMap());
        Map<Integer, String> outputs = guiOutputItems.getOrDefault(guiKey, Collections.emptyMap());
        Map<Integer, Integer> payouts = guiPayoutAmounts.getOrDefault(guiKey, Collections.emptyMap());

        if (logics.containsKey(slot)) {
            String raw = logics.get(slot).replace("%player%", player.getName());
            CommandSender sender;
            if (raw.startsWith("console:")) {
                sender = Bukkit.getConsoleSender();
                raw = raw.substring("console:".length());
            } else if (raw.startsWith("player:")) {
                sender = player;
                raw = raw.substring("player:".length());
            } else {
                sender = player;
            }
            if (raw.startsWith("/")) raw = raw.substring(1);
            Bukkit.dispatchCommand(sender, raw.trim());
        } else if (outputs.containsKey(slot)) {
            Material mat = Material.matchMaterial(outputs.get(slot));
            if (mat != null) player.getInventory().addItem(new ItemStack(mat, payouts.getOrDefault(slot, 1)));
        }
    }

    public String getGuiKeyByInventory(Player player, Inventory inv) {
        Map<String, Inventory> guis = playerGuis.get(player.getUniqueId());
        if (guis == null) return null;
        return guis.entrySet().stream().filter(e -> e.getValue().equals(inv)).map(Map.Entry::getKey).findFirst().orElse(null);
    }
}
