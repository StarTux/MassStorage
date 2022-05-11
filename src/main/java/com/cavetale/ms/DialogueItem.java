package com.cavetale.ms;

public final class DialogueItem extends DialogueState {
    protected StorableItem storable;

    public DialogueItem(final StorableItem storable) {
        this.storable = storable;
    }
}
