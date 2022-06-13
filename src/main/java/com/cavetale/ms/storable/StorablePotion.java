package com.cavetale.ms.storable;

import com.cavetale.core.item.ItemKinds;
import com.cavetale.core.util.Json;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import static com.cavetale.core.util.CamelCase.toCamelCase;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText;

/**
 * A StorablePotion is defined by its material, potion type, and
 * an optional enhancement.
 *
 * Materials.  Each material corresponds with exactly one StorageType,
 * and vice versa.  Each Material is an item, and the implied ItemMeta
 * implements PotionMeta.
 * - Potion
 * - SplashPotion
 * - LingeringPotion
 *
 * Enhancements:
 * - None (default)
 * - Upgraded ("strong_")
 * - Extended ("long_")
 *
 * Uniqueness is provided because each material has its own
 * StorageType.  Thus the sqlName must contain the PotionData.  Bukkit
 * does not provide the key for each PotionType, so we have to get it
 * from the serialized item.
 */
public final class StorablePotion implements StorableItem {
    @Getter private final Type type;
    private final ItemStack prototype;
    @Getter private final String name;
    @Getter private final Component displayName;
    @Getter private final String category;
    @Getter private final Component icon;
    @Getter private final String sqlName;
    @Getter private final int index;

    @RequiredArgsConstructor
    public enum Type {
        POTION(Material.POTION, StorageType.POTION),
        SPLASH_POTION(Material.SPLASH_POTION, StorageType.SPLASH_POTION),
        LINGERING_POTION(Material.LINGERING_POTION, StorageType.LINGERING_POTION),
        ARROW(Material.TIPPED_ARROW, StorageType.TIPPED_ARROW);

        public final Material material;
        public final StorageType storageType;

        public static Type get(Material theMaterial) {
            for (Type type : values()) {
                if (theMaterial == type.material) return type;
            }
            return null;
        }
    }

    protected StorablePotion(final Type type, final PotionData potionData, final int index) {
        this.type = type;
        this.prototype = new ItemStack(type.material);
        prototype.editMeta(meta -> {
                if (!(meta instanceof PotionMeta potionMeta)) {
                    throw new IllegalStateException(meta.getClass().getName());
                }
                potionMeta.setBasePotionData(potionData);
            });
        this.icon = ItemKinds.icon(prototype);
        String serialized = prototype.getItemMeta().getAsString();
        Map<?, ?> map = Json.deserialize(serialized, Map.class);
        String potionName = map.get("Potion").toString();
        this.sqlName = potionName.substring(potionName.indexOf(":") + 1);
        this.index = index;
        this.displayName = join(noSeparators(),
                                translatable(prototype),
                                (potionData.isUpgraded() ? text(" II") : empty()),
                                (potionData.isExtended() ? text(" +") : empty()));
        this.name = plainText().serialize(displayName);
        this.category = toCamelCase(" ", type);
    }

    @Override
    public StorageType getStorageType() {
        return type.storageType;
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
        return type.material.getMaxStackSize();
    }
}
