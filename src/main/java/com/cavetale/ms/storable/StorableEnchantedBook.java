package com.cavetale.ms.storable;

import com.cavetale.core.font.VanillaItems;
import java.util.Map;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import static com.cavetale.mytems.util.Text.roman;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText;

public final class StorableEnchantedBook implements StorableItem {
    private ItemStack prototype;
    @Getter private final String name;
    @Getter private final Component displayName;
    @Getter private final Material material;
    @Getter private final String sqlName;
    @Getter private final String category;
    @Getter private final int index;
    private final Enchantment enchantment;
    private final int enchantmentLevel;

    protected StorableEnchantedBook(final Enchantment enchantment, final int level, final int index) {
        this.material = Material.ENCHANTED_BOOK;
        this.prototype = new ItemStack(this.material);
        prototype.editMeta(EnchantmentStorageMeta.class, meta -> meta.addStoredEnchant(enchantment, level, false));
        this.sqlName = enchantment.getKey().getKey() + "_" + level;
        this.displayName = join(noSeparators(),
                                translatable(enchantment),
                                text(" " + roman(level) + " Enchanted Book"));
        this.name = plainText().serialize(displayName);
        this.category = plainText().serialize(translatable(enchantment));
        this.index = index;
        this.enchantment = enchantment;
        this.enchantmentLevel = level;
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
        return prototype.isSimilar(itemStack) || isStorableEnchantedBook(itemStack);
    }

    @Override
    public boolean canStack(ItemStack itemStack) {
        return prototype.isSimilar(itemStack) || isStorableEnchantedBook(itemStack);
    }

    public boolean isStorableEnchantedBook(ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }
        if (itemStack.getType() != Material.ENCHANTED_BOOK) {
            return false;
        }
        if (!(itemStack.getItemMeta() instanceof EnchantmentStorageMeta meta)) {
            return false;
        }
        final Map<Enchantment, Integer> enchants = meta.getStoredEnchants();
        if (enchants.size() != 1) {
            return false;
        }
        final Map.Entry<Enchantment, Integer> storedEnchantment = enchants.entrySet().iterator().next();
        if (storedEnchantment.getKey() != this.enchantment) {
            return false;
        }
        if (storedEnchantment.getValue() != this.enchantmentLevel) {
            return false;
        }
        return true;
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
