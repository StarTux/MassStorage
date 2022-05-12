package com.cavetale.ms.session;

import com.cavetale.ms.MassStoragePlugin;
import com.cavetale.ms.dialogue.MassStorageDialogue;
import com.cavetale.ms.sql.SQLMassStorage;
import com.cavetale.ms.storable.StorableItem;
import java.util.ArrayList;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class MassStorageSession {
    private final MassStoragePlugin plugin;
    private final UUID uuid;
    private final int[] ids;
    private final int[] amounts;
    @Getter private boolean enabled;
    private MassStorageDialogue dialogue;

    protected MassStorageSession(final MassStoragePlugin plugin, final UUID uuid) {
        this.plugin = plugin;
        this.uuid = uuid;
        final int size = plugin.getIndex().size();
        this.ids = new int[size];
        this.amounts = new int[size];
    }

    public void setup() {
        plugin.getDatabase().scheduleAsyncTask(this::setupAsync);
    }

    private void setupAsync() {
        Date now = new Date();
        for (SQLMassStorage row : plugin.getDatabase().find(SQLMassStorage.class).eq("owner", uuid).findList()) {
            StorableItem storable = plugin.getIndex().get(row);
            if (!storable.isValid()) {
                plugin.getLogger().warning("Invalid row: " + row);
                continue;
            }
            int index = storable.getIndex();
            ids[index] = row.getId();
            amounts[index] = row.getAmount();
            row.setUpdated(now);
            plugin.getDatabase().update(row, "updated");
        }
        for (int i = 0; i < ids.length; i += 1) {
            if (ids[i] != 0) continue;
            SQLMassStorage row = new SQLMassStorage(uuid, plugin.getIndex().get(i));
            if (plugin.getDatabase().insert(row) == 0) {
                throw new IllegalStateException("Insert failed: " + row);
            }
            ids[i] = row.getId();
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
                enabled = true;
            });
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public void insertAndSubtract(Inventory inventory, Consumer<Map<StorableItem, Integer>> callback) {
        List<ItemStack> items = new ArrayList<>(inventory.getSize());
        for (ItemStack item : inventory) {
            if (item == null || item.getType().isAir()) continue;
            items.add(item);
        }
        insertAndSubtract(items, callback);
    }

    public void insertAndSubtract(List<ItemStack> items, Consumer<Map<StorableItem, Integer>> callback) {
        final Map<StorableItem, Integer> map = new IdentityHashMap<>();
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) continue;
            StorableItem storable = plugin.getIndex().get(item);
            if (!storable.canStore(item)) continue;
            int value = map.getOrDefault(storable, 0);
            int amount = item.getAmount();
            map.put(storable, value + amount);
            item.subtract(amount);
        }
        plugin.getDatabase().scheduleAsyncTask(() -> {
                for (Map.Entry<StorableItem, Integer> entry : map.entrySet()) {
                    insert(entry.getKey(), entry.getValue());
                }
                callback.accept(map);
            });
    }

    /**
     * Insert an item.  The storable must be valid and the amount
     * positive!
     *
     * An insert should never fail, so if it happens after all, we
     * spam console.
     */
    public boolean insert(StorableItem storable, int amount) {
        final int result = plugin.getDatabase().update(SQLMassStorage.class)
            .add("amount", amount)
            .where(c -> c.eq("id", ids[storable.getIndex()]))
            .sync();
        final boolean success = result != 0;
        if (!success) {
            plugin.getLogger().severe("Insert failed!"
                                      + " result=" + result
                                      + " player=" + uuid
                                      + " item=" + amount + "x" + storable);
        } else {
            amounts[storable.getIndex()] += amount;
        }
        return success;
    }

    public void insertAsync(StorableItem storable, int amount, Consumer<Boolean> callback) {
        plugin.getDatabase().scheduleAsyncTask(() -> {
                boolean result = insert(storable, amount);
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(result));
            });
    }

    /**
     * Retrieval is expected to fail when the backing storage does not
     * have enough amount in store.  Thus, we anticipate the fail
     * state without an error.
     */
    public boolean retrieve(StorableItem storable, int amount) {
        final int index = storable.getIndex();
        final int result = plugin.getDatabase().update(SQLMassStorage.class)
            .subtract("amount", amount)
            .where(c -> c
                   .eq("id", ids[index])
                   .gte("amount", amount))
            .sync();
        boolean success = result != 0;
        if (success) {
            amounts[index] -= amount;
        }
        return success;
    }

    public void retrieveAsync(StorableItem storable, int amount, Consumer<Boolean> callback) {
        plugin.getDatabase().scheduleAsyncTask(() -> {
                boolean result = retrieve(storable, amount);
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(result));
            });
    }

    public int getAmount(StorableItem storable) {
        return amounts[storable.getIndex()];
    }

    public void complete(List<String> result, String arg) {
        String lower = arg.toLowerCase();
        for (int i = 0; i < amounts.length; i += 1) {
            if (amounts[i] == 0) continue;
            StorableItem storable = plugin.getIndex().get(i);
            String lname = storable.getName().toLowerCase();
            if (lname.contains(lower)) {
                result.add(lname);
            }
        }
    }

    public List<StorableItem> allStorables() {
        List<StorableItem> result = new ArrayList<>();
        for (int i = 0; i < amounts.length; i += 1) {
            if (amounts[i] == 0) continue;
            StorableItem storable = plugin.getIndex().get(i);
            result.add(storable);
        }
        return result;
    }

    public List<StorableItem> storables(String arg) {
        String lower = arg.toLowerCase();
        List<StorableItem> result = new ArrayList<>();
        for (int i = 0; i < amounts.length; i += 1) {
            if (amounts[i] == 0) continue;
            StorableItem storable = plugin.getIndex().get(i);
            if (storable.getName().toLowerCase().contains(lower)) {
                result.add(storable);
            }
        }
        return result;
    }

    public List<StorableItem> filter(List<StorableItem> in) {
        List<StorableItem> result = new ArrayList<>();
        for (StorableItem it : in) {
            if (amounts[it.getIndex()] > 0) {
                result.add(it);
            }
        }
        return result;
    }

    public int count(List<StorableItem> in) {
        int result = 0;
        for (StorableItem it : in) {
            result += amounts[it.getIndex()];
        }
        return result;
    }

    public MassStorageDialogue getDialogue() {
        if (dialogue == null) {
            dialogue = new MassStorageDialogue(plugin, this);
        }
        return dialogue;
    }
}
