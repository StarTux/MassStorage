package com.winthier.massstorage;

import com.winthier.massstorage.util.Msg;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

@RequiredArgsConstructor @Getter
final class Session implements InventoryHolder {
    final MassStoragePlugin plugin;
    final UUID uuid;
    static final int CHEST_SIZE = 6 * 9;
    Inventory inventory = null;
    Map<Item, SQLItem> sqlItems = null;
    @Setter boolean autoStorageEnabled = false;
    @Setter boolean debugModeEnabled = false;
    @Setter long lastAutoStorage = 0;
    @Setter int openCategory = -1;
    @Setter boolean informed = false;

    Player getPlayer() {
        return Bukkit.getServer().getPlayer(uuid);
    }

    Inventory openInventory() {
        final Player player = getPlayer();
        if (player == null) return null;
        if (inventory != null) return inventory;
        Inventory inv = Bukkit.getServer().createInventory(this, CHEST_SIZE, "Mass Storage");
        player.openInventory(inv);
        this.inventory = inv;
        player.playSound(player.getEyeLocation(), Sound.BLOCK_CHEST_OPEN, SoundCategory.MASTER, 0.2f, 1.5f);
        return inventory;
    }

    void close() {
        if (inventory == null) return;
        Player player = getPlayer();
        if (player == null) return;
        player.closeInventory();
        onInventoryClose();
    }

    StorageResult onInventoryClose() {
        Inventory inv = this.inventory;
        this.inventory = null;
        if (inv == null) return null;
        StorageResult result = storeItems(inv.getContents());
        Player player = getPlayer();
        if (player != null) reportStorageResult(player, result);
        player.playSound(player.getEyeLocation(), Sound.BLOCK_CHEST_CLOSE, SoundCategory.MASTER, 0.2f, 1.4f);
        return result;
    }

    void reportStorageResult(Player player, StorageResult result) {
        for (ItemStack drop : player.getInventory().addItem(result.returnedItems.toArray(new ItemStack[0])).values()) {
            player.getWorld().dropItem(player.getEyeLocation(), drop).setPickupDelay(0);
        }
        int storedItemCount = result.getStoredItemCount();
        int returnedItemCount = result.getReturnedItemCount();
        if (storedItemCount > 0 || returnedItemCount > 0) {
            List<Object> messages = new ArrayList<>();
            if (storedItemCount > 0) {
                StringBuilder tooltip = new StringBuilder("&aStored items: &2").append(storedItemCount);
                for (Map.Entry<String, Integer> entry : result.storedItemNames.entrySet()) {
                    tooltip.append("\n&a").append(entry.getKey()).append("&2: ").append(entry.getValue());
                }
                messages.add(Msg.button(ChatColor.WHITE,
                                        Msg.format("Stored &a%d&r items.", storedItemCount),
                                        Msg.format(tooltip.toString()),
                                        null));
            }
            if (returnedItemCount > 0) {
                StringBuilder tooltip = new StringBuilder("&4Returned items: &c").append(returnedItemCount);
                if (!result.rejectedItemNames.isEmpty()) {
                    tooltip.append("\n&4Unable to store items:");
                    for (Map.Entry<String, Integer> entry : result.rejectedItemNames.entrySet()) {
                        tooltip.append("\n&8- &c").append(entry.getKey()).append("&4: ").append(entry.getValue());
                    }
                }
                messages.add(Msg.button(ChatColor.WHITE,
                                        Msg.format("Returned &c%d&r items.", returnedItemCount),
                                        Msg.format(tooltip.toString()),
                                        null));
            }
            List<Object> json = new ArrayList<>();
            json.add("");
            json.add(Msg.pluginTag());
            for (Object o : messages) {
                json.add(" ");
                json.add(o);
            }
            Msg.raw(player, json);
        } else if (result.isShouldReportEmpty()) {
            Msg.info(player, "No items were stored.");
        }
    }

    @Getter
    class StorageResult {
        final List<ItemStack> returnedItems = new ArrayList<>();
        final List<ItemStack> storedItems = new ArrayList<>();
        int getReturnedItemCount() {
            int result = 0;
            for (ItemStack item : returnedItems) result += item.getAmount();
            return result;
        }
        int getStoredItemCount() {
            int result = 0;
            for (ItemStack item : storedItems) result += item.getAmount();
            return result;
        }
        @Setter private boolean shouldReportEmpty = false;
        private Map<String, Integer> rejectedItemNames = new HashMap<>();
        private Map<String, Integer> storedItemNames = new HashMap<>();
        void addItemName(Map<String, Integer> map, ItemStack item) {
            String itemName = Session.this.plugin.getItemName(item);
            Integer amount = map.get(itemName);
            if (amount == null) amount = 0;
            map.put(itemName, amount + item.getAmount());
        }
        void addRejectedItemName(ItemStack item) {
            addItemName(rejectedItemNames, item);
        }
        void addStoredItemName(ItemStack item) {
            addItemName(storedItemNames, item);
        }
    }

