package com.cavetale.ms;

import java.util.List;
import net.kyori.adventure.text.Component;

public final class DialogueCategory extends DialogueItems {
    protected StorableSet category;

    public DialogueCategory(final StorableSet category) {
        this.category = category;
    }

    @Override
    public Component getTitle() {
        return category.getTitle();
    }

    @Override
    public List<StorableItem> getStorables() {
        return category.getStorables();
    }
}
