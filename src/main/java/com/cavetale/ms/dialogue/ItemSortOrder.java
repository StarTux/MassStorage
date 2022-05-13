package com.cavetale.ms.dialogue;

import com.cavetale.ms.session.MassStorageSession;
import com.cavetale.ms.storable.StorableItem;
import com.cavetale.mytems.Mytems;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public enum ItemSortOrder {
    GROUP(4, Mytems.FOLDER::createIcon, "Keep items grouped") {
        @Override public int compare(MassStorageSession session, StorableItem a, StorableItem b) {
            int res = String.CASE_INSENSITIVE_ORDER.compare(a.getCategory(), b.getCategory());
            return res != 0 ? res : NAME.compare(session, a, b);
        }
    },
    NAME(3, Mytems.LETTER_A::createIcon, "Sort by name") {
        @Override public int compare(MassStorageSession session, StorableItem a, StorableItem b) {
            return String.CASE_INSENSITIVE_ORDER.compare(a.getName(), b.getName());
        }
    },
    AMOUNT(5, Mytems.NUMBER_1::createIcon, "Sort by amount") {
        @Override public int compare(MassStorageSession session, StorableItem a, StorableItem b) {
            int res = Integer.compare(session.getAmount(b), session.getAmount(a));
            return res != 0 ? res : GROUP.compare(session, a, b);
        }
    };

    public final int slot;
    private final Supplier<ItemStack> itemSupplier;
    public final String description;

    public ItemStack createIcon() {
        return itemSupplier.get();
    }

    public abstract int compare(MassStorageSession session, StorableItem a, StorableItem b);
}
