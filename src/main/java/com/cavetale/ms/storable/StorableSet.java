package com.cavetale.ms.storable;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

public interface StorableSet {
    ItemStack getIcon();

    Component getTitle();

    List<StorableItem> getStorables();
}
