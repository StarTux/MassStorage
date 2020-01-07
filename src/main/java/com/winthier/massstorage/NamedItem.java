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

    public final Item item;
    public final int amount;
    public final String name;
}
