package com.cavetale.ms.storable;

import com.cavetale.core.font.VanillaItems;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText;

public final class StorableEnchantedBook implements StorableItem {
    private ItemStack prototype;
    @Getter private final String name;
    @Getter private final Component displayName;
    @Getter private final String sqlName;
    @Getter private final String category;
    @Getter private final int index;

    protected StorableEnchantedBook(final Enchantment enchantment, final int level, final int index) {
        this.prototype = new ItemStack(Material.ENCHANTED_BOOK);
        prototype.editMeta(meta -> {
                ((EnchantmentStorageMeta) meta).addStoredEnchant(enchantment, level, false);
            });
        this.sqlName = enchantment.getKey().getKey() + "_" + level;
        this.displayName = join(noSeparators(),
                                translatable(enchantment),
                                text(" " + roman(level) + " Enchanted Book"));
        this.name = plainText().serialize(displayName);
        this.category = plainText().serialize(translatable(enchantment));
        this.index = index;
    }

    private static String roman(int value) {
        switch (value) {
        case 1: return "I";
        case 2: return "II";
        case 3: return "III";
        case 4: return "IV";
        case 5: return "V";
        default: return "" + value;
        }
    }

    @Override
    public Component getIcon() {
        return VanillaItems.ENCHANTED_BOOK.component;
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.ENCHANTED_BOOK;
    }

    @Override
    public boolean canStore(ItemStack itemStack) {
        return prototype.isSimilar(itemStack);
    }

    @Override
    public boolean canStack(ItemStack itemStack) {
        return prototype.isSimilar(itemStack);
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public ItemStack createItemStack(int amount) {
        return prototype.asQuantity(amount);
    }

    @Override
    public ItemStack createIcon() {
        return prototype.clone();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
