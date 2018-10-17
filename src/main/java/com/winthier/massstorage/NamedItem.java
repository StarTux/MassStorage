package com.winthier.massstorage;

import java.util.Comparator;
import lombok.Value;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Value
public final class NamedItem {
    public static final Comparator<NamedItem> NAME_COMPARATOR = new Comparator<NamedItem>() {
        @Override public int compare(NamedItem a, NamedItem b) {
            return a.name.compareToIgnoreCase(b.name);
        }
    };
    public static final Comparator<NamedItem> AMOUNT_COMPARATOR = new Comparator<NamedItem>() {
        @Override public int compare(NamedItem a, NamedItem b) {
            return Integer.compare(b.amount, a.amount);
        }
    };

    private final Item item;
    private final Material material;
    private final int amount;
    private final String name;

    public NamedItem(Material material, int amount) {
        this.item = new Item(material);
        this.material = material;
        this.amount = amount;
        this.name = MassStoragePlugin.getInstance().getItemName(new ItemStack(material, amount));
    }
}