    StorageResult storePlayerInventory(Player player) {
        PlayerInventory inv = player.getInventory();
        int storage = getStorage();
        Set<SQLItem> dirtyItems = new HashSet<>();
        StorageResult result = new StorageResult();
        for (int i = 9; i < 36; i += 1) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR) {
                continue; // Ignore
            } else if (item.getAmount() <= 0) {
                continue; // Ignore. Warn maybe?
            } else if (!plugin.canStore(item)) {
                continue; // Ignore
            } else {
                // Fetch SQL item
                Item itemKey = Item.of(item);
                SQLItem sqlItem = getSQLItems().get(itemKey);
                if (sqlItem == null) {
                    sqlItem = SQLItem.of(uuid, itemKey);
                    getSQLItems().put(itemKey, sqlItem);
                }
                dirtyItems.add(sqlItem);
                int storedAmount = item.getAmount();
                sqlItem.setAmount(sqlItem.getAmount() + storedAmount);
                storage += storedAmount;
                if (item.getAmount() > storedAmount) {
                    item.setAmount(item.getAmount() - storedAmount);
                    inv.setItem(i, item);
                } else {
                    inv.setItem(i, null);
                }
                result.storedItems.add(item);
                result.addStoredItemName(item);
            }
        }
        if (!dirtyItems.isEmpty()) {
            this.plugin.saveToDatabase(dirtyItems);
        }
        return result;
    }

    // Returns items that could not be stored
    StorageResult storeItems(ItemStack... items) {
        int storage = getStorage();
        Set<SQLItem> dirtyItems = new HashSet<>();
        StorageResult result = new StorageResult();
        for (ItemStack item : items) {
            if (item == null || item.getType() == Material.AIR) {
                continue; // Ignore
            } else if (item.getAmount() <= 0) {
                continue; // Ignore. Warn maybe?
            } else if (!plugin.canStore(item)) {
                result.returnedItems.add(item);
                result.addRejectedItemName(item);
            } else {
                // Fetch SQL item
                Item itemKey = Item.of(item);
                SQLItem sqlItem = getSQLItems().get(itemKey);
                if (sqlItem == null) {
                    sqlItem = SQLItem.of(uuid, itemKey);
                    getSQLItems().put(itemKey, sqlItem);
                }
                dirtyItems.add(sqlItem);
                int storedAmount = item.getAmount();
                sqlItem.setAmount(sqlItem.getAmount() + storedAmount);
                storage += storedAmount;
                if (item.getAmount() > storedAmount) {
                    ItemStack returnedItem = item.clone();
                    returnedItem.setAmount(item.getAmount() - storedAmount);
                    result.returnedItems.add(returnedItem);
                    result.addRejectedItemName(returnedItem);
                    item.setAmount(storedAmount);
                }
                result.storedItems.add(item);
                result.addStoredItemName(item);
            }
        }
        if (!dirtyItems.isEmpty()) {
            this.plugin.saveToDatabase(dirtyItems);
        }
        return result;
    }

    int fillInventory(Item... itemKeys) {
        Inventory inv = this.inventory;
        if (inv == null) return 0;
        int invIndex = 0;
        Set<SQLItem> dirtyItems = new HashSet<>();
        int result = 0;
    itemLoop: for (Item itemKey : itemKeys) {
            do {
                while (inv.getItem(invIndex) != null && inv.getItem(invIndex).getType() != Material.AIR) {
                    invIndex += 1;
                    if (invIndex >= inv.getSize()) break itemLoop;
                }
                SQLItem sqlItem = getSQLItems().get(itemKey);
                if (sqlItem == null || sqlItem.getAmount() <= 0) continue itemLoop;
                int filledAmount = Math.min(sqlItem.getAmount(), itemKey.getMaterial().getMaxStackSize());
                sqlItem.setAmount(sqlItem.getAmount() - filledAmount);
                dirtyItems.add(sqlItem);
                inv.setItem(invIndex, itemKey.toItemStack(filledAmount));
                result += filledAmount;
            } while (true);
        }
        if (!dirtyItems.isEmpty()) {
            this.plugin.saveToDatabase(dirtyItems);
        }
        return result;
    }

    Map<Item, SQLItem> getSQLItems() {
        if (sqlItems == null) {
            Map<Item, SQLItem> result = new HashMap<>();
            for (SQLItem item : SQLItem.find(plugin, uuid)) {
                result.put(item.getItem(), item);
            }
            sqlItems = result;
        }
        return sqlItems;
    }

    int getStorage() {
        int result = 0;
        for (SQLItem item : getSQLItems().values()) {
            result += item.getAmount();
        }
        return result;
    }

    void flush() {
        sqlItems = null;
    }
}
