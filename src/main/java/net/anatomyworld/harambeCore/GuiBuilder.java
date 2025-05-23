package net.anatomyworld.harambeCore;

import net.anatomyworld.harambeCore.storage.StorageManager;
import net.anatomyworld.harambeCore.item.ItemRegistry;
import net.anatomyworld.harambeCore.util.RandomGibberishNameGenerator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.william278.huskhomes.api.HuskHomesAPI;
import net.william278.huskhomes.position.Home;
import net.william278.huskhomes.user.OnlineUser;
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
import java.util.concurrent.ConcurrentHashMap;

import java.util.*;

public class GuiBuilder {

    public enum SlotType {BUTTON, INPUT_SLOT, CHECK_BUTTON, OUTPUT_SLOT, FILLER, STORAGE_SLOT, HUSKHOME_BUTTON}
    public enum ActionType {COMMAND, GIVE, REWARD_GET, TELEPORT, CREATE, DELETE, RANDOM_TELEPORT, PAGE, DEATH_ITEMS, DEATH_GET}
    public enum InputActionType {NONE, CONSUME}

    /* ---------------- cached data maps ---------------- */
    final Map<UUID, Map<String, Inventory>>                          playerGuis              = new HashMap<>();
    private final Map<String, Map<Integer, SlotType>>                guiSlotTypes            = new HashMap<>();
    private final Map<String, Map<Integer, String>>                  buttonLogicCache        = new HashMap<>();
    private final Map<String, Map<Integer, Double>>                  guiSlotCosts            = new HashMap<>();
    private final Map<String, Map<Integer, Boolean>>                 guiCostPerStack         = new HashMap<>();
    private final Map<String, Map<Integer, Boolean>>                 guiCostIsPayout         = new HashMap<>();
    private final Map<String, Map<Integer, String>>                  guiAcceptedItems        = new HashMap<>();
    private final Map<String, Map<Integer, Integer>>                 guiAcceptedAmounts      = new HashMap<>();
    private final Map<String, Map<Integer, String>>                  guiOutputItems          = new HashMap<>();
    private final Map<String, Map<Integer, Integer>>                 guiPayoutAmounts        = new HashMap<>();
    private final Map<String, Map<Integer, InputActionType>>         guiInputActions         = new HashMap<>();
    private final Map<String, Map<Integer, List<Integer>>>           guiSlotConnections      = new HashMap<>();
    private final Map<String, Map<Integer, Integer>>                 guiReverseConnections   = new HashMap<>();
    private final Map<String, Map<Integer, String>>                  guiCheckItems           = new HashMap<>();
    private final Map<String, Map<Integer, String>>                  guiRewardGroups         = new HashMap<>();
    private final Map<String, List<Integer>>                         huskHomeSlots           = new HashMap<>();
    private final Map<String, Map<ActionType, ConfigurationSection>> huskHomeDesigns         = new HashMap<>();
    private final Map<String, List<Integer>>                         huskHomeCreateSlots     = new HashMap<>();
    private final Map<String, List<Integer>>                         huskHomeDeleteSlots     = new HashMap<>();
    private final Map<String, List<Integer>>                         huskHomeRtpSlots        = new HashMap<>();
    private final Map<String, Map<Integer, Boolean>>                 guiScaleWithOutput      = new HashMap<>();
    private final Map<String, Map<Integer, Boolean>>                 guiCopyItems            = new HashMap<>();
    private final Map<String, String>                                guiRtpWorld             = new HashMap<>();
    private final Map<String, Map<Integer, String>>                  guiSlotPermissions      = new HashMap<>();
    private final Map<UUID, Map<String,Integer>>                     guiPage                 = new ConcurrentHashMap<>();
    private final Map<String,Integer>                                guiMaxPages             = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Map<Integer, ItemStack>>>    sessionCache            = new ConcurrentHashMap<>();
    private static final int                                         PAGE_STRIDE             = 1000;

    private static int  virtualIndex(int page, int localSlot) {return page * PAGE_STRIDE + localSlot;}


    private final JavaPlugin      plugin;
    private final ItemRegistry    itemRegistry;
    private FileConfiguration     config;
    private ItemStack             cachedFillerItem;
    private final StorageManager  storageManager;


