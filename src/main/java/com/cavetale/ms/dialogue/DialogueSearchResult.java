package com.cavetale.ms.dialogue;

import com.cavetale.core.font.Unicode;
import com.cavetale.ms.storable.StorableItem;
import java.util.List;
import net.kyori.adventure.text.Component;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class DialogueSearchResult extends DialogueItems {
    protected String searchTerm;
    protected List<StorableItem> storables;
    protected Component title;

    public DialogueSearchResult(final String term, final List<StorableItem> storables) {
        this.searchTerm = term;
        this.storables = storables;
        final int max = 26;
        String titleTerm = term.length() <= max ? term : term.substring(0, max) + "...";
        this.title = text(Unicode.tiny(titleTerm), WHITE);
    }

    @Override
    public Component getTitle() {
        return this.title;
    }

    @Override
    public List<StorableItem> getStorables() {
        return this.storables;
    }
}
