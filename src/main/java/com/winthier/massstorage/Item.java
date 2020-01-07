package com.winthier.massstorage;

import lombok.NonNull;
import lombok.Value;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Value
public final class Item {
    public final Material material;

    public Item(@NonNull final Material material) {
        this.material = material;
    }

    public static Item of(ItemStack itemStack) {
        return new Item(itemStack.getType());
    }

    public ItemStack toItemStack(int amount) {
        return new ItemStack(material, amount);
    }

    public ItemStack toItemStack() {
        return toItemStack(1);
    }
}
