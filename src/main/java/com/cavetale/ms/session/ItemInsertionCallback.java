package com.cavetale.ms.session;

@FunctionalInterface
public interface ItemInsertionCallback {
    void accept(ItemInsertionResult data);
}
