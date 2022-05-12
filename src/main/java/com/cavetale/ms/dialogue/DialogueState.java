package com.cavetale.ms.dialogue;

public abstract sealed class DialogueState permits DialogueItems, DialogueOverview, DialogueItem {
    protected int pageIndex;
}
