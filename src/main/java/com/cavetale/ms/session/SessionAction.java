package com.cavetale.ms.session;

import org.bukkit.entity.Player;

public abstract sealed class SessionAction permits SessionWorldContainerAction {
    public abstract void onCancel(Player player);
}
