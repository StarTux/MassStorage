package com.cavetale.ms.storable;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

public final class UnstorableItem implements StorableItem {
    @Override
    public String getName() {
        throw new IllegalStateException("Cannot get name of unstorable item");
    }

    @Override
    public Component getDisplayName() {
        throw new IllegalStateException("Cannot get display name of unstorable item");
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.INVALID;
    }

    @Override
    public String getSqlName() {
        throw new IllegalStateException("Cannot get SQL name of unstorable item");
    }

    @Override
    public boolean canStore(ItemStack itemStack) {
        return false;
    }

    @Override
    public int getIndex() {
        return -1;
    }

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public ItemStack createItemStack(int amount) {
        throw new IllegalStateException("Cannot create unstorable item");
    }

    @Override
    public ItemStack createIcon() {
        throw new IllegalStateException("Cannot create unstorable item");
    }

    @Override
    public int getMaxStackSize() {
        throw new IllegalStateException("Cannot get max stack size of unstorable item");
    }
}
