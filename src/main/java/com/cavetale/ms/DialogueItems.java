package com.cavetale.ms;

import java.util.List;
import net.kyori.adventure.text.Component;

public abstract sealed class DialogueItems extends DialogueState permits DialogueCategory, DialogueSearchResult {
    public abstract Component getTitle();

    public abstract List<StorableItem> getStorables();
}
