package com.cavetale.ms;

import com.cavetale.mytems.Mytems;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public enum ItemSortOrder {
    NAME(Mytems.LETTER_A::createIcon, "Sort by name"),
    AMOUNT(Mytems.NUMBER_1::createIcon, "Sort by amount");

    private final Supplier<ItemStack> itemSupplier;
    public final String description;

    public ItemStack createIcon() {
        return itemSupplier.get();
    }
}
