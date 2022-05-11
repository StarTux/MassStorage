package com.cavetale.ms;

import com.cavetale.mytems.MytemTag;
import com.cavetale.mytems.Mytems;
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

    protected StorableMytemsItem(final Mytems mytems, final int index) {
        this.mytems = mytems;
        this.index = index;
        this.sqlName = mytems.id;
        this.name = plainText().serialize(mytems.getMytem().getDisplayName());
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
}
