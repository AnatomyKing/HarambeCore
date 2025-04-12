package net.anatomyworld.harambeCore.storage;

import net.anatomyworld.harambeCore.GuiBuilder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StorageManager {

    private static File baseFolder;
    private static final Map<UUID, Map<String, Integer>> playerPages = new HashMap<>();

    public static void initialize(Plugin plugin) {
        baseFolder = new File(plugin.getDataFolder(), "storage");
        if (!baseFolder.exists() && !baseFolder.mkdirs()) {
            plugin.getLogger().warning("Failed to create storage folder.");
        }
    }

    // Track page
    public static int getCurrentPage(UUID playerId, String guiKey) {
        return playerPages.getOrDefault(playerId, new HashMap<>()).getOrDefault(guiKey, 1);
    }

    public static void setCurrentPage(UUID playerId, String guiKey, int page) {
        playerPages.computeIfAbsent(playerId, k -> new HashMap<>()).put(guiKey, page);
    }

    private static File getPlayerFile(UUID playerId, String guiKey) {
        File guiFolder = new File(baseFolder, guiKey);
        if (!guiFolder.exists() && !guiFolder.mkdirs()) {
            return null;
        }
        return new File(guiFolder, playerId + ".yml");
    }

    public static ItemStack[] loadPage(String guiKey, UUID playerId, int page) {
        File file = getPlayerFile(playerId, guiKey);
        if (file == null || !file.exists()) return new ItemStack[54];

        Map<Integer, ItemStack[]> data = YamlStorage.loadPlayerData(file);
        return data.getOrDefault(page, new ItemStack[54]);
    }

    public static void savePage(String guiKey, UUID playerId, int page, ItemStack[] contents) {
        File file = getPlayerFile(playerId, guiKey);
        if (file == null) return;

        Map<Integer, ItemStack[]> data = YamlStorage.loadPlayerData(file);
        data.put(page, contents);
        YamlStorage.savePlayerData(file, data);
    }

    public static void loadToInventory(Player player, String guiKey, GuiBuilder guiBuilder, int page) {
        Inventory inventory = player.getOpenInventory().getTopInventory();
        ItemStack[] savedItems = loadPage(guiKey, player.getUniqueId(), page);

        Map<Integer, GuiBuilder.SlotType> slotTypes = guiBuilder.getGuiSlotTypes().get(guiKey);
        if (slotTypes == null) return;

        for (int i = 0; i < inventory.getSize(); i++) {
            if (slotTypes.get(i) == GuiBuilder.SlotType.STORAGE_SLOT) {
                inventory.setItem(i, savedItems[i]);
            }
        }

        setCurrentPage(player.getUniqueId(), guiKey, page);
    }

    public static void saveFromInventory(Player player, String guiKey, GuiBuilder guiBuilder) {
        Inventory inventory = player.getOpenInventory().getTopInventory();
        int page = getCurrentPage(player.getUniqueId(), guiKey);
        ItemStack[] contents = loadPage(guiKey, player.getUniqueId(), page); // existing data

        Map<Integer, GuiBuilder.SlotType> slotTypes = guiBuilder.getGuiSlotTypes().get(guiKey);
        if (slotTypes == null) return;

        for (int i = 0; i < inventory.getSize(); i++) {
            if (slotTypes.get(i) == GuiBuilder.SlotType.STORAGE_SLOT) {
                contents[i] = inventory.getItem(i);
            }
        }

        savePage(guiKey, player.getUniqueId(), page, contents);
    }
}
