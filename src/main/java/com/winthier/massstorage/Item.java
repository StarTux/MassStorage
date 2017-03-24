package com.winthier.massstorage;

import lombok.Value;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Value
public final class Item {
    private final int type, data;

    public static Item of(ItemStack itemStack) {
        return new Item(itemStack.getType().getId(), (int)itemStack.getDurability());
    }

    public Material getMaterial() {
        return Material.getMaterial(type);
    }

    public ItemStack toItemStack(int amount) {
        return new ItemStack(getMaterial(), amount, (short)data);
    }

    public ItemStack toItemStack() {
        return toItemStack(1);
    }

    public static boolean canStore(ItemStack itemStack) {
        Material mat = itemStack.getType();
        if (MassStoragePlugin.getInstance().getMaterialBlacklist().contains(mat)) return false;
        if (mat.getMaxStackSize() == 1 && !MassStoragePlugin.getInstance().permitNonStackingItems()) return false;
        if (mat.getMaxDurability() > 0 && itemStack.getDurability() > 0) return false;
        if (!of(itemStack).toItemStack().isSimilar(itemStack)) return false;
        return true;
    }
}
