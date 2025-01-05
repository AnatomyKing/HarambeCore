package net.anatomyworld.harambefd.guis.enderlink;

import net.anatomyworld.harambefd.GuiBuilder;
import net.anatomyworld.harambefd.harambemethods.YamlUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Map;
import java.util.UUID;

public class EnderlinkMethods {

    // Instead of storing all pages in memory, we only track the "currentPage" for each player.
    // The actual page data is loaded/saved from disk on demand.

    private static File storageFolder;

    // Track current page per player
    private static final Map<UUID, Integer> playerPageMap = new java.util.HashMap<>();

    /**
     * Called once on plugin enable, initializes the storage folder.
     */
    public static void initialize(Plugin plugin) {
        storageFolder = new File(plugin.getDataFolder(), "enderlink");
        if (!storageFolder.exists() && !storageFolder.mkdirs()) {
            plugin.getLogger().warning("Failed to create enderlink storage folder.");
        }
    }

    /**
     * No big "saveAllStorage" needed, because we read/write on demand.
     * But if you like, you could keep it to finalize everything onDisable.
     */
    public static void saveAllStorage() {
        // In an on-demand approach, we have nothing big to store.
        // If you want, you can do nothing here or remove it entirely.
    }

    // ---------------- Page Tracking ----------------

    public static int getCurrentPage(UUID playerId) {
        return playerPageMap.getOrDefault(playerId, 1);
    }

    public static void setCurrentPage(UUID playerId, int page) {
        playerPageMap.put(playerId, page);
    }

    // --------------- On-Disk Page I/O ---------------

    /**
     * Gets the YML file for this player's Enderlink.
     */
    private static File getPlayerFile(UUID playerId) {
        return new File(storageFolder, playerId + ".yml");
    }

    /**
     * Loads the entire Map<Integer, ItemStack[]> from this player's file.
     * If file doesn't exist or is empty, returns a new empty Map.
     */
    private static Map<Integer, ItemStack[]> loadAllPagesFromDisk(UUID playerId) {
        File playerFile = getPlayerFile(playerId);
        if (!playerFile.exists()) {
            return new java.util.HashMap<>();
        }
        return YamlUtil.loadPlayerData(playerFile);
    }

    /**
     * Saves the entire Map<Integer, ItemStack[]> to disk for this player.
     */
    private static void saveAllPagesToDisk(UUID playerId, Map<Integer, ItemStack[]> data) {
        File playerFile = getPlayerFile(playerId);
        YamlUtil.savePlayerData(playerFile, data);
    }

    /**
     * Load a single page from the player's file. If missing, returns a new ItemStack[54].
     */
    private static ItemStack[] loadPageFromDisk(UUID playerId, int page) {
        Map<Integer, ItemStack[]> allPages = loadAllPagesFromDisk(playerId);
        return allPages.computeIfAbsent(page, k -> new ItemStack[54]);
    }

    /**
     * Save a single page. We read the player's file, update just that page, then write back.
     */
    private static void savePageToDisk(UUID playerId, int page, ItemStack[] contents) {
        Map<Integer, ItemStack[]> allPages = loadAllPagesFromDisk(playerId);
        allPages.put(page, contents);
        saveAllPagesToDisk(playerId, allPages);
    }

    // --------------- Public Methods ---------------

    /**
     * Loads the specified page into the Enderlink GUI, skipping filler slots.
     */
    public static void loadPage(Player player, int page, GuiBuilder guiBuilder) {
        UUID playerId = player.getUniqueId();
        setCurrentPage(playerId, page);

        // Get that page from disk
        ItemStack[] savedItems = loadPageFromDisk(playerId, page);

        Inventory top = player.getOpenInventory().getTopInventory();
        // Identify arrow/filler slots from config
        Map<Integer, String> buttonMap = guiBuilder.getButtonKeyMap("enderlink");

        for (int i = 0; i < 54; i++) {
            if (buttonMap != null && buttonMap.containsKey(i)) {
                // skip filler or arrow
                continue;
            }
            if (savedItems[i] != null) {
                top.setItem(i, savedItems[i]);
            } else {
                top.setItem(i, null);
            }
        }
    }

    /**
     * Saves only normal storage slots to disk for the player's current page.
     */
    public static void saveCurrentPage(Player player, GuiBuilder guiBuilder) {
        UUID playerId = player.getUniqueId();
        int page = getCurrentPage(playerId);

        Inventory top = player.getOpenInventory().getTopInventory();
        ItemStack[] pageItems = loadPageFromDisk(playerId, page);
        // ^ We load the existing page from disk first, to keep other pages intact.

        Map<Integer, String> buttonMap = guiBuilder.getButtonKeyMap("enderlink");
        for (int i = 0; i < 54; i++) {
            // skip arrow/filler
            if (buttonMap != null && buttonMap.containsKey(i)) {
                continue;
            }
            pageItems[i] = top.getItem(i);
        }
        // Now we write that single page back
        savePageToDisk(playerId, page, pageItems);
    }

    /**
     * Called when user presses an arrow button.
     */
    public static void handleButtonClick(Player player, String buttonKey, GuiBuilder guiBuilder) {
        UUID playerId = player.getUniqueId();
        int currentPage = getCurrentPage(playerId);

        // Save old page first
        saveCurrentPage(player, guiBuilder);

        switch (buttonKey) {
            case "next_page_button" -> {
                currentPage++;
                setCurrentPage(playerId, currentPage);
                loadPage(player, currentPage, guiBuilder);
                player.sendMessage("Moved to page " + currentPage);
            }
            case "previous_page_button" -> {
                if (currentPage > 1) {
                    currentPage--;
                    setCurrentPage(playerId, currentPage);
                    loadPage(player, currentPage, guiBuilder);
                    player.sendMessage("Moved to page " + currentPage);
                } else {
                    player.sendMessage("You are already on the first page!");
                }
            }
            default -> player.sendMessage("Unknown Enderlink button: " + buttonKey);
        }
    }
}
