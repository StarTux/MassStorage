package com.cavetale.ms;

public abstract sealed class DialogueState permits DialogueItems, DialogueOverview, DialogueItem {
    protected int pageIndex;
}
