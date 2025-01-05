package net.anatomyworld.harambefd.harambemethods;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ItemRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ItemRegistry.class);
    private final Map<String, ItemStack> items = new HashMap<>();
    private final File submitItemsFolder;

    public ItemRegistry(Plugin plugin) {
        this.submitItemsFolder = new File(plugin.getDataFolder(), "submititems");
        loadItems();  // Load existing items from file
    }

    // Generates or retrieves an item by its tag
    public ItemStack generateItem(String itemTag) {
        if (itemTag == null || itemTag.isEmpty()) {
            logger.error("Item tag is null or empty.");
            return null;
        }

        ItemStack item = items.get(itemTag.toLowerCase());
        if (item == null) {
            logger.error("No item found with tag: {}", itemTag.toLowerCase());
            return null;
        }
        return item.clone();
    }

    // Register a new item dynamically with its itemTag
    public void registerItem(String itemTag, ItemStack itemStack) {
        items.put(itemTag.toLowerCase(), itemStack);
        logger.info("Registered item with tag: {}", itemTag.toLowerCase());
        saveItem(itemTag, itemStack);
    }

    // Check if an item is registered in the item registry
    public boolean isItemRegistered(ItemStack itemStack) {
        return items.values().stream()
                .anyMatch(registeredItem -> registeredItem.isSimilar(itemStack));  // Compare if the items are similar
    }



    // Save reward item under a specific submit item
    public void registerReward(String submitItemTag, String rewardItemTag, ItemStack rewardItem) {
        File submitFile = new File(submitItemsFolder, submitItemTag.toLowerCase() + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(submitFile);
        config.set("rewarditem." + rewardItemTag.toLowerCase(), rewardItem);

        try {
            config.save(submitFile);
            logger.info("Saved reward '{}' for submit item '{}'.", rewardItemTag, submitItemTag);
        } catch (IOException e) {
            logger.error("Failed to save reward '{}' for submit item '{}' to file: {}", rewardItemTag, submitItemTag, submitFile.getAbsolutePath(), e);
        }
    }

    // Load reward item from the YAML file for a given submit item
    public ItemStack loadRewardItem(String submitItemTag, String rewardItemTag) {
        File submitFile = new File(submitItemsFolder, submitItemTag.toLowerCase() + ".yml");
        if (!submitFile.exists()) {
            logger.error("Submit item file '{}' does not exist.", submitItemTag);
            return null;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(submitFile);
        ItemStack rewardItem = config.getItemStack("rewarditem." + rewardItemTag.toLowerCase());
        if (rewardItem == null) {
            logger.warn("Reward '{}' not found for submit item '{}'.", rewardItemTag, submitItemTag);
        }
        return rewardItem;
    }

    // Load all items from the 'submititems' folder into the registry
    private void loadItems() {
        if (!submitItemsFolder.exists()) {
            logger.warn("Submit items folder does not exist: {}", submitItemsFolder.getAbsolutePath());
            return;
        }

        File[] files = submitItemsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            logger.warn("No submit items found in folder: {}", submitItemsFolder.getAbsolutePath());
            return;
        }

        for (File file : files) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            ItemStack itemStack = config.getItemStack("itemstack");
            if (itemStack != null) {
                String itemTag = file.getName().replace(".yml", "").toLowerCase();
                items.put(itemTag, itemStack);
                logger.info("Loaded item with tag: {}", itemTag);
            } else {
                logger.warn("No itemstack found in file: {}", file.getAbsolutePath());
            }
        }
    }

    // Save individual item to its corresponding YAML file
    private void saveItem(String itemTag, ItemStack itemStack) {
        File itemFile = new File(submitItemsFolder, itemTag.toLowerCase() + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        config.set("itemstack", itemStack);

        try {
            config.save(itemFile);
            logger.info("Item '{}' saved successfully.", itemTag);
        } catch (IOException e) {
            logger.error("Failed to save item '{}' to file: {}", itemTag, itemFile.getAbsolutePath(), e);
        }
    }

    public String getItemTag(ItemStack itemStack) {
        return items.entrySet().stream()
                .filter(entry -> entry.getValue().isSimilar(itemStack))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
}
