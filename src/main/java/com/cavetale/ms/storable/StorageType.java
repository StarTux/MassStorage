package com.cavetale.ms.storable;

import com.cavetale.core.command.CommandWarn;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum StorageType {
    INVALID(-1),
    BUKKIT(0),
    MYTEMS(1),
    POTION(2),
    SPLASH_POTION(3),
    LINGERING_POTION(4),
    TIPPED_ARROW(5),
    ENCHANTED_BOOK(6);

    public final int id;

    public static @NonNull StorageType of(int ofId) {
        for (StorageType it : StorageType.values()) {
            if (it.id == ofId) return it;
        }
        return INVALID;
    }

    public static StorageType require(String arg) {
        try {
            return valueOf(arg.toUpperCase());
        } catch (IllegalArgumentException iae) {
            throw new CommandWarn("Storage type not found: " + arg);
        }
    }
}
