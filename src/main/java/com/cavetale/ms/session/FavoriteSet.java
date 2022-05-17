package com.cavetale.ms.session;

import com.cavetale.ms.storable.StorableItem;
import com.cavetale.ms.storable.StorableSet;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public final class FavoriteSet implements StorableSet {
    public final FavoriteSlot slot;
    protected final List<StorableItem> storables = new ArrayList<>();

    @Override
    public ItemStack getIcon() {
        return slot.createIcon();
    }

    @Override
    public Component getTitle() {
        return slot.getDisplayName();
    }

    @Override
    public List<StorableItem> getStorables() {
        return storables;
    }
}