    public GuiBuilder(JavaPlugin plugin,
                      FileConfiguration config,
                      ItemRegistry itemRegistry,
                      StorageManager storageManager) {

        this.plugin             = plugin;
        this.config             = config;
        this.itemRegistry       = itemRegistry;
        this.storageManager     = storageManager;
        this.cachedFillerItem   = createFillerItem();


        plugin.getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onQuit(org.bukkit.event.player.PlayerQuitEvent e) {
                UUID id = e.getPlayer().getUniqueId();
                sessionCache.remove(id);   // free the transient item stacks
                playerGuis.remove(id);     // (optional) drop cached Inventory objects
            }
        }, plugin);

    }

    /* ---------------- public getters ---------------- */

    public Map<String, Map<Integer, SlotType>> getGuiSlotTypes()                         { return guiSlotTypes; }
    public Map<Integer, Double>                getSlotCosts(String key)                  { return guiSlotCosts.getOrDefault(key, Collections.emptyMap()); }
    public Map<Integer, Boolean>               getCostPerStack(String key)               { return guiCostPerStack.getOrDefault(key, Collections.emptyMap()); }
    public Map<Integer, Boolean>               getCostIsPayout(String key)               { return guiCostIsPayout.getOrDefault(key, Collections.emptyMap()); }
    public Map<Integer, String>                getAcceptedItems(String key)              { return guiAcceptedItems.getOrDefault(key, Collections.emptyMap()); }
    public Map<Integer, Integer>               getAcceptedAmounts(String key)            { return guiAcceptedAmounts.getOrDefault(key, Collections.emptyMap()); }
    public Map<Integer, String>                getOutputItems(String key)                { return guiOutputItems.getOrDefault(key, Collections.emptyMap()); }
    public Map<Integer, Integer>               getPayoutAmounts(String key)              { return guiPayoutAmounts.getOrDefault(key, Collections.emptyMap()); }
    public Map<Integer, InputActionType>       getInputActions(String key)               { return guiInputActions.getOrDefault(key, Collections.emptyMap()); }
    public Map<Integer, List<Integer>>         getSlotConnections(String key)            { return guiSlotConnections.getOrDefault(key, Collections.emptyMap()); }
    public Map<Integer, Integer>               getReverseSlotConnections(String key)     { return guiReverseConnections.getOrDefault(key, Collections.emptyMap()); }
    public Map<Integer, String>                getCheckItems(String key)                 { return guiCheckItems.getOrDefault(key, Collections.emptyMap()); }
    public Map<Integer, String>                getRewardGroups(String key)               { return guiRewardGroups.getOrDefault(key, Collections.emptyMap()); }
    public Map<Integer, Boolean>               getScaleWithOutput(String key)            { return guiScaleWithOutput.getOrDefault(key, Collections.emptyMap()); }
    public Map<Integer, Boolean>               getCopyItems(String key)                  { return guiCopyItems.getOrDefault(key, Collections.emptyMap()); }
    public Map<Integer, String>                getSlotPermissions(String key)            { return guiSlotPermissions.getOrDefault(key, Collections.emptyMap()); }
    public Map<Integer,String>                 getButtonLogic(String guiKey)             { return buttonLogicCache.getOrDefault(guiKey, Collections.emptyMap()); }

    public int  getPage(UUID id, String key)                                             { return guiPage.getOrDefault(id, Collections.emptyMap()).getOrDefault(key,0);}
    public void setPage(UUID id, String key,int p)                                       { guiPage.computeIfAbsent(id,k->new HashMap<>()).put(key,p);}
    public int  getMaxPages(String key)                                                  { return guiMaxPages.getOrDefault(key,9999);}

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
        guiScaleWithOutput.clear();
        guiPayoutAmounts.clear();
        guiInputActions.clear();
        guiSlotConnections.clear();
        guiReverseConnections.clear();
        guiCheckItems.clear();
        guiRewardGroups.clear();
        guiSlotPermissions.clear();
        guiCopyItems.clear();
        sessionCache.clear();

        //huskhomes
        huskHomeSlots.clear();
        huskHomeDesigns.clear();
        huskHomeCreateSlots.clear();
        huskHomeDeleteSlots.clear();
        huskHomeRtpSlots.clear();
        guiRtpWorld.clear();

        cachedFillerItem = createFillerItem();
    }

    public Set<String> getGuiKeys() {
        ConfigurationSection guiSection = config.getConfigurationSection("gui");
        return guiSection != null ? guiSection.getKeys(false) : Collections.emptySet();
    }

    public void createAndOpenGui(String guiKey, Player player) {
                setPage(player.getUniqueId(), guiKey, 0);
                Inventory gui = generateGui(guiKey, player.getUniqueId(), 0);
                playerGuis.computeIfAbsent(player.getUniqueId(),k->new HashMap<>()).put(guiKey, gui);
        if (gui != null) player.openInventory(gui);
        else player.sendMessage("§cInvalid GUI configuration for '" + guiKey + "'!");
    }

    /* --------------------------------------------------------------- */
    /*               GUI creation & parsing                            */
    /* --------------------------------------------------------------- */

    Inventory generateGui(String guiKey, UUID playerId, int page) {
        ConfigurationSection guiSection = config.getConfigurationSection("gui." + guiKey);
        if (guiSection == null) return null;

        huskHomeSlots.put(guiKey,       new ArrayList<>());
        huskHomeDeleteSlots.put(guiKey, new ArrayList<>());
        huskHomeCreateSlots.put(guiKey, new ArrayList<>());
        huskHomeRtpSlots.put(guiKey,    new ArrayList<>());

        int maxPages = guiSection.getInt("max_pages", 9999);
        guiMaxPages.put(guiKey, maxPages);

        /* add %page% → 1-based page number */
        String rawTitle = guiSection.getString("title", "&cUnnamed GUI")
                .replace("%page%", String.valueOf(page + 1));

        int size = guiSection.getInt("size", 54);
        Component titleComponent =
                LegacyComponentSerializer.legacySection().deserialize(rawTitle);
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
        Map<Integer, Boolean>         scaleMap        = new HashMap<>();
        Map<Integer, Boolean>         copyMap         = new HashMap<>();
        Map<Integer, String>          permMap         = new HashMap<>();

        ConfigurationSection buttonsSection = guiSection.getConfigurationSection("buttons");
        if (buttonsSection != null) {
            for (String buttonKey : buttonsSection.getKeys(false)) {
                ConfigurationSection bc = buttonsSection.getConfigurationSection(buttonKey);
                if (bc == null) continue;                     // 1 – null-guard first

                List<Integer> visible = bc.getIntegerList("pages"); // 2 – page filter
                if (!visible.isEmpty() && !visible.contains(page)) continue;

                List<Integer> slots = bc.getIntegerList("slot");
                SlotType slotType;
                String permLine = bc.getString("perm", null);
                try { slotType = SlotType.valueOf(bc.getString("type", "FILLER").toUpperCase(Locale.ROOT)); }
                catch (IllegalArgumentException ex) { continue; }

                /* ----- cost parsing (eco, per_stack, payout) ----- */
                double  ecoCost  = 0.0;
                boolean costPS   = false;
                boolean costPay  = false;
                boolean scaleOut = false;
                Object  rc       = bc.get("cost");
                if (rc instanceof Number n) {
                    ecoCost = n.doubleValue();
                } else if (rc instanceof ConfigurationSection cs) {
                    ecoCost  = cs.getDouble("eco", 0.0);
                    costPS   = cs.getBoolean("per_stack", false);
                    costPay  = cs.getBoolean("payout", false);
                    scaleOut = cs.getBoolean("scale_with_output", false);
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
                            if (permLine != null) permMap.put(s, permLine);
                            if (ecoCost > 0) slotCosts.put(s, ecoCost);
                            if (costPS)      perStackMap.put(s, true);
                            if (costPay)     payoutMap.put(s, true);
                            if (rGroup != null) rewardGroups.put(s, rGroup);
                        }
                    }

                    /* ---------------- OUTPUT_SLOT --------------- */
                    case OUTPUT_SLOT -> {
                        ActionType at = ActionType.valueOf(
                                bc.getString("action", "REWARD_GET").toUpperCase(Locale.ROOT));
                        String    rGroup = extractRewardGroup(bc);

    /* ─── NEW: for death-chest slots we need both logic *and* an
       entry in rewardGroups so groupMap.get(slot) != null later ─── */
                        if (at == ActionType.DEATH_GET) {
                            for (int s : slots) {
                                buttonLogics.put(s, "DEATH_GET");
                                rewardGroups.put(s, "");  // placeholder so groupMap.containsKey(s) == true
                            }
                        }

                        /* ─── now do the normal OUTPUT_SLOT setup ─── */
                        for (int s : slots) {
                            slotTypes.put(s, SlotType.OUTPUT_SLOT);
                            if (permLine != null)     permMap.put(s, permLine);
                            if (ecoCost > 0)          slotCosts.put(s, ecoCost);
                            if (costPS)               perStackMap.put(s, true);
                            if (costPay)              payoutMap.put(s, true);
                            if (rGroup != null && at == ActionType.REWARD_GET)
                                rewardGroups.put(s, rGroup);
                        }
                    }

                    case STORAGE_SLOT -> {
                        Map<Integer, ItemStack> store = storageManager.getOrCreateStorage(playerId, guiKey);
                        for (int s : slots) {
                            int v = virtualIndex(page, s);        // ← helper instead of page * PAGE_STRIDE + s
                            if (store.containsKey(v)) gui.setItem(s, store.get(v));
                            slotTypes.put(s, slotType);
                            if (permLine != null) permMap.put(s, permLine);
                        }
                    }


                    case HUSKHOME_BUTTON -> {
                        /* -------- determine the button’s action -------- */
                        ActionType at = ActionType.valueOf(
                                bc.getString("action", "TELEPORT").toUpperCase(Locale.ROOT));

                        /* -------- remember the design block once per action -------- */
                        huskHomeDesigns
                                .computeIfAbsent(guiKey, k -> new EnumMap<>(ActionType.class))
                                .putIfAbsent(at, bc.getConfigurationSection("design"));

                        /* -------- paint placeholders & cache cost flags -------- */
                        for (int s : slots) {
                            slotTypes.put(s, slotType);
                            gui.setItem(s, cachedFillerItem);
                            if (permLine != null) permMap.put(s, permLine);
                            if (ecoCost > 0) slotCosts.put(s, ecoCost);
                            if (costPS)      perStackMap.put(s, true);
                            if (costPay)     payoutMap.put(s, true);
                        }

                        /* -------- remember slot lists / logic by action -------- */
                        switch (at) {

                            case TELEPORT -> huskHomeSlots
                                    .computeIfAbsent(guiKey, k -> new ArrayList<>())
                                    .addAll(slots);

                            case CREATE -> huskHomeCreateSlots
                                    .computeIfAbsent(guiKey, k -> new ArrayList<>())
                                    .addAll(slots);

                            case DELETE -> huskHomeDeleteSlots
                                    .computeIfAbsent(guiKey, k -> new ArrayList<>())
                                    .addAll(slots);

                            case RANDOM_TELEPORT -> {
                                String rtpWorld = bc.getString("tel_world", "world");      // read YAML

                                huskHomeRtpSlots                                     // remember slots
                                        .computeIfAbsent(guiKey, k -> new ArrayList<>())
                                        .addAll(slots);

                                guiRtpWorld.put(guiKey, rtpWorld);                   // remember world
                            }
                        }
                    }



                    case BUTTON, CHECK_BUTTON -> {

                        /* ------------------------------------------------------------ */
                        /*  1)  Action & optional page-button special-case               */
                        /* ------------------------------------------------------------ */
                        ActionType at = ActionType.valueOf(
                                bc.getString("action", "COMMAND").toUpperCase(Locale.ROOT));
                        String logic = bc.getString("logic");           // optional custom tag

                        /* ─── PAGE buttons (next / back) ───────────────────────────── */
                        if (at == ActionType.PAGE) {
                            int  delta = bc.getInt("page_offset", 0);          // +1 or −1
                            ConfigurationSection d = bc.getConfigurationSection("design");

                            ItemStack icon = createButtonItem(
                                    d != null ? d.getString("material", "ARROW") : "ARROW",
                                    d != null ? d.getString("name",
                                            delta > 0 ? "&aNext ▶" : "&c◀ Back") : "&aPage",
                                    d != null ? d.getInt("custom_model_data", 0) : 0);

                            for (int s : slots) {
                                gui.setItem(s, icon);
                                slotTypes.put(s, SlotType.BUTTON);
                                buttonLogics.put(s, "page:" + (delta > 0 ? "+" : "") + delta);
                                if (permLine != null) permMap.put(s, permLine);
                            }
                            continue;                 // PAGE finished – skip the rest
                        }

                        /* ─── NEW: mark DEATH_ITEMS / DEATH_GET slots early ─────────── */
                        if (at == ActionType.DEATH_ITEMS || at == ActionType.DEATH_GET) {
                            for (int s : slots) buttonLogics.put(s, at.name());   // "DEATH_ITEMS" | "DEATH_GET"
                        }

                        /* ------------------------------------------------------------ */
                        /*  2)  Optional output_item (for ActionType.GIVE)              */
                        /* ------------------------------------------------------------ */
                        String outItem = null;
                        int    payAmt  = 1;
                        Object oi      = bc.get("output_item");

                        if (oi instanceof ConfigurationSection oc) {
                            String myth = oc.getString("mythic");
                            String mat  = oc.getString("material");

                            if      (myth != null && !myth.isEmpty()) outItem = "MYTHIC:" + myth;
                            else if (mat  != null && !mat.isEmpty())  outItem = mat.toUpperCase(Locale.ROOT);

                            payAmt = oc.getInt("amount", 1);

                        } else if (oi instanceof String s) {
                            outItem = s.toUpperCase(Locale.ROOT);
                            payAmt  = bc.getInt("payout_amount", 1);
                        }

                        /* ------------------------------------------------------------ */
                        /*  3)  Slot-connection / reward-group parsing                  */
                        /* ------------------------------------------------------------ */
                        String connectKey = bc.getString("slot_connection");
                        String rGroup     = extractRewardGroup(bc);

                        List<Integer> connected = new ArrayList<>();
                        if (connectKey != null && buttonsSection.isConfigurationSection(connectKey)) {
                            connected = Objects.requireNonNull(
                                            buttonsSection.getConfigurationSection(connectKey))
                                    .getIntegerList("slot");
                        }

                        /*  copy_item flag (only relevant for CHECK_BUTTON)  */
                        boolean copyFlag = false;
                        if (slotType == SlotType.CHECK_BUTTON && bc.isConfigurationSection("check_item")) {
                            copyFlag = Objects.requireNonNull(
                                            bc.getConfigurationSection("check_item"))
                                    .getBoolean("copy_item", false);
                        }

                        /* ------------------------------------------------------------ */
                        /*  4)  Button item design                                      */
                        /* ------------------------------------------------------------ */
                        ConfigurationSection d = bc.getConfigurationSection("design");
                        ItemStack btnItem;

                        if (d != null && d.contains("mythic")) {
                            btnItem = itemRegistry.getItem(d.getString("mythic"));
                            if (btnItem == null) btnItem = new ItemStack(Material.BARRIER);
                        } else {
                            String matName  = d != null ? d.getString("material", "STONE") : "STONE";
                            String itemName = d != null ? d.getString("name", "&fButton")   : "&fButton";
                            int    cmd      = d != null ? d.getInt("custom_model_data", 0)  : 0;
                            btnItem = createButtonItem(matName, itemName, cmd);
                        }

                        /* ------------------------------------------------------------ */
                        /*  5)  Apply to every physical slot in this logical button     */
                        /* ------------------------------------------------------------ */
                        for (int s : slots) {
                            gui.setItem(s, btnItem);
                            slotTypes.put(s, slotType);

                            if (permLine != null) permMap.put(s, permLine);
                            if (ecoCost  > 0)     slotCosts.put(s, ecoCost);
                            if (costPS)           perStackMap.put(s, true);
                            if (costPay)          payoutMap.put(s, true);
                            if (scaleOut)         scaleMap.put(s, true);

                            if (slotType == SlotType.CHECK_BUTTON && copyFlag) copyMap.put(s, true);

                            /* command logic tag */
                            if (at == ActionType.COMMAND && logic != null)
                                buttonLogics.put(s, logic);

                            /* GIVE-button output item */
                            if (at == ActionType.GIVE && outItem != null) {
                                outputItems.put(s, outItem);
                                payoutAmounts.put(s, payAmt);
                            }

                            /* reward-group for REWARD_GET buttons */
                            if (rGroup != null)          checkItems.put(s, rGroup);

                            /* input-slot connections */
                            if (!connected.isEmpty())    slotConnections.put(s, connected);
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
        guiScaleWithOutput.put(guiKey, scaleMap);
        guiCopyItems.put(guiKey, copyMap);
        guiSlotPermissions.put(guiKey, permMap);


        /* ----------  inject session-cached items  ---------- */
        Map<Integer, ItemStack> sessItems =
                sessionCache
                        .getOrDefault(playerId, Collections.emptyMap())
                        .getOrDefault(guiKey, Collections.emptyMap());

        for (Map.Entry<Integer, ItemStack> entry : sessItems.entrySet()) {
            int virtual    = entry.getKey();
            int targetPage = virtual / PAGE_STRIDE;
            if (targetPage != page) continue;     // belongs to another page

            int local = virtual % PAGE_STRIDE;
            if (local < size) {
                gui.setItem(local, entry.getValue().clone());
            }
        }

        populateHuskHomeButtons(guiKey, gui, Bukkit.getPlayer(playerId));
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

        /* ── 1.  COMMAND / TELEPORT logic ───────────────────────── */
        if (logics.containsKey(slot)) {
            String cmdLine = logics.get(slot).replace("%player%", player.getName());

            // HuskHomes teleport shortcut
            if (cmdLine.startsWith("huskhomes:tp:")) {
                String     homeName = cmdLine.substring("huskhomes:tp:".length());
                OnlineUser user     = HuskHomesAPI.getInstance().adaptUser(player);

                HuskHomesAPI.getInstance().getHome(user, homeName).thenAccept(opt ->
                        opt.ifPresent(home -> HuskHomesAPI.getInstance().teleportBuilder()
                                .teleporter(user)
                                .target(home)
                                .buildAndComplete(true)   // instant teleport
                        )
                );
                return;   // done
            }

            if ("huskhomes:create".equals(cmdLine)) {
                OnlineUser user = HuskHomesAPI.getInstance().adaptUser(player);

                if (HuskHomesAPI.getInstance().getFreeHomeSlots(user) <= 0) {
                    player.sendMessage("§cYou have no free home slots.");
                    return;
                }

                String name = RandomGibberishNameGenerator.generate() + "-" +
                        UUID.randomUUID().toString().substring(0, 5);

                HuskHomesAPI.getInstance()
                        .createHome(user, name,
                                HuskHomesAPI.getInstance().adaptPosition(player.getLocation()))
                        .thenAccept(home -> Bukkit.getScheduler().runTask(plugin, () -> {

                            // 1. feedback
                            player.sendMessage("§aCreated home §e" + name);

                            // 2. rebuild the GUI *synchronously* so every slot & click-logic is fresh
                            int page = getPage(player.getUniqueId(), guiKey);
                            Inventory fresh = generateGui(guiKey, player.getUniqueId(), page);
                            playerGuis.get(player.getUniqueId()).put(guiKey, fresh);
                            player.openInventory(fresh);

                            // 3. (optional) tiny sound or particles here if you like
                        }))
                        .exceptionally(ex -> {
                            Bukkit.getScheduler().runTask(plugin,
                                    () -> player.sendMessage("§cCouldn’t create home: "
                                            + ex.getMessage()));
                            return null;
                        });
                return;
            }

            // ── DELETE a home ─────────────────────────────────────────────
            if (cmdLine.startsWith("huskhomes:del:")) {
                String     homeName = cmdLine.substring("huskhomes:del:".length());
                OnlineUser user     = HuskHomesAPI.getInstance().adaptUser(player);

                // 1. ask HuskHomes to delete the home (it will run async internally)
                HuskHomesAPI.getInstance().deleteHome(user, homeName);

                // 2. tell the player right away
                player.sendMessage("§cDeleted home §e" + homeName);

                // 3. repaint ~0.25 s later (5 ticks) – plenty of time for the cache to update
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    // make sure the player is still viewing the same GUI
                    player.getOpenInventory();
                    if (player.getOpenInventory().getTopInventory().equals(
                    playerGuis.get(player.getUniqueId()).get(guiKey))) {

                        populateHuskHomeButtons(guiKey,
                                player.getOpenInventory().getTopInventory(), player);
                    }
                }, 5L); // 5 ticks = 0.25 s

                return;
            }
            if (cmdLine.startsWith("huskhomes:rtp:")) {
                String worldName = cmdLine.substring("huskhomes:rtp:".length()).trim();
                if (!worldName.isEmpty()) {
                    Bukkit.dispatchCommand(
                            Bukkit.getConsoleSender(),
                            "rtp " + player.getName() + " " + worldName
                    );
                }
                return;
            }

            // Regular console:/player:/ or bare command
            CommandSender sender;
            if (cmdLine.startsWith("console:")) {
                sender  = Bukkit.getConsoleSender();
                cmdLine = cmdLine.substring(8);
            } else if (cmdLine.startsWith("player:")) {
                sender  = player;
                cmdLine = cmdLine.substring(7);
            } else {
                sender = player;
            }
            if (cmdLine.startsWith("/")) cmdLine = cmdLine.substring(1);
            Bukkit.dispatchCommand(sender, cmdLine.trim());
            return;
        }

        /* ── 2.  GIVE-item logic (MYTHIC or vanilla) ────────────── */
        if (outputs.containsKey(slot)) {
            String raw    = outputs.get(slot);
            int    amount = pays.getOrDefault(slot, 1);

            if (raw.toUpperCase(Locale.ROOT).startsWith("MYTHIC:")) {
                String id   = raw.substring("MYTHIC:".length());
                ItemStack it = itemRegistry.getItem(id);
                if (it != null) {
                    it.setAmount(amount);
                    player.getInventory().addItem(it);
                } else {
                    player.sendMessage("§cMythic item '" + id + "' not found.");
                }
            } else {
                Material mat = Material.matchMaterial(raw);
                if (mat != null) {
                    player.getInventory().addItem(new ItemStack(mat, amount));
                } else {
                    player.sendMessage("§cInvalid output_item: " + raw);
                }
            }
        }
    }



    public void handleGuiClose(Player player, Inventory inv) {
        String guiKey = getGuiKeyByInventory(player, inv);
        if (guiKey == null) return;

        UUID uuid = player.getUniqueId();
        int page  = getPage(uuid, guiKey);

        Map<Integer, SlotType> slots = guiSlotTypes.getOrDefault(guiKey, Collections.emptyMap());

        /* ----------------------------------------------------------------
         * 1)  Persist real STORAGE_SLOTs to disk via StorageManager
         * ---------------------------------------------------------------- */
        Map<Integer, ItemStack> storage =
                storageManager.getOrCreateStorage(uuid, guiKey);

        for (Map.Entry<Integer, SlotType> e : slots.entrySet()) {
            if (e.getValue() != SlotType.STORAGE_SLOT) continue;


            int local   = e.getKey();
            int virtual = virtualIndex(page, local);  // ← helper call
            ItemStack it = inv.getItem(local);

            if (it == null || it.getType() == Material.AIR) {
                storage.remove(virtual);
            } else {
                storage.put(virtual, it.clone());
            }
        }
        storageManager.savePlayerStorage(uuid);

        /* ----------------------------------------------------------------
         * 2)  Keep ALL other slot types only in RAM until restart/reload
         * ---------------------------------------------------------------- */
        Map<Integer, ItemStack> sess =
                sessionCache
                        .computeIfAbsent(uuid, k -> new HashMap<>())
                        .computeIfAbsent(guiKey, k -> new HashMap<>());

        for (Map.Entry<Integer, SlotType> e : slots.entrySet()) {
            SlotType t = e.getValue();

            if (t == SlotType.STORAGE_SLOT || t == SlotType.HUSKHOME_BUTTON) continue;


            int local   = e.getKey();

            int virtual = virtualIndex(page, local);
            ItemStack it = inv.getItem(local);

            if (it == null || it.getType() == Material.AIR) {
                sess.remove(virtual);
            } else {
                sess.put(virtual, it.clone());
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


    private ConfigurationSection getDesign(String guiKey, ActionType action) {
        return huskHomeDesigns
                .getOrDefault(guiKey, Collections.emptyMap())
                .get(action);
    }

    private void populateHuskHomeButtons(String guiKey, Inventory gui, Player player) {
        var tpSlots  = huskHomeSlots.get(guiKey);       // TELEPORT row (pearls)
        var delSlots = huskHomeDeleteSlots.get(guiKey); // DELETE   row (barriers)
        var crtSlots = huskHomeCreateSlots.get(guiKey); // CREATE   row  (lime blocks)

        if (player == null) return;

        ConfigurationSection tpDesign  = getDesign(guiKey, ActionType.TELEPORT);
        ConfigurationSection delDesign = getDesign(guiKey, ActionType.DELETE);
        ConfigurationSection crtDesign = getDesign(guiKey, ActionType.CREATE);

        OnlineUser user = HuskHomesAPI.getInstance().adaptUser(player);

        HuskHomesAPI.getInstance().getUserHomes(user).thenAccept(homes ->
                Bukkit.getScheduler().runTask(plugin, () -> {

                    /* ───────────────── TELEPORT + DELETE rows ───────────────── */
                    if (tpSlots != null) {
                        for (int i = 0; i < tpSlots.size(); i++) {
                            int tpSlot = tpSlots.get(i);
                            int delSlot = (delSlots != null && i < delSlots.size()) ? delSlots.get(i) : -1;

                            /* ---- no home for this column → clear both cells ---- */
                            if (i >= homes.size()) {
                                gui.setItem(tpSlot, cachedFillerItem);
                                if (delSlot >= 0) gui.setItem(delSlot, cachedFillerItem);

                                // wipe stale logic
                                Map<Integer, String> logicMap = buttonLogicCache.get(guiKey);
                                if (logicMap != null) {
                                    logicMap.remove(tpSlot);
                                    if (delSlot >= 0) logicMap.remove(delSlot);
                                }

                                // ── NEW: downgrade the slot to a real filler & remove any cost flags ──
                                guiSlotTypes.get(guiKey).put(tpSlot, SlotType.FILLER);
                                guiSlotCosts.get(guiKey).remove(tpSlot);
                                guiCostPerStack.get(guiKey).remove(tpSlot);
                                guiCostIsPayout.get(guiKey).remove(tpSlot);

                                if (delSlot >= 0) {
                                    guiSlotTypes.get(guiKey).put(delSlot, SlotType.FILLER);
                                    guiSlotCosts.get(guiKey).remove(delSlot);
                                    guiCostPerStack.get(guiKey).remove(delSlot);
                                    guiCostIsPayout.get(guiKey).remove(delSlot);
                                }
                                // ---------------------------------------------------------------------

                                continue;
                            }

                            /* ---- there IS a home → (re)draw both icons -------- */
                            Home home = homes.get(i);

                            ItemStack tpIcon = createButtonItem(
                                    tpDesign != null ? tpDesign.getString("material", "ENDER_PEARL") : "ENDER_PEARL",
                                    (tpDesign != null ? tpDesign.getString("name", "&a%home_name%") : "&a%home_name%")
                                            .replace("%home_name%", home.getName()),
                                    tpDesign != null ? tpDesign.getInt("custom_model_data", 0) : 0);

                            gui.setItem(tpSlot, tpIcon);
                            buttonLogicCache
                                    .computeIfAbsent(guiKey, k -> new HashMap<>())
                                    .put(tpSlot, "huskhomes:tp:" + home.getName());

                            if (delSlot >= 0) {
                                ItemStack delIcon = createButtonItem(
                                        delDesign != null ? delDesign.getString("material", "BARRIER") : "BARRIER",
                                        (delDesign != null ? delDesign.getString("name", "&cDelete &a%home_name%") : "&cDelete")
                                                .replace("%home_name%", home.getName()),
                                        delDesign != null ? delDesign.getInt("custom_model_data", 0) : 0);

                                gui.setItem(delSlot, delIcon);
                                buttonLogicCache.get(guiKey).put(delSlot, "huskhomes:del:" + home.getName());
                            }
                        }
                    }

                    /* RANDOM TELEPORT row ----------------------------------------- */
                    var rtpSlots = huskHomeRtpSlots.get(guiKey);
                    if (rtpSlots != null) {
                        String worldName = guiRtpWorld.getOrDefault(guiKey, "world");

                        ItemStack pearl = createButtonItem(
                                "ENDER_PEARL",
                                "&aRandom Teleport",
                                0);

                        for (int s : rtpSlots) {
                            gui.setItem(s, pearl);

                            // re-write click logic every repaint
                            buttonLogicCache
                                    .computeIfAbsent(guiKey, k -> new HashMap<>())
                                    .put(s, "huskhomes:rtp:" + worldName);
                        }
                    }


                    /* ───────────────────── CREATE row ───────────────────────── */
                    if (crtSlots != null) {
                        for (int s : crtSlots) {
                            ItemStack crtIcon = createButtonItem(
                                    crtDesign != null ? crtDesign.getString("material", "LIME_CONCRETE") : "LIME_CONCRETE",
                                    crtDesign != null ? crtDesign.getString("name", "&aCreate Home") : "&aCreate Home",
                                    crtDesign != null ? crtDesign.getInt("custom_model_data", 0) : 0);

                            gui.setItem(s, crtIcon);
                            buttonLogicCache
                                    .computeIfAbsent(guiKey, k -> new HashMap<>())
                                    .put(s, "huskhomes:create");
                        }
                    }
                })
        );
    }



}
