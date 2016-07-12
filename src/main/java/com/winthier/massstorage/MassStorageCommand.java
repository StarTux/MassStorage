package com.winthier.massstorage;

import com.winthier.massstorage.sql.SQLItem;
import com.winthier.massstorage.util.Msg;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class MassStorageCommand implements CommandExecutor {
    final MassStoragePlugin plugin;
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = sender instanceof Player ? (Player)sender : null;
        if (player == null) return false;
        String cmd = args.length > 0 ? args[0].toLowerCase() : null;
        if (cmd == null) {
            plugin.getSession(player).openInventory();
            Msg.info(player, "Free storage: &9%d&r items.", plugin.getSession(player).getFreeStorage());
        } else if (cmd.equals("help") || cmd.equals("?")) {
            usage(player);
        } else if (cmd.equals("find") || cmd.equals("search") || cmd.equals("list")) {
            String searchTerm;
            if (args.length > 1) {
                StringBuilder sb = new StringBuilder(args[1]);
                for (int i = 2; i < args.length; ++i) sb.append(" ").append(args[i]);
                searchTerm = sb.toString().toLowerCase();
            } else {
                searchTerm = null;
            }
            List<Object> json = new ArrayList<>();
            int count = 0;
            for (SQLItem sqlItem: plugin.getSession(player).getSQLItems().values()) {
                if (sqlItem.getAmount() <= 0) continue;
                Item item = sqlItem.getItem();
                String itemName = plugin.getVaultHandler().getItemName(item);
                if (searchTerm != null && !itemName.toLowerCase().contains(searchTerm)) continue;
                count += 1;
                json.add(" ");
                json.add(Msg.button(ChatColor.BLUE,
                                    "&r[&9" + sqlItem.getAmount() + "&8x&9" + itemName + "&r]",
                                    "&a" + itemName,
                                    "/ms " + itemName));
            }
            if (count == 0) {
                Msg.warn(player, "Nothing found.");
            } else {
                player.sendMessage("");
                if (searchTerm != null) {
                    Msg.info(player, "%d results for &9%s&r", count, searchTerm);
                } else {
                    Msg.info(player, "%d item types in storage.", count, searchTerm);
                }
                Msg.raw(player, json);
                player.sendMessage("");
            }
        } else if (cmd.equals("buy")) {
            int amount;
            if (args.length == 1) {
                amount = 1;
            } else if (args.length == 2) {
                try {
                    amount = Integer.parseInt(args[1]);
                    if (amount < 1) throw new NumberFormatException();
                } catch (NumberFormatException nfe) {
                    Msg.warn(player, "Number expected: %s.", args[1]);
                    return true;
                }
            } else {
                usage(player);
                return true;
            }
            int itemAmount = plugin.getConfig().getInt("BuyCapacity.Amount", 6*9*64) * amount;
            double price = plugin.getConfig().getDouble("BuyCapacity.Price", 500.0) * (double)amount;
            String displayName = plugin.getConfig().getString("BuyCapacity.DisplayName", "Double Chest");
            String priceFormat = plugin.getVaultHandler().formatMoney(price);
            if (!plugin.getVaultHandler().hasMoney(player, price)) {
                Msg.warn(player, "You cannot afford %s.", priceFormat);
                return true;
            }
            UUID code = UUID.randomUUID();
            plugin.getSession(player).buyConfirmationCode = code;
            Msg.raw(player,
                    Msg.button(ChatColor.WHITE,
                               "Buy &9"+amount+"&8x&9"+displayName+"&r for &9"+priceFormat+"&r? ",
                               "&aThat is "+itemAmount+" items worth of Mass Storage.", null),
                    Msg.button(ChatColor.GREEN,
                               "&r[&aConfirm&r]",
                               "&aConfirm",
                               "/ms confirm " + amount + " " + code));
        } else if (cmd.equals("confirm")) {
            if (args.length != 3) return true;
            int amount;
            try {
                amount = Integer.parseInt(args[1]);
                if (amount < 1) throw new NumberFormatException();
            } catch (NumberFormatException nfe) { return true; }
            UUID code;
            try {
                code = UUID.fromString(args[2]);
            } catch (IllegalArgumentException iae) { return true; }
            if (!code.equals(plugin.getSession(player).buyConfirmationCode)) return true;
            plugin.getSession(player).buyConfirmationCode = null;
            int itemAmount = plugin.getConfig().getInt("BuyCapacity.Amount", 6*9*64) * amount;
            double price = plugin.getConfig().getDouble("BuyCapacity.Price", 500.0) * (double)amount;
            String displayName = plugin.getConfig().getString("BuyCapacity.DisplayName", "Double Chest");
            String priceFormat = plugin.getVaultHandler().formatMoney(price);
            if (!plugin.getVaultHandler().hasMoney(player, price) ||
                !plugin.getVaultHandler().takeMoney(player, price)) {
                Msg.warn(player, "You cannot afford %s.", priceFormat);
                return true;
            }
            plugin.getSession(player).addCapacity(itemAmount);
            Msg.info(player, "Purchased &9%d&8x&9%s&r for &9%s&r.", amount, displayName, priceFormat);
        } else if (cmd.equals("reload") && player.hasPermission("massstorage.admin")) {
            plugin.reloadAll();
            Msg.info(player, "Configuration reloaded.");
        } else {
            StringBuilder sb = new StringBuilder(args[0]);
            for (int i = 1; i < args.length; ++i) sb.append(" ").append(args[i]);
            String searchTerm = sb.toString();
            List<Item> items = plugin.getVaultHandler().findItems(searchTerm);
            plugin.getSession(player).openInventory();
            int freeStorage = plugin.getSession(player).getFreeStorage();
            int displayed = plugin.getSession(player).fillInventory(items.toArray(new Item[0]));
            Msg.info(player, "Found &a%d&r items. Free storage: &9%d items.", displayed, freeStorage);
        }
        return true;
    }

    void usage(Player player) {
        player.sendMessage("");
        Msg.info(player, "&9&lMass Storage&r Help");
        Msg.raw(player, " ", Msg.button("/ms", "&a/ms\n&oOpen Mass Storage Inventory", "/ms"), Msg.format(" &8-&r Open Mass Storage Inventory."));
        Msg.raw(player, " ", Msg.button("/ms list &7[item]", "&a/ms\n&oList Mass Storage Contents", "/ms list "), Msg.format(" &8-&r List Mass Storage Contents."));
        Msg.raw(player, " ", Msg.button("/ms buy &7[amount]", "&a/ms\n&oBuy additional storage", "/ms buy "), Msg.format(" &8-&r Buy additional storage."));
        player.sendMessage("");
    }
}
