package com.cavetale.ms.session;

import com.cavetale.ms.storable.StorableItem;
import com.cavetale.ms.storable.StorableSet;
import com.cavetale.mytems.util.BlockColor;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BundleMeta;

@RequiredArgsConstructor
public final class FavoriteSet implements StorableSet {
    public final FavoriteSlot slot;
    protected final List<StorableItem> storables = new ArrayList<>();

    @Override
    public ItemStack getIcon() {
        ItemStack result = new ItemStack(slot.blockColor.getMaterial(BlockColor.Suffix.BUNDLE));
        result.editMeta(BundleMeta.class, meta -> {
                for (StorableItem storable : getStorables()) {
                    meta.addItem(storable.createItemStack(1));
                }
            });
        return result;
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
