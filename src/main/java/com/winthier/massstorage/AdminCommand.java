package com.winthier.massstorage;

import com.winthier.massstorage.sql.SQLItem;
import com.winthier.massstorage.sql.SQLPlayer;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public final class AdminCommand implements CommandExecutor {
    final MassStoragePlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = args.length > 0 ? args[0].toLowerCase() : null;
        Player player = sender instanceof Player ? (Player)sender : null;
        if (cmd == null) {
            return false;
        } else if (cmd.equals("reload") && args.length == 1) {
            plugin.reloadAll();
            sender.sendMessage("Configuration reloaded.");
        } else if (cmd.equals("debug") && args.length == 1) {
            if (player == null) return false;
            boolean newVal = !plugin.getSession(player).isDebugModeEnabled();
            plugin.getSession(player).setDebugModeEnabled(newVal);
            if (newVal) {
                sender.sendMessage("Debug mode enabled");
            } else {
                sender.sendMessage("Debug mode disabled");
            }
        } else if (cmd.equals("info") && args.length == 2) {
            SQLPlayer sqlPlayer = SQLPlayer.find(args[1]);
            if (sqlPlayer == null) {
                sender.sendMessage("Player not found: " + args[1]);
                return true;
            }
            int total = 0;
            List<SQLItem> items = SQLItem.find(plugin, sqlPlayer.getUuid());
            for (SQLItem item: items) total += item.getAmount();
            sender.sendMessage("Storage info of " + sqlPlayer.getName());
            sender.sendMessage("Used: " + total + "/" + sqlPlayer.getCapacity());
            sender.sendMessage("Free: " + (sqlPlayer.getCapacity() - total));
            StringBuilder sb = new StringBuilder("Items");
            for (SQLItem item: items) sb.append(" ").append(item.getAmount()).append("x").append(plugin.getItemName(item.getItem().toItemStack()));
            sender.sendMessage(sb.toString());
        } else if (cmd.equals("grant") && args.length == 3) {
            Player target = plugin.getServer().getPlayer(args[1]);
            SQLPlayer sqlPlayer;
            if (target == null) {
                sqlPlayer = SQLPlayer.find(args[1]);
                if (sqlPlayer == null) {
                    sender.sendMessage("Player not found: " + args[1]);
                    return true;
                }
            } else {
                sqlPlayer = plugin.getSession(target).getSQLPlayer();
            }
            int amount = Integer.parseInt(args[2]);
            int itemAmount = plugin.getConfig().getInt("BuyCapacity.Amount", 6 * 9 * 64) * amount;
            sqlPlayer.setCapacity(Math.max(0, sqlPlayer.getCapacity() + itemAmount));
            plugin.getDb().save(sqlPlayer);
            sender.sendMessage("Adjusted capacity of " + sqlPlayer.getName() + " by " + itemAmount + ". Total: " + sqlPlayer.getCapacity());
        } else if (cmd.equals("category") && args.length >= 2) {
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
        } else {
            return false;
        }
        return true;
    }
}
