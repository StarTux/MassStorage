package com.cavetale.ms.session;

import com.cavetale.ms.storable.StorableItem;
import lombok.RequiredArgsConstructor;

/**
 * A storable to be displayed in the player HUD.
 */
@RequiredArgsConstructor
public final class StorableDisplay {
    protected final StorableItem storable;
    protected int changedAmount;
    protected long timeout;
}
