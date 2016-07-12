package com.winthier.massstorage;

import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

@RequiredArgsConstructor
public class InventoryListener implements Listener {
    final MassStoragePlugin plugin;

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player)event.getPlayer();
        plugin.getSession(player).onInventoryClose();
    }
}
