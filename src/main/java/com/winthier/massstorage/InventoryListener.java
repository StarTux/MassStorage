package com.winthier.massstorage;

import com.winthier.custom.CustomPlugin;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@RequiredArgsConstructor
public class InventoryListener implements Listener {
    final MassStoragePlugin plugin;

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player)event.getPlayer();
        plugin.getSession(player).onInventoryClose();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player)event.getWhoClicked();
        Session session = plugin.getSession(player);
        if (session.getInventory() != null && event.getSlot() < 0 && event.getCursor().getType() == Material.AIR) {
            event.setCancelled(true);
            plugin.getServer().getScheduler().runTask(plugin, () -> CustomPlugin.getInstance().getInventoryManager().openInventory(player, new MenuInventory(plugin, player)));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getSessions().remove(event.getPlayer().getUniqueId());
    }
}
