package com.winthier.massstorage;

import com.winthier.massstorage.util.Msg;
import com.winthier.massstorage.sql.SQLItem;
import com.winthier.massstorage.sql.SQLPlayer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
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
    // Cached side effect
    int lastStorageCount = 0;

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

    void onInventoryClose() {
        Inventory inv = this.inventory;
        this.inventory = null;
        if (inv == null) return;
        ItemStack[] rest = storeItems(inv.getContents()).toArray(new ItemStack[0]);
        Player player = getPlayer();
        if (player != null) {
            int restCount = 0;
            for (ItemStack i: rest) restCount += i.getAmount();
            for (ItemStack drop: player.getInventory().addItem(rest).values()) {
                player.getWorld().dropItem(player.getEyeLocation(), drop).setPickupDelay(0);
            }
            if (lastStorageCount > 0 || restCount > 0) {
                StringBuilder sb = new StringBuilder();
                if (lastStorageCount > 0) {
                    sb.append("Stored &a").append(lastStorageCount).append("&r items.");
                }
                if (restCount > 0) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append("Returned &c").append(restCount).append("&r items.");
                }
                if (sb.length() > 0) sb.append(" ");
                sb.append("Free storage: &9").append(getFreeStorage()).append("&r items.");
                Msg.info(player, sb.toString());
            }
        }
    }

    // Returns items that could not be stored
    List<ItemStack> storeItems(ItemStack... items) {
        List<ItemStack> drops = new ArrayList<>();
        int capacity = getCapacity();
        int storage = getStorage();
        Set<SQLItem> dirtyItems = new HashSet<>();
        lastStorageCount = 0;
        for (ItemStack item: items) {
            if (item == null || item.getType() == Material.AIR) {
                // Ignore
            } else if (item.getAmount() <= 0) {
                // Ignore. Warn maybe?
            } else if (storage >= capacity) {
                drops.add(item);
            } else if (!Item.canStore(item)) {
                drops.add(item);
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
                lastStorageCount += storedAmount;
                storage += storedAmount;
                if (item.getAmount() > storedAmount) {
                    item.setAmount(item.getAmount() - storedAmount);
                    drops.add(item);
                }
            }
        }
        MassStoragePlugin.getInstance().getDatabase().save(dirtyItems);
        return drops;
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
