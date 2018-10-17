package com.winthier.massstorage;

import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@RequiredArgsConstructor
public class InventoryListener implements Listener {
    final MassStoragePlugin plugin;

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player)event.getPlayer();
        plugin.getSession(player).onInventoryClose();
        if (event.getInventory().getHolder() instanceof MenuInventory) {
            MenuInventory menu = (MenuInventory)event.getInventory().getHolder();
            menu.onInventoryOpen(event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player)event.getPlayer();
        plugin.getSession(player).onInventoryClose();
        if (event.getInventory().getHolder() instanceof MenuInventory) {
            MenuInventory menu = (MenuInventory)event.getInventory().getHolder();
            menu.onInventoryClose(event);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player)event.getWhoClicked();
        Session session = plugin.getSession(player);
        if (session.getInventory() != null && event.getSlot() < 0 && event.getCursor().getType() == Material.AIR) {
            // Click outside the window
            event.setCancelled(true);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                    new MenuInventory(plugin, player).open();
                });
        }
        if (!event.isCancelled() && event.getInventory().getHolder() instanceof MenuInventory) {
            MenuInventory menu = (MenuInventory)event.getInventory().getHolder();
            event.setCancelled(true);
            menu.onInventoryClick(event);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player)event.getWhoClicked();
        if (!event.isCancelled() && event.getInventory().getHolder() instanceof MenuInventory) {
            MenuInventory menu = (MenuInventory)event.getInventory().getHolder();
            event.setCancelled(true);
            menu.onInventoryDrag(event);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getSessions().remove(event.getPlayer().getUniqueId());
    }
}
