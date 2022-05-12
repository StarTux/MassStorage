package com.cavetale.ms.storable;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum StorageType {
    INVALID(-1),
    BUKKIT(0),
    MYTEMS(1);

    public final int id;

    public static @NonNull StorageType of(int ofId) {
        for (StorageType it : StorageType.values()) {
            if (it.id == ofId) return it;
        }
        return INVALID;
    }
}
