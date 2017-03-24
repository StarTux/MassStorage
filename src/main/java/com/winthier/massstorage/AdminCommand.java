package com.winthier.massstorage;

import com.winthier.massstorage.sql.SQLItem;
import com.winthier.massstorage.sql.SQLPlayer;
import com.winthier.massstorage.util.Msg;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class AdminCommand implements CommandExecutor {
    final MassStoragePlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = args.length > 0 ? args[0].toLowerCase() : null;
        if (cmd == null) {
            return false;
        } else if (cmd.equals("reload")) {
            plugin.reloadAll();
            sender.sendMessage("Configuration reloaded.");
        } else if (cmd.equals("info") && args.length == 2) {
            SQLPlayer sqlPlayer = SQLPlayer.find(args[1]);
            if (sqlPlayer == null) {
                sender.sendMessage("Player not found: " + args[1]);
                return true;
            }
            int total = 0;
            List<SQLItem> items = SQLItem.find(sqlPlayer.getUuid());
            for (SQLItem item: items) total += item.getAmount();
            sender.sendMessage("Storage info of " + sqlPlayer.getName());
            sender.sendMessage("Used: " + total + "/" + sqlPlayer.getCapacity());
            sender.sendMessage("Free: " + (sqlPlayer.getCapacity() - total));
            StringBuilder sb = new StringBuilder("Items");
            for (SQLItem item: items) sb.append(" ").append(item.getAmount()).append("x").append(plugin.getVaultHandler().getItemName(item.getItem()));
            sender.sendMessage(sb.toString());
        } else if (cmd.equals("grant") && args.length == 3) {
            Player player = plugin.getServer().getPlayer(args[1]);
            SQLPlayer sqlPlayer;
            if (player == null) {
                sqlPlayer = SQLPlayer.find(args[1]);
                if (sqlPlayer == null) {
                    sender.sendMessage("Player not found: " + args[1]);
                    return true;
                }
            } else {
                sqlPlayer = plugin.getSession(player).getSQLPlayer();
            }
            int amount = Integer.parseInt(args[2]);
            int itemAmount = plugin.getConfig().getInt("BuyCapacity.Amount", 6*9*64) * amount;
            sqlPlayer.setCapacity(Math.max(0, sqlPlayer.getCapacity() + itemAmount));
            plugin.getDb().save(sqlPlayer);
            sender.sendMessage("Adjusted capacity of " + sqlPlayer.getName() + " by " + itemAmount + ". Total: " + sqlPlayer.getCapacity());
        } else {
            return false;
        }
        return true;
    }
}
