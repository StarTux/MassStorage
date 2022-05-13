package com.cavetale.ms.storable;

import com.cavetale.mytems.MytemTag;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.util.Text;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import static net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText;

@Getter
public final class StorableMytemsItem implements StorableItem {
    protected final String name;
    protected final Mytems mytems;
    protected final int index;
    protected final String sqlName;
    protected final String category;

    protected StorableMytemsItem(final Mytems mytems, final int index) {
        this.mytems = mytems;
        this.index = index;
        this.sqlName = mytems.id;
        this.name = plainText().serialize(mytems.getMytem().getDisplayName());
        this.category = Text.toCamelCase(mytems.category, " ");
    }

    @Override
    public String toString() {
        return name
            + " mytems=" + mytems
            + " index=" + index
            + " sqlName=" + sqlName
            + " category=" + category;
    }

    @Override
    public Component getDisplayName() {
        return mytems.getMytem().getDisplayName();
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.MYTEMS;
    }

    @Override
    public boolean canStore(ItemStack itemStack) {
        MytemTag tag = mytems.getMytem().serializeTag(itemStack);
        if (tag == null) return true;
        tag.setAmount(null);
        return tag.isDismissable();
    }

    @Override
    public boolean canStack(ItemStack itemStack) {
        return mytems.isItem(itemStack) && canStore(itemStack);
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public ItemStack createItemStack(int amount) {
        return mytems.createItemStack(amount);
    }

    @Override
    public ItemStack createIcon() {
        return mytems.createIcon();
    }

    @Override
    public int getMaxStackSize() {
        return mytems.getMytem().getMaxStackSize();
    }
}
