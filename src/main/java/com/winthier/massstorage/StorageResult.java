package com.winthier.massstorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;

@Getter
public final class StorageResult {
    protected final List<ItemStack> returnedItems = new ArrayList<>();
    protected final List<ItemStack> storedItems = new ArrayList<>();
    @Setter protected boolean shouldReportEmpty = false;
    protected Map<String, Integer> rejectedItemNames = new HashMap<>();
    protected Map<String, Integer> storedItemNames = new HashMap<>();

    public int getReturnedItemCount() {
        int result = 0;
        for (ItemStack item : returnedItems) result += item.getAmount();
        return result;
    }

    public int getStoredItemCount() {
        int result = 0;
        for (ItemStack item : storedItems) result += item.getAmount();
        return result;
    }

    public void addItemName(Map<String, Integer> map, ItemStack item) {
        String itemName = MassStoragePlugin.instance.getItemName(item);
        Integer amount = map.get(itemName);
        if (amount == null) amount = 0;
        map.put(itemName, amount + item.getAmount());
    }

    public void addRejectedItemName(ItemStack item) {
        addItemName(rejectedItemNames, item);
    }

    public void addStoredItemName(ItemStack item) {
        addItemName(storedItemNames, item);
    }
}
