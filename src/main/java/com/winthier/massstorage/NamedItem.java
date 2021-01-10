package com.winthier.massstorage;

import java.util.Comparator;
import lombok.Value;
import org.bukkit.Material;

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

    public final Material mat;
    public final int amount;
    public final String name;
    public final String i18nName;

    public boolean matches(String in) {
        return (name != null && name.toLowerCase().contains(in))
            || (i18nName != null && i18nName.toLowerCase().contains(in));
    }

    public boolean equalsName(String in) {
        return (name != null && name.equalsIgnoreCase(in))
            || (i18nName != null && i18nName.equalsIgnoreCase(in));
    }
}
