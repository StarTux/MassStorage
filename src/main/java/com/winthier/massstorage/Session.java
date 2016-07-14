package com.winthier.massstorage;

import com.winthier.massstorage.sql.SQLItem;
import com.winthier.massstorage.sql.SQLPlayer;
import com.winthier.massstorage.util.Msg;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
class Session {
    final UUID uuid;
    final static int CHEST_SIZE = 6*9;

    Inventory inventory = null;
    Map<Item, SQLItem> sqlItems = null;
    SQLPlayer sqlPlayer = null;
    UUID buyConfirmationCode = null;

    Player getPlayer() {
        return Bukkit.getServer().getPlayer(uuid);
    }

    Inventory openInventory() {
        final Player player = getPlayer();
        if (player == null) return null;
        if (inventory != null) return inventory;
        Inventory inv = Bukkit.getServer().createInventory(player, CHEST_SIZE, "Mass Storage");
        player.openInventory(inv);
        this.inventory = inv;
        return inventory;
    }

    void close() {
        if (inventory == null) return;
        Player player = getPlayer();
        if (player == null) return;
        player.closeInventory();
    }

    StorageResult onInventoryClose() {
        Inventory inv = this.inventory;
        this.inventory = null;
        if (inv == null) return null; // Should never happen.
        StorageResult result = storeItems(inv.getContents());
        Player player = getPlayer();
        if (player != null) {
            for (ItemStack drop: player.getInventory().addItem(result.returnedItems.toArray(new ItemStack[0])).values()) {
                player.getWorld().dropItem(player.getEyeLocation(), drop).setPickupDelay(0);
            }
            int storedItemCount = result.getStoredItemCount();
            int returnedItemCount = result.getReturnedItemCount();
            if (storedItemCount > 0 || returnedItemCount > 0) {
                List<Object> messages = new ArrayList<>();
                if (storedItemCount > 0) {
                    StringBuilder tooltip = new StringBuilder("&aStored items: &2").append(storedItemCount);
                    for (Map.Entry<String, Integer> entry: result.storedItemNames.entrySet()) {
                        tooltip.append("\n&a").append(entry.getKey()).append("&2: ").append(entry.getValue());
                    }
                    messages.add(Msg.button(ChatColor.WHITE,
                                            Msg.format("Stored &a%d&r items.", storedItemCount),
                                            Msg.format(tooltip.toString()),
                                            null));
                }
                if (returnedItemCount > 0) {
                    StringBuilder tooltip = new StringBuilder("&4Returned items: &c").append(returnedItemCount);
                    if (result.outOfStorage) {
                        tooltip.append("\n&4Out of storage: &c").append(getCapacity());
                    }
                    if (!result.rejectedItemNames.isEmpty()) {
                        tooltip.append("\n&4Unable to store items:");
                        for (String itemName: result.rejectedItemNames) tooltip.append("\n&8- &c").append(itemName);
                    }
                    messages.add(Msg.button(ChatColor.WHITE,
                                            Msg.format("Returned &c%d&r items.", returnedItemCount),
                                            Msg.format(tooltip.toString()),
                                            null));
                }
                messages.add(Msg.format("Free storage: &9%d&r items.", getFreeStorage()));
                List<Object> json = new ArrayList<>();
                json.add("");
                json.add(Msg.pluginTag());
                for (Object o: messages) {
                    json.add(" ");
                    json.add(o);
                }
                Msg.raw(player, json);
            }
        }
        return result;
    }

    static class StorageResult {
        final List<ItemStack> returnedItems = new ArrayList<>();
        final List<ItemStack> storedItems = new ArrayList<>();
        int getReturnedItemCount() {
            int result = 0;
            for (ItemStack item: returnedItems) result += item.getAmount();
            return result;
        }
        int getStoredItemCount() {
            int result = 0;
            for (ItemStack item: storedItems) result += item.getAmount();
            return result;
        }
        boolean outOfStorage = false;
        Set<String> rejectedItemNames = new HashSet<>();
        Map<String, Integer> storedItemNames = new HashMap<>();
    }

