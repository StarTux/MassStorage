package com.cavetale.ms.storable;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;

public sealed interface StorableItem permits UnstorableItem, StorableBukkitItem, StorableMytemsItem {
    String getName();

    Component getDisplayName();

    Component getIcon();

    default Component getIconName() {
        return join(noSeparators(), getIcon(), getDisplayName());
    }

    StorageType getStorageType();

    String getSqlName();

    /**
     * The category is used for sorting within the menu.
     */
    String getCategory();

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
     * Determine if the given item stack can stack with items
     * represented by this storable.  Implies that canStore yields
     * true for the same item.
     * In other words, do the same as canStore without assuming that
     * the item type matches.
     */
    boolean canStack(ItemStack itemStack);

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

    /**
     * Try to fit as many items as possible (up to max) into the given
     * inventory.  The fill flag determines if the inventory is
     * actually to be filled or if this is just a count.
     * @param inventory the inventory
     * @param max the maximum number of items
     * @param fill true if the items are to be inserted
     * @return the final item count
     */
    default int fit(Inventory inventory, int max, boolean fill) {
        final int maxStackSize = getMaxStackSize();
        final int size = inventory instanceof PlayerInventory
            ? 4 * 9
            : inventory.getSize();
        int todo = max;
        // Add existing items
        for (int i = 0; i < size && todo > 0; i += 1) {
            ItemStack slot = inventory.getItem(i);
            if (slot == null || !canStack(slot)) continue;
            int stacking = Math.min(todo, maxStackSize - slot.getAmount());
            if (stacking <= 0) continue;
            todo -= stacking;
            if (fill) {
                slot.add(stacking);
            }
        }
        // Add empty slots
        for (int i = 0; i < size && todo > 0; i += 1) {
            ItemStack slot = inventory.getItem(i);
            if (slot != null && !slot.getType().isAir()) continue;
            int stacking = Math.min(todo, maxStackSize);
            todo -= stacking;
            if (fill) {
                inventory.setItem(i, createItemStack(stacking));
            }
        }
        return max - todo;
    }

    /**
     * Determine how many of an item can stack with existing items in
     * a player inventory.  We use this to allow picking up items even
     * while the assistant is on.  Storing items which would usually
     * stack is irritating.
     */
    default int canStack(PlayerInventory inventory, int max, boolean allowEmptySlot) {
        final int maxStackSize = getMaxStackSize();
        int todo = max;
        boolean hasAny = false;
        boolean hasEmpty = false;
        for (int i = 0; i < 40 && todo > 0; i += 1) {
            if (i >= 36 && i <= 39) continue;
            ItemStack slot = inventory.getItem(i);
            if (slot == null || slot.getType().isAir()) {
                hasEmpty = true;
                continue;
            }
            if (!canStack(slot)) continue;
            hasAny = true;
            int stacking = Math.min(todo, maxStackSize - slot.getAmount());
            todo -= stacking;
        }
        int result = max - todo;
        if (result == 0 && allowEmptySlot && !hasAny && hasEmpty) {
            return maxStackSize;
        } else {
            return result;
        }
    }
}
