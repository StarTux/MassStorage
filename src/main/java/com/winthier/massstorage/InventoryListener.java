package com.winthier.massstorage;

import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

@RequiredArgsConstructor
public class InventoryListener implements Listener {
    final MassStoragePlugin plugin;

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player)event.getPlayer();
        plugin.getSession(player).onInventoryClose();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getSessions().remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        Session session = plugin.getSession(player);
        if (!session.isAutoStorageEnabled()) return;
        long now = System.currentTimeMillis();
        if (session.getLastAutoStorage() + 5000L >= now) return;
        int emptySlots = 0;
        PlayerInventory inv = player.getInventory();
        for (int i = 9; i < 36; i += 1) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getAmount() == 0) {
                emptySlots += 1;
                if (emptySlots > 2) return;
            }
        }
        session.setLastAutoStorage(now);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                Session.StorageResult result = plugin.getSession(player).storePlayerInventory(player);
                plugin.getSession(player).reportStorageResult(player, result);
            });
    }
}
