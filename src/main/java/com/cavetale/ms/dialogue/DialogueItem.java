package com.cavetale.ms.dialogue;

import com.cavetale.ms.storable.StorableItem;
import lombok.Getter;

@Getter
public final class DialogueItem extends DialogueState {
    protected StorableItem storable;

    public DialogueItem(final StorableItem storable) {
        this.storable = storable;
    }
}
