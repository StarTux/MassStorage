package com.winthier.massstorage;

import java.util.Comparator;
import lombok.Value;

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
    public static final Comparator<NamedItem> TYPE_COMPARATOR = new Comparator<NamedItem>() {
        @Override public int compare(NamedItem a, NamedItem b) {
            int c = Integer.compare(a.type, b.type);
            if (c != 0) return c;
            return Integer.compare(a.data, b.data);
        }
    };

    private final Item item;
    private final int type, data, amount;
    private final String name;

    public NamedItem(int type, int data, int amount) {
        this.item = new Item(type, data);
        this.type = type;
        this.data = data;
        this.amount = amount;
        this.name = MassStoragePlugin.getInstance().getVaultHandler().getItemName(item);
    }
}
