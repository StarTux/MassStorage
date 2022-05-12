package com.cavetale.ms.dialogue;

import com.cavetale.ms.storable.StorableItem;

public final class DialogueItem extends DialogueState {
    protected StorableItem storable;

    public DialogueItem(final StorableItem storable) {
        this.storable = storable;
    }
}
