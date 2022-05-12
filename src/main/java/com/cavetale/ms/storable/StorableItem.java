package com.cavetale.ms.storable;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

public sealed interface StorableItem permits UnstorableItem, StorableBukkitItem, StorableMytemsItem {
    String getName();

    Component getDisplayName();

    StorageType getStorageType();

    String getSqlName();

    /**
     * This function is called with the exception that the itemStack
     * is valid (not air) and has the same type (material, mytems) as
     * the StorableItem.  It only needs to establish whether this item
     * can be boiled down to a SQLMassStorage object (true), or it
     * contains extraneous data (false) and has to be rejected.
     * @param itemStack the ItemStack
     * @return true or false
     */
    boolean canStore(ItemStack itemStack);

    /**
     * Return the index within the StorableItemIndex.  This is used by
     * player sessions.
     * The index is not persistent and thus must never be stored.  It
     * is only valid locally from index population until plugin
     * disable.
     */
    int getIndex();

    boolean isValid();

    ItemStack createItemStack(int amount);

    ItemStack createIcon();

    int getMaxStackSize();
}
