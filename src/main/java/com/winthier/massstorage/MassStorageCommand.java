package com.winthier.massstorage;

import com.winthier.massstorage.sql.SQLItem;
import com.winthier.massstorage.util.Msg;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class MassStorageCommand implements CommandExecutor {
    final MassStoragePlugin plugin;

    static class Page {
        final List<List<Object>> lines = new ArrayList<>();
        static List<Page> pagesOf(List<List<Object>> lines) {
            List<Page> result = new ArrayList<>();
            int i = 0;
            Page page = new Page();
            for (List<Object> line: lines) {
                page.lines.add(line);
                i += 1;
                if (i == 9) {
                    result.add(page);
                    page = new Page();
                    i = 0;
                }
            }
            if (!page.lines.isEmpty()) result.add(page);
            return result;
        }
    }

    @RequiredArgsConstructor
    class PlayerContext {
        final UUID player;
        final List<Page> pages = new ArrayList<>();;
        void clear() {
            pages.clear();
        }
    }
    
    void showPage(Player player, int index) {
        int pageCount = getPlayerContext(player).pages.size();
        if (index < 0 || index >= pageCount) return;
        Page page = getPlayerContext(player).pages.get(index);
        Msg.info(player, "Page %d/%d", index+1, pageCount);
        for (List<Object> json: page.lines) {
            Msg.raw(player, json);
        }
        if (index+1 < pageCount) {
            Msg.raw(player,
                Msg.button(ChatColor.BLUE, "&r[&9More&r]", "Next page", "/ms page " + (index+2))
                );
        }
    }

    PlayerContext getPlayerContext(Player player) {
        PlayerContext result = contexts.get(player.getUniqueId());
        if (result == null) {
            result = new PlayerContext(player.getUniqueId());
            contexts.put(player.getUniqueId(), result);
        }
        return result;
    }

    final Map<UUID, PlayerContext> contexts = new HashMap<>();
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = sender instanceof Player ? (Player)sender : null;
        if (player == null) return false;
        String cmd = args.length > 0 ? args[0].toLowerCase() : null;
        if (cmd == null) {
            plugin.getSession(player).openInventory();
            Msg.info(player, "Free storage: &9%d&r items.", plugin.getSession(player).getFreeStorage());
            quickUsage(player);
        } else if (cmd.equals("help") || cmd.equals("?")) {
            usage(player);
        } else if (cmd.equals("info")) {
            player.sendMessage("");
            Msg.info(player, "&9&lMass Storage&r Info");
            Msg.raw(player, " ", Msg.button(ChatColor.GRAY, plugin.getConfig().getString("CommandHelp", ""), null, null));
            int storage = plugin.getSession(player).getStorage();
            int capacity = plugin.getSession(player).getCapacity();
            int buyAmount = plugin.getConfig().getInt("BuyCapacity.Amount", 3*9*64);
            String buyName = plugin.getConfig().getString("BuyCapacity.DisplayName", "Chest");
            int free = plugin.getSession(player).getFreeStorage();
            Msg.send(player, " &oUsed storage:&r &7%d&8/&7%d &8(&7%d&8x&7%s&8)", storage, capacity, capacity / buyAmount, buyName);
            Msg.send(player, " &oFree storage:&r &9%d&r items &r(&9%d&8x&9%s&r)", free, free / buyAmount, buyName);
            quickUsage(player);
            player.sendMessage("");
        } else if (cmd.equals("auto")) {
            plugin.getSession(player).storePlayerInventory(player);
        } else if (cmd.equals("id")) {
            if (args.length != 3) return true;
            Item item;
            try {
                item = new Item(Integer.parseInt(args[1]), Integer.parseInt(args[2]));
            } catch (NumberFormatException nfe) {
                return true;
            }
            plugin.getSession(player).openInventory();
            int freeStorage = plugin.getSession(player).getFreeStorage();
            int displayed = plugin.getSession(player).fillInventory(item);
            Msg.info(player, "Found &a%d&r items. Free storage: &9%d&r items.", displayed, freeStorage);
        } else if (cmd.equals("find") || cmd.equals("search") || cmd.equals("list")) {
            String searchTerm;
            StringBuilder sb = new StringBuilder("");
            String space = "";
            char sorting = 'n'; // Name
            // Parse Args
            for (int i = 1; i < args.length; ++i) {
                String arg = args[i];
                // Arg starting with dash is a flag
                if (arg.startsWith("-")) {
                    for (int j = 1; j < arg.length(); ++j) {
                        char c = arg.charAt(j);
                        switch (c) {
                        case 'a': sorting = 'a'; // Amount
                            break;
                        case 'n': sorting = 'n'; // Name
                            break;
                        default:
                            Msg.warn(player, "Invalid flag: '-%s'", c);
                            return true;
                        }
                    }
                } else {
                    sb.append(space);
                    sb.append(arg);
                    space = " ";
                }
            }
            String tmp = sb.toString();
            searchTerm = tmp.isEmpty() ? null : tmp;
            // Fetch matching items
            List<NamedItem> items = new ArrayList<>();
            for (SQLItem sqlItem: plugin.getSession(player).getSQLItems().values()) {
                if (sqlItem.getAmount() <= 0) continue;
                NamedItem item = sqlItem.getNamedItem();
                if (searchTerm != null && !item.getName().toLowerCase().contains(searchTerm)) continue;
                items.add(item);
            }
            // Sort list
            if (sorting == 'a') {
                Collections.sort(items, NamedItem.AMOUNT_COMPARATOR);
            } else if (sorting == 'n') {
                Collections.sort(items, NamedItem.NAME_COMPARATOR);
            }
            // Build JSON
            List<List<Object>> jsons = new ArrayList<>();
            for (NamedItem item: items) {
                List<Object> json = new ArrayList<>();
                Material mat = item.getItem().getMaterial();
                int amount = item.getAmount();
                int stacks = (amount - 1) / mat.getMaxStackSize() + 1;
                int doubleChests = (stacks - 1) / (6 * 9) + 1;
                json.add(Msg.button(ChatColor.WHITE,
                                    " " + item.getAmount() + "&8x&r" + item.getName(),
                                    Msg.format("&r%s (#%04d/%d)\n&8craftbukkit:%s\n&8--------------------\n&7In Storage:\n&8Items: &7%d\n&8Stacks: &7%d\n&8Double Chests: &7%d",
                                               item.getName(), item.getType(), item.getData(),
                                               mat.name().toLowerCase(),
                                               amount, stacks, doubleChests),
                                    "/ms id " + item.getType() + " " + item.getData()));
                jsons.add(json);
            }
            getPlayerContext(player).clear();
            if (jsons.isEmpty()) {
                Msg.warn(player, "Nothing found.");
            } else {
                getPlayerContext(player).pages.addAll(Page.pagesOf(jsons));
                if (searchTerm != null) {
                    Msg.info(player, "%d results for &9%s&r", jsons.size(), searchTerm);
                } else {
                    Msg.info(player, "%d item types in storage.", jsons.size(), searchTerm);
                }
                showPage(player, 0);
            }
        } else if (cmd.equals("page")) {
            if (args.length != 2) return true;
            int page;
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException nfe) {
                return true;
            }
            if (page <= 0) return false;
            showPage(player, page - 1);
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
            int itemAmount = plugin.getConfig().getInt("BuyCapacity.Amount", 3*9*64) * amount;
            double price = plugin.getConfig().getDouble("BuyCapacity.Price", 500.0) * (double)amount;
            String displayName = plugin.getConfig().getString("BuyCapacity.DisplayName", "Chest");
            String priceFormat = plugin.getVaultHandler().formatMoney(price);
            if (!plugin.getVaultHandler().hasMoney(player, price)) {
                Msg.warn(player, "You cannot afford %s.", priceFormat);
                return true;
            }
            UUID code = UUID.randomUUID();
            plugin.getSession(player).buyConfirmationCode = code;
            Msg.raw(player,
                    "",
                    Msg.pluginTag(),
                    " ",
                    Msg.button(ChatColor.WHITE,
                               "Buy &9"+amount+"&8x&9"+displayName+"&r for &9"+priceFormat+"&r? ",
                               "&aThat is "+itemAmount+" items worth of Mass Storage.", null),
                    Msg.button(ChatColor.GREEN,
                               "&r[&aConfirm&r]",
                               "&aConfirm",
                               "/ms confirm " + amount + " " + code),
                    " ",
                    Msg.button(ChatColor.RED,
                               "&r[&cCancel&r]",
                               "&aCancel",
                               "/ms cancel"));
        } else if (cmd.equals("cancel")) {
            UUID uuid = plugin.getSession(player).buyConfirmationCode;
            if (uuid != null) {
                plugin.getSession(player).buyConfirmationCode = null;
                Msg.info(player, "Purchase cancelled.");
            }
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
        } else {
            StringBuilder sb = new StringBuilder(args[0]);
            for (int i = 1; i < args.length; ++i) sb.append(" ").append(args[i]);
            String searchTerm = sb.toString().toLowerCase();
            LinkedList<Item> items = new LinkedList<>();
            for (SQLItem sqlItem: plugin.getSession(player).getSQLItems().values()) {
                Item item = sqlItem.getItem();
                String itemName = plugin.getVaultHandler().getItemName(item).toLowerCase();
                if (itemName.equals(searchTerm)) {
                    items.addFirst(item);
                } else if (itemName.contains(searchTerm)) {
                    items.addLast(item);
                }
            }
            plugin.getSession(player).openInventory();
            int freeStorage = plugin.getSession(player).getFreeStorage();
            int displayed = plugin.getSession(player).fillInventory(items.toArray(new Item[0]));
            Msg.info(player, "Found &a%d&r items. Free storage: &9%d&r items.", displayed, freeStorage);
        }
        return true;
    }

    void usage(Player player) {
        player.sendMessage("");
        Msg.info(player, "&9&lMass Storage&r Help");
        Msg.raw(player, " ", Msg.button("/ms", "&a/ms\n&oOpen Mass Storage Inventory", "/ms"), Msg.format(" &8-&r Open Mass Storage Inventory."));
        Msg.raw(player, " ", Msg.button("/ms &7[item]", "&a/ms [item]\n&oRetrieve items", "/ms "), Msg.format(" &8-&r Retrieve items."));
        Msg.raw(player, " ", Msg.button("/ms info", "&a/ms info\n&oShow some info", "/ms info"), Msg.format(" &8-&r Show some info."));
        Msg.raw(player, " ", Msg.button("/ms list &7[-n|-a]", "&a/ms list\n&oList Mass Storage contents", "/ms list"), Msg.format(" &8-&r List Mass Storage contents."));
        Msg.raw(player, " ", Msg.button("/ms search &7[item] [-n|-a]", "&a/ms search [item]\n&oFind stored items", "/ms search "), Msg.format(" &8-&r Find stored items."));
        Msg.raw(player, "  ", Msg.button(ChatColor.GRAY, "&7-n&8 = &7Sort by name&8; &7-a&8 = &7by amount", null, null));
        Msg.raw(player, " ", Msg.button("/ms auto", "&a/ms auto\n&oAutomatically store inventory", "/ms auto "), Msg.format(" &8-&r Auto store inventory."));
        String purchaseCost = plugin.getVaultHandler().formatMoney(plugin.getConfig().getDouble("BuyCapacity.Price", 500.0));
        Msg.raw(player, " ", Msg.button("/ms buy &7[amount]", "&a/ms buy [amount]\n&oBuy additional storage\nPrice: " + purchaseCost, "/ms buy "), Msg.format(" &8-&r Buy additional storage."));
        player.sendMessage("");
    }

    void quickUsage(Player player) {
        String purchaseCost = plugin.getVaultHandler().formatMoney(plugin.getConfig().getDouble("BuyCapacity.Price", 500.0));
        Msg.raw(
            player,
            Msg.format(" &oClick here:&r "),
            Msg.button(ChatColor.GREEN, "&r[&ams&r]", "&a/ms [item]\n&oOpen Mass Storage Inventory", "/ms "),
            " ",
            Msg.button(ChatColor.GREEN, "&r[&a?&r]", "&a/ms ?\n&oHelp Screen", "/ms ?"),
            " ",
            Msg.button(ChatColor.GREEN, "&r[&ainfo&r]", "&a/ms info\n&oShow some info", "/ms info"),
            " ",
            Msg.button(ChatColor.GREEN, "&r[&alist&r]", "&a/ms list [item]\n&oList Mass Storage contents", "/ms list "),
            " ",
            Msg.button(ChatColor.GREEN, "&r[&abuy&r]", "&a/ms buy [amount]\n&oBuy additional storage\nPrice: " + purchaseCost, "/ms buy "));
    }
}
