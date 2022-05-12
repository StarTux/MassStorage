package com.cavetale.ms.dialogue;

import com.cavetale.ms.storable.StorableItem;
import com.cavetale.ms.storable.StorableSet;
import java.util.List;
import net.kyori.adventure.text.Component;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class DialogueCategory extends DialogueItems {
    protected StorableSet category;

    public DialogueCategory(final StorableSet category) {
        this.category = category;
    }

    @Override
    public Component getTitle() {
        return join(noSeparators(), text("Category ", GRAY), category.getTitle());
    }

    @Override
    public List<StorableItem> getStorables() {
        return category.getStorables();
    }
}
