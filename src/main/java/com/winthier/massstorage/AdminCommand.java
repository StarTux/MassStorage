package com.winthier.massstorage;

import com.winthier.generic_events.GenericEvents;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public final class AdminCommand implements CommandExecutor {
    final MassStoragePlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return false;
        Player player = sender instanceof Player ? (Player) sender : null;
        switch (args[0]) {
        case "reload": {
            if (args.length != 1) return false;
            plugin.reloadAll();
            sender.sendMessage("Configuration reloaded.");
            return true;
        }
        case "debug": {
            if (args.length != 1) return false;
            if (player == null) return false;
            boolean newVal = !plugin.getSession(player).isDebugModeEnabled();
            plugin.getSession(player).setDebugModeEnabled(newVal);
            if (newVal) {
                sender.sendMessage("Debug mode enabled");
            } else {
                sender.sendMessage("Debug mode disabled");
            }
            return true;
        }
        case "info": {
            if (args.length != 2) return false;
            UUID uuid = GenericEvents.cachedPlayerUuid(args[1]);
            if (uuid == null) {
                sender.sendMessage("Player not found: " + args[1]);
                return true;
            }
            String name = GenericEvents.cachedPlayerName(uuid);
            int total = 0;
            List<SQLItem> items = SQLItem.find(plugin, uuid);
            for (SQLItem item: items) total += item.getAmount();
            sender.sendMessage("Storage info of " + name);
            sender.sendMessage("Used: " + total);
            StringBuilder sb = new StringBuilder("Items");
            for (SQLItem item: items) sb.append(" ").append(item.getAmount()).append("x").append(plugin.getItemName(item.getItem().toItemStack()));
            sender.sendMessage(sb.toString());
            return true;
        }
        case "category": {
            if (args.length < 2) return false;
            StringBuilder sb = new StringBuilder(args[1]);
            for (int i = 2; i < args.length; i += 1) sb.append(" ").append(args[i]);
            String name = sb.toString();
            Category category = null;
            for (Category cat: plugin.getCategories()) {
                if (cat.name.equalsIgnoreCase(name)) {
                    category = cat;
                    break;
                }
            }
            if (category == null) {
                sender.sendMessage("Category not found: " + name);
                return true;
            }
            sb = new StringBuilder(category.name).append(":");
            for (Material mat: category.materials) sb.append(" ").append(mat.name().toLowerCase());
            sender.sendMessage(sb.toString());
            return true;
        }
        case "misc": {
            if (args.length != 1 && args.length != 2) return false;
            String pat = args.length < 2 ? null : args[1].toUpperCase();
            int count = 0;
            StringBuilder sb = new StringBuilder();
            for (Material mat : plugin.miscMaterials) {
                if (pat != null && !mat.name().contains(pat)) continue;
                sb.append("\n  - ").append(mat.name());
                count += 1;
            }
            sender.sendMessage("" + count + " mats:" + sb.toString());
            return true;
        }
        case "one": {
            if (args.length != 1) return false;
            if (player == null) return false;
            Session session = plugin.getSession(player);
            int count = 0;
            for (Material mat: Material.values()) {
                if (!plugin.getMaterialBlacklist().contains(mat) && mat.isItem() && !mat.isLegacy() && !mat.name().startsWith("LEGACY_")) {
                    SQLItem sqli = session.getSQLItems().get(new Item(mat));
                    if (sqli == null || sqli.getAmount() == 0) {
                        session.storeItems(new ItemStack(mat));
                        count += 1;
                    }
                }
            }
            player.sendMessage(count + " items stored");
            return true;
        }
        default:
            return false;
        }
    }
}
