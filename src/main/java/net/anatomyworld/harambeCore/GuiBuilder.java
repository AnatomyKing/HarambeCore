package net.anatomyworld.harambeCore;

import net.anatomyworld.harambeCore.item.ItemRegistry;
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

    /* ---------------- cached data maps ---------------- */
    private final Map<UUID, Map<String, Inventory>> playerGuis              = new HashMap<>();
    private final Map<String, Map<Integer, SlotType>>    guiSlotTypes       = new HashMap<>();
    private final Map<String, Map<Integer, String>>     buttonLogicCache    = new HashMap<>();
    private final Map<String, Map<Integer, Double>>     guiSlotCosts        = new HashMap<>();
    private final Map<String, Map<Integer, Boolean>>    guiCostPerStack     = new HashMap<>();
    private final Map<String, Map<Integer, Boolean>>    guiCostIsPayout     = new HashMap<>();
    private final Map<String, Map<Integer, String>>     guiAcceptedItems    = new HashMap<>();
    private final Map<String, Map<Integer, Integer>>    guiAcceptedAmounts  = new HashMap<>();
    private final Map<String, Map<Integer, String>>     guiOutputItems      = new HashMap<>();
    private final Map<String, Map<Integer, Integer>>    guiPayoutAmounts    = new HashMap<>();
    private final Map<String, Map<Integer, InputActionType>> guiInputActions  = new HashMap<>();
    private final Map<String, Map<Integer, List<Integer>>>   guiSlotConnections = new HashMap<>();
    private final Map<String, Map<Integer, Integer>>         guiReverseConnections = new HashMap<>();
    private final Map<String, Map<Integer, String>> guiCheckItems   = new HashMap<>();
    private final Map<String, Map<Integer, String>> guiRewardGroups = new HashMap<>();

    private final ItemRegistry itemRegistry;
    private FileConfiguration  config;
    private ItemStack          cachedFillerItem;

    public GuiBuilder(JavaPlugin plugin, FileConfiguration config, ItemRegistry itemRegistry) {
        this.config          = config;
        this.itemRegistry    = itemRegistry;
        this.cachedFillerItem = createFillerItem();
    }

    /* ---------------- public getters ---------------- */

    public Map<String, Map<Integer, SlotType>> getGuiSlotTypes()                   { return guiSlotTypes; }
    public Map<Integer, Double>  getSlotCosts(String key)                          { return guiSlotCosts.getOrDefault(key, Collections.emptyMap()); }
    public Map<Integer, Boolean> getCostPerStack(String key)                       { return guiCostPerStack.getOrDefault(key, Collections.emptyMap()); }
    public Map<Integer, Boolean> getCostIsPayout(String key)                       { return guiCostIsPayout.getOrDefault(key, Collections.emptyMap()); }
    public Map<Integer, String>  getAcceptedItems(String key)                      { return guiAcceptedItems.getOrDefault(key, Collections.emptyMap()); }
    public Map<Integer, Integer> getAcceptedAmounts(String key)                    { return guiAcceptedAmounts.getOrDefault(key, Collections.emptyMap()); }
    public Map<Integer, String>  getOutputItems(String key)                        { return guiOutputItems.getOrDefault(key, Collections.emptyMap()); }
    public Map<Integer, Integer> getPayoutAmounts(String key)                      { return guiPayoutAmounts.getOrDefault(key, Collections.emptyMap()); }
    public Map<Integer, InputActionType> getInputActions(String key)               { return guiInputActions.getOrDefault(key, Collections.emptyMap()); }
    public Map<Integer, List<Integer>>   getSlotConnections(String key)            { return guiSlotConnections.getOrDefault(key, Collections.emptyMap()); }
    public Map<Integer, Integer>         getReverseSlotConnections(String key)     { return guiReverseConnections.getOrDefault(key, Collections.emptyMap()); }
    public Map<Integer, String>          getCheckItems(String key)                 { return guiCheckItems.getOrDefault(key, Collections.emptyMap()); }
    public Map<Integer, String>          getRewardGroups(String key)               { return guiRewardGroups.getOrDefault(key, Collections.emptyMap()); }

    /* ---------------- cache reset on config reload ---------------- */

    public void updateConfig(FileConfiguration config) {
        this.config = config;
        playerGuis.clear();
        guiSlotTypes.clear();
        buttonLogicCache.clear();
        guiSlotCosts.clear();
        guiCostPerStack.clear();
        guiCostIsPayout.clear();
        guiAcceptedItems.clear();
        guiAcceptedAmounts.clear();
        guiOutputItems.clear();
        guiPayoutAmounts.clear();
        guiInputActions.clear();
        guiSlotConnections.clear();
        guiReverseConnections.clear();
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

    /* --------------------------------------------------------------- */
    /*               GUI creation & parsing                            */
    /* --------------------------------------------------------------- */

    private Inventory generateGui(String guiKey) {
        ConfigurationSection guiSection = config.getConfigurationSection("gui." + guiKey);
        if (guiSection == null) return null;

        String title = guiSection.getString("title", "&cUnnamed GUI");
        int size     = guiSection.getInt("size", 54);
        Component titleComponent = LegacyComponentSerializer.legacySection().deserialize(title);
        Inventory gui = Bukkit.createInventory(null, size, titleComponent);

        Map<Integer, SlotType>        slotTypes       = new HashMap<>();
        Map<Integer, String>          buttonLogics    = new HashMap<>();
        Map<Integer, Double>          slotCosts       = new HashMap<>();
        Map<Integer, Boolean>         perStackMap     = new HashMap<>();
        Map<Integer, Boolean>         payoutMap       = new HashMap<>();
        Map<Integer, String>          acceptedItems   = new HashMap<>();
        Map<Integer, Integer>         acceptedAmounts = new HashMap<>();
        Map<Integer, String>          outputItems     = new HashMap<>();
        Map<Integer, Integer>         payoutAmounts   = new HashMap<>();
        Map<Integer, InputActionType> inputActions    = new HashMap<>();
        Map<Integer, List<Integer>>   slotConnections = new HashMap<>();
        Map<Integer, String>          checkItems      = new HashMap<>();
        Map<Integer, String>          rewardGroups    = new HashMap<>();

        ConfigurationSection buttonsSection = guiSection.getConfigurationSection("buttons");
        if (buttonsSection != null) {
            for (String buttonKey : buttonsSection.getKeys(false)) {
                ConfigurationSection bc = buttonsSection.getConfigurationSection(buttonKey);
                if (bc == null) continue;

                List<Integer> slots   = bc.getIntegerList("slot");
                SlotType      slotType;
                try { slotType = SlotType.valueOf(bc.getString("type", "FILLER").toUpperCase(Locale.ROOT)); }
                catch (IllegalArgumentException ex) { continue; }

                /* ----- cost parsing (eco, per_stack, payout) ----- */
                double  ecoCost  = 0.0;
                boolean costPS   = false;
                boolean costPay  = false;
                Object  rc       = bc.get("cost");
                if (rc instanceof Number n) {
                    ecoCost = n.doubleValue();
                } else if (rc instanceof ConfigurationSection cs) {
                    ecoCost  = cs.getDouble("eco", 0.0);
                    costPS   = cs.getBoolean("per_stack", false);
                    costPay  = cs.getBoolean("payout", false);
                }

                switch (slotType) {

                    /* ---------------- INPUT_SLOT ---------------- */
                    case INPUT_SLOT -> {
                        InputActionType ia = InputActionType.valueOf(
                                bc.getString("action", "NONE").toUpperCase(Locale.ROOT));
                        String rGroup = extractRewardGroup(bc);

                        if (bc.isString("accepted_item")) {
                            String mat = bc.getString("accepted_item");
                            for (int s : slots) acceptedItems.put(s, mat == null ? "" : mat.toUpperCase(Locale.ROOT));
                        } else if (bc.isConfigurationSection("accepted_item")) {
                            ConfigurationSection ai = bc.getConfigurationSection("accepted_item");
                            assert ai != null;
                            String mat  = ai.getString("material");
                            String myth = ai.getString("mythic");
                            int    amt  = ai.getInt("amount", -1);
                            String rg   = ai.getString("reward_group", null);

                            for (int s : slots) {
                                if (mat  != null) acceptedItems.put(s, mat.toUpperCase(Locale.ROOT));
                                if (myth != null) acceptedItems.put(s, "MYTHIC:" + myth);
                                if (amt > 0)     acceptedAmounts.put(s, amt);
                                if (rg != null)  rewardGroups.put(s, rg);
                            }
                        }

                        for (int s : slots) {
                            slotTypes.put(s, slotType);
                            inputActions.put(s, ia);
                            if (ecoCost > 0) slotCosts.put(s, ecoCost);
                            if (costPS)      perStackMap.put(s, true);
                            if (costPay)     payoutMap.put(s, true);
                            if (rGroup != null) rewardGroups.put(s, rGroup);
                        }
                    }

                    /* ---------------- OUTPUT_SLOT --------------- */
                    case OUTPUT_SLOT -> {
                        ActionType at = ActionType.valueOf(bc.getString("action", "REWARD_GET").toUpperCase(Locale.ROOT));
                        String rGroup = extractRewardGroup(bc);
                        for (int s : slots) {
                            slotTypes.put(s, slotType);
                            if (ecoCost > 0) slotCosts.put(s, ecoCost);
                            if (costPS)      perStackMap.put(s, true);
                            if (costPay)     payoutMap.put(s, true);
                            if (rGroup != null && at == ActionType.REWARD_GET) rewardGroups.put(s, rGroup);
                        }
                    }

                    /* ------------- BUTTON / CHECK_BUTTON -------- */
                    case BUTTON, CHECK_BUTTON -> {
                        ActionType at    = ActionType.valueOf(bc.getString("action", "COMMAND").toUpperCase(Locale.ROOT));
                        String     logic = bc.getString("logic");

                        String outItem = null;
                        int    payAmt  = 1;
                        Object oi      = bc.get("output_item");
                        if (oi instanceof ConfigurationSection oc) {
                            if (oc.contains("material"))
                                outItem = Objects.requireNonNull(oc.getString("material")).toUpperCase(Locale.ROOT);
                            else if (oc.contains("mythic"))
                                outItem = "MYTHIC:" + oc.getString("mythic");
                            payAmt = oc.getInt("amount", 1);
                        } else if (oi instanceof String s) {
                            outItem = s.toUpperCase(Locale.ROOT);
                            payAmt  = bc.getInt("payout_amount", 1);
                        }

                        String connectKey = bc.getString("slot_connection");
                        String rGroup     = extractRewardGroup(bc);
                        List<Integer> connected = new ArrayList<>();
                        if (connectKey != null && buttonsSection.isConfigurationSection(connectKey))
                            connected = Objects.requireNonNull(
                                    buttonsSection.getConfigurationSection(connectKey)).getIntegerList("slot");

                        /* ---- button item design ---- */
                        ConfigurationSection d = bc.getConfigurationSection("design");
                        ItemStack btnItem;
                        if (d != null && d.contains("mythic")) {
                            btnItem = itemRegistry.getItem(d.getString("mythic"));
                            if (btnItem == null) btnItem = new ItemStack(Material.BARRIER);
                        } else {
                            String matName  = d != null ? d.getString("material", "STONE") : "STONE";
                            String itemName = d != null ? d.getString("name", "&fButton") : "&fButton";
                            int    cmd      = d != null ? d.getInt("custom_model_data", 0) : 0;
                            btnItem = createButtonItem(matName, itemName, cmd);
                        }

                        for (int s : slots) {
                            gui.setItem(s, btnItem);
                            slotTypes.put(s, slotType);
                            if (ecoCost > 0) slotCosts.put(s, ecoCost);
                            if (costPS)      perStackMap.put(s, true);
                            if (costPay)     payoutMap.put(s, true);
                            if (at == ActionType.COMMAND && logic != null) buttonLogics.put(s, logic);
                            if (at == ActionType.GIVE && outItem != null) {
                                outputItems.put(s, outItem);
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

        /* -------- filler -------- */
        for (int i = 0; i < size; i++) {
            if (!slotTypes.containsKey(i)) {
                gui.setItem(i, cachedFillerItem);
                slotTypes.put(i, SlotType.FILLER);
            }
        }

        /* -------- reverse map -------- */
        Map<Integer, Integer> reverse = new HashMap<>();
        slotConnections.forEach((checkBtn, inp) -> inp.forEach(s -> reverse.put(s, checkBtn)));

        /* -------- cache -------- */
        guiSlotTypes.put(guiKey, slotTypes);
        buttonLogicCache.put(guiKey, buttonLogics);
        guiSlotCosts.put(guiKey, slotCosts);
        guiCostPerStack.put(guiKey, perStackMap);
        guiCostIsPayout.put(guiKey, payoutMap);
        guiAcceptedItems.put(guiKey, acceptedItems);
        guiAcceptedAmounts.put(guiKey, acceptedAmounts);
        guiOutputItems.put(guiKey, outputItems);
        guiPayoutAmounts.put(guiKey, payoutAmounts);
        guiInputActions.put(guiKey, inputActions);
        guiSlotConnections.put(guiKey, slotConnections);
        guiReverseConnections.put(guiKey, reverse);
        guiCheckItems.put(guiKey, checkItems);
        guiRewardGroups.put(guiKey, rewardGroups);
        return gui;
    }

    /* ---------------- helper methods --------------------------- */

    private String extractRewardGroup(ConfigurationSection section) {
        if (section.isConfigurationSection("accepted_item"))
            return Objects.requireNonNull(
                    section.getConfigurationSection("accepted_item")).getString("reward_group", null);
        if (section.isConfigurationSection("check_item"))
            return Objects.requireNonNull(
                    section.getConfigurationSection("check_item")).getString("reward_group", null);
        return section.getString("reward_group");
    }

    private ItemStack createButtonItem(String materialName, String itemName, int cmd) {
        Material mat = Material.matchMaterial(materialName);
        if (mat == null) return new ItemStack(Material.BARRIER);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta  = item.getItemMeta();
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
        ItemMeta meta  = item.getItemMeta();
        meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(
                config.getString("filler.name", "&8")));
        int cmd = config.getInt("filler.custom_model_data", 0);
        if (cmd > 0) meta.setCustomModelData(cmd);
        item.setItemMeta(meta);
        return item;
    }

    /* --------------- button logic dispatcher ------------------- */

    public void handleButtonClick(Player player, String guiKey, int slot) {
        Map<Integer, String>  logics  = buttonLogicCache.getOrDefault(guiKey, Collections.emptyMap());
        Map<Integer, String>  outputs = guiOutputItems.getOrDefault(guiKey, Collections.emptyMap());
        Map<Integer, Integer> pays    = guiPayoutAmounts.getOrDefault(guiKey, Collections.emptyMap());

        if (logics.containsKey(slot)) {
            String cmdLine = logics.get(slot).replace("%player%", player.getName());
            CommandSender sender;
            if (cmdLine.startsWith("console:")) { sender = Bukkit.getConsoleSender(); cmdLine = cmdLine.substring(8); }
            else if (cmdLine.startsWith("player:")) { sender = player; cmdLine = cmdLine.substring(7); }
            else sender = player;
            if (cmdLine.startsWith("/")) cmdLine = cmdLine.substring(1);
            Bukkit.dispatchCommand(sender, cmdLine.trim());
        } else if (outputs.containsKey(slot)) {
            String raw   = outputs.get(slot);
            int amount   = pays.getOrDefault(slot, 1);
            if (raw.toUpperCase(Locale.ROOT).startsWith("MYTHIC:")) {
                String id = raw.substring("MYTHIC:".length());
                ItemStack myth = itemRegistry.getItem(id);
                if (myth != null) {
                    myth.setAmount(amount);
                    player.getInventory().addItem(myth);
                } else player.sendMessage("§cMythic item '" + id + "' not found.");
            } else {
                Material mat = Material.matchMaterial(raw);
                if (mat != null) player.getInventory().addItem(new ItemStack(mat, amount));
                else player.sendMessage("§cInvalid output_item: " + raw);
            }
        }
    }

    /* --------------- helper: get gui key ----------------------- */

    public String getGuiKeyByInventory(Player player, Inventory inv) {
        Map<String, Inventory> map = playerGuis.get(player.getUniqueId());
        if (map == null) return null;
        return map.entrySet().stream()
                .filter(e -> e.getValue().equals(inv))
                .map(Map.Entry::getKey).findFirst().orElse(null);
    }
}
