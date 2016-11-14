package com.winthier.massstorage;

import java.util.Comparator;
import lombok.Value;

@Value
public class NamedItem {
    static final public Comparator<NamedItem> NAME_COMPARATOR = new Comparator<NamedItem>() {
        @Override public int compare(NamedItem a, NamedItem b) {
            return a.name.compareToIgnoreCase(b.name);
        }
    };
    static final public Comparator<NamedItem> AMOUNT_COMPARATOR = new Comparator<NamedItem>() {
        @Override public int compare(NamedItem a, NamedItem b) {
            return Integer.compare(b.amount, a.amount);
        }
    };
    
    Item item;
    int type, data, amount;
    String name;

    public NamedItem(int type, int data, int amount) {
        this.item = new Item(type, data);
        this.type = type;
        this.data = data;
        this.amount = amount;
        this.name = MassStoragePlugin.getInstance().getVaultHandler().getItemName(item);
    }
}
