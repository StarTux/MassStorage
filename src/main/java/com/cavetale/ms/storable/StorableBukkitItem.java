package com.cavetale.ms.storable;

import java.util.ArrayList;
import java.util.List;
import lombok.Value;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@Value
public final class StorableBukkitItem implements StorableItem {
    protected final String name;
    protected final Component displayName;
    protected final Material material;
    protected final int index;
    protected final String sqlName;
    protected final List<ItemStack> prototypes = new ArrayList<>();

    protected StorableBukkitItem(final Material material, final int index) {
        this.material = material;
        this.index = index;
        this.sqlName = material.name().toLowerCase();
        ItemStack prototype = new ItemStack(material);
        prototypes.add(prototype);
        this.name = prototype.getI18NDisplayName();
        this.displayName = text(name, WHITE);
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.BUKKIT;
    }

    @Override
    public boolean canStore(ItemStack itemStack) {
        for (ItemStack prototype : prototypes) {
            if (prototype.isSimilar(itemStack)) return true;
        }
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public ItemStack createItemStack(int amount) {
        return new ItemStack(material, amount);
    }

    @Override
    public ItemStack createIcon() {
        return new ItemStack(material);
    }

    @Override
    public int getMaxStackSize() {
        return material.getMaxStackSize();
    }
}