    // Returns items that could not be stored
    StorageResult storeItems(ItemStack... items) {
        List<ItemStack> drops = new ArrayList<>();
        int capacity = getCapacity();
        int storage = getStorage();
        Set<SQLItem> dirtyItems = new HashSet<>();
        StorageResult result = new StorageResult();
        for (ItemStack item: items) {
            if (item == null || item.getType() == Material.AIR) {
                // Ignore
            } else if (item.getAmount() <= 0) {
                // Ignore. Warn maybe?
            } else if (storage >= capacity) {
                result.returnedItems.add(item);
                result.outOfStorage = true;
            } else if (!Item.canStore(item)) {
                result.returnedItems.add(item);
                result.rejectedItemNames.add(MassStoragePlugin.getInstance().getVaultHandler().getItemName(item));
            } else {
                // Fetch SQL item
                Item itemKey = Item.of(item);
                SQLItem sqlItem = getSQLItems().get(itemKey);
                if (sqlItem == null) {
                    sqlItem = SQLItem.of(uuid, itemKey);
                    getSQLItems().put(itemKey, sqlItem);
                }
                dirtyItems.add(sqlItem);
                int storedAmount = Math.min(item.getAmount(), capacity - storage);
                sqlItem.setAmount(sqlItem.getAmount() + storedAmount);
                storage += storedAmount;
                if (item.getAmount() > storedAmount) {
                    item.setAmount(item.getAmount() - storedAmount);
                    drops.add(item);
                }
                result.storedItems.add(item);
                String itemName = MassStoragePlugin.getInstance().getVaultHandler().getItemName(item);
                Integer amount = result.storedItemNames.get(itemName);
                if (amount == null) amount = 0;
                amount += storedAmount;
                result.storedItemNames.put(itemName, amount);
            }
        }
        MassStoragePlugin.getInstance().getDatabase().save(dirtyItems);
        return result;
    }

    int fillInventory(Item... itemKeys) {
        Inventory inv = this.inventory;
        if (inv == null) return 0;
        int invIndex = 0;
        Set<SQLItem> dirtyItems = new HashSet<>();
        int result = 0;
    itemLoop: for (Item itemKey: itemKeys) {
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
        MassStoragePlugin.getInstance().getDatabase().save(dirtyItems);
        return result;
    }

    Map<Item, SQLItem> getSQLItems() {
        if (sqlItems == null) {
            Map<Item, SQLItem> result = new HashMap<>();
            for (SQLItem item: SQLItem.find(uuid)) {
                result.put(item.getItem(), item);
            }
            sqlItems = result;
        }
        return sqlItems;
    }

    SQLPlayer getSQLPlayer() {
        if (sqlPlayer == null) {
            sqlPlayer = SQLPlayer.get(uuid);
            Player player = getPlayer();
            if (player != null) {
                if (sqlPlayer.getName() == null ||
                    !sqlPlayer.getName().equals(player.getName())) {
                    sqlPlayer.setName(player.getName());
                    MassStoragePlugin.getInstance().getDatabase().save(sqlPlayer);
                }
            }
        }
        return sqlPlayer;
    }

    int getStorage() {
        int result = 0;
        for (SQLItem item: getSQLItems().values()) {
            result += item.getAmount();
        }
        return result;
    }

    int getCapacity() {
        return getSQLPlayer().getCapacity();
    }

    int getFreeStorage() {
        return getCapacity() - getStorage();
    }

    int addCapacity(int amount) {
        SQLPlayer sqlPlayer = getSQLPlayer();
        sqlPlayer.setCapacity(sqlPlayer.getCapacity() + amount);
        MassStoragePlugin.getInstance().getDatabase().save(sqlPlayer);
        return sqlPlayer.getCapacity();
    }

    void flush() {
        sqlPlayer = null;
        sqlItems = null;
    }
}
