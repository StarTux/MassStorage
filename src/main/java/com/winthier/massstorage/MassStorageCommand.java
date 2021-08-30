package com.winthier.massstorage;

import com.cavetale.core.event.player.PluginPlayerEvent.Detail;
import com.cavetale.core.event.player.PluginPlayerEvent;
import com.winthier.massstorage.util.Msg;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public final class MassStorageCommand implements TabExecutor {
    final MassStoragePlugin plugin;
    final Map<UUID, PlayerContext> contexts = new HashMap<>();

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
        Msg.info(player, "Page %d/%d", index + 1, pageCount);
        for (List<Object> json: page.lines) {
            Msg.raw(player, json);
        }
        if (index + 1 < pageCount) {
            Msg.raw(player,
                Msg.button(ChatColor.BLUE, "&r[&9More&r]", "Next page", "/ms page " + (index + 2))
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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = sender instanceof Player ? (Player) sender : null;
        if (player == null) return false;
        String cmd = args.length > 0 ? args[0].toLowerCase() : null;
        if (cmd == null) {
            Session session = plugin.getSession(player);
            session.setOpenCategory(-1);
            new MenuInventory(plugin, player).open();
            if (!session.isInformed()) {
                session.setInformed(true);
                menuUsage(player);
            }
            quickUsage(player);
        } else if (cmd.equals("store") && args.length == 1) {
            plugin.getSession(player).openInventory();
        } else if ((cmd.equals("help") || cmd.equals("?")) && args.length >= 1 && args.length <= 2) {
            if (args.length == 1) {
                usage(player);
            } else if (args.length == 2 && "menu".equals(args[1])) {
                menuUsage(player);
            } else {
                return false;
            }
        } else if (cmd.equals("info") && args.length == 1) {
            player.sendMessage("");
            Msg.info(player, "&9&lMass Storage&r Info");
            Msg.raw(player, " ", Msg.button(ChatColor.GRAY, plugin.getConfig().getString("CommandHelp", ""), null, null));
            int storage = plugin.getSession(player).getStorage();
            Msg.send(player, " &7&oTotal storage:&f %d items", storage);
            quickUsage(player);
            player.sendMessage("");
        } else if (cmd.equals("dump") && args.length == 1) {
            long now = System.currentTimeMillis();
            Session session = plugin.getSession(player);
            if (session.getLastAutoStorage() + 1000L > now) return true;
            session.setLastAutoStorage(now);
            StorageResult result = session.storePlayerInventory(player);
            result.setShouldReportEmpty(true);
            session.reportStorageResult(player, result);
            player.playSound(player.getEyeLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.MASTER, 0.2f, 1.25f);
        } else if (cmd.equals("auto") && args.length == 1) {
            Session session = plugin.getSession(player);
            boolean newVal = !session.isAutoStorageEnabled();
            session.setAutoStorageEnabled(newVal);
            if (newVal) {
                Msg.info(player, "Auto Storage Enabled");
                long now = System.currentTimeMillis();
                if (session.getLastAutoStorage() + 1000L < now) {
                    session.setLastAutoStorage(now);
                    StorageResult result = session.storePlayerInventory(player);
                    session.reportStorageResult(player, result);
                }
                player.playSound(player.getEyeLocation(), Sound.BLOCK_LEVER_CLICK, SoundCategory.MASTER, 0.2f, 1.5f);
            } else {
                Msg.info(player, "Auto Storage Disabled");
                player.playSound(player.getEyeLocation(), Sound.BLOCK_LEVER_CLICK, SoundCategory.MASTER, 0.2f, 0.5f);
            }
        } else if (cmd.equals("id")) {
            if (args.length != 2) return true;
            Material mat;
            try {
                mat = Material.valueOf(args[1].toUpperCase());
            } catch (IllegalArgumentException iae) {
                return true;
            }
            plugin.getSession(player).openInventory();
            int displayed = plugin.getSession(player).fillInventory(mat);
            Msg.info(player, "Found &a%d&r items.", displayed);
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
                NamedItem item = plugin.getNamedItem(sqlItem);
                if (searchTerm != null && !item.matches(searchTerm)) continue;
                items.add(item);
            }
            if (items.isEmpty()) {
                Msg.warn(player, "Nothing found.");
            } else {
                if (searchTerm != null) {
                    Msg.info(player, "%d results for &9%s&r", items.size(), searchTerm);
                } else {
                    Msg.info(player, "%d item types in storage.", items.size());
                }
            }
            // Sort list
            if (sorting == 'a') {
                Collections.sort(items, NamedItem.AMOUNT_COMPARATOR);
            } else if (sorting == 'n') {
                Collections.sort(items, NamedItem.NAME_COMPARATOR);
            }
            sendItemList(player, items);
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
        } else {
            String searchTerm = String.join(" ", args);
            LinkedList<Material> mats = new LinkedList<>();
            for (SQLItem sqlItem: plugin.getSession(player).getSQLItems().values()) {
                Material mat = sqlItem.getMat();
                NamedItem namedItem = plugin.getNamedItem(sqlItem);
                if (namedItem.equalsName(searchTerm)) {
                    mats.addFirst(mat);
                } else if (namedItem.matches(searchTerm)) {
                    mats.addLast(mat);
                }
            }
            plugin.getSession(player).openInventory();
            int displayed = plugin.getSession(player).fillInventory(mats.toArray(new Material[0]));
            Msg.info(player, "Found &a%d&r items.", displayed);
            PluginPlayerEvent.Name.SEARCH_MASS_STORAGE.ultimate(plugin, player)
                .detail(Detail.NAME, searchTerm)
                .call();
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        String term = args.length > 0 ? args[args.length - 1].toLowerCase() : "";
        if (args.length <= 1) {
            return Arrays.asList("store", "help", "?", "info", "dump", "auto", "find", "search", "list", "page")
                .stream().filter(s -> s.startsWith(term)).collect(Collectors.toList());
        } else {
            return null;
        }
    }

    void usage(Player player) {
        player.sendMessage("");
        Msg.info(player, "&9&lMass Storage&r Help");
        Msg.raw(player, Msg.button("/ms", "&a/ms\n&r&oOpen Mass Storage Menu", "/ms"),
                Msg.format(" &8-&r Open Mass Storage Menu."));
        Msg.raw(player, Msg.button("/ms help menu", "&a/ms help menu\n&r&oMass Storage\n&oMenu Help", "/ms help menu"),
                Msg.format(" &8-&r Menu Help."));
        Msg.raw(player, Msg.button("/ms &7[item]", "&a/ms [item]\n&r&oRetrieve items", "/ms "),
                Msg.format(" &8-&r Retrieve items."));
        Msg.raw(player, Msg.button("/ms info", "&a/ms info\n&r&oShow some info", "/ms info"),
                Msg.format(" &8-&r Show some info."));
        Msg.raw(player, Msg.button("/ms list &7[-n|-a]", "&a/ms list\n&r&oList Mass Storage contents", "/ms list"),
                Msg.format(" &8-&r List Mass Storage contents."));
        Msg.raw(player, Msg.button("/ms search &7[item] [-n|-a]", "&a/ms search [item]\n&r&oFind stored items", "/ms search "),
                Msg.format(" &8-&r Find stored items."));
        Msg.raw(player, " ", Msg.button(ChatColor.GRAY, "&7-n&8 = &7Sort by name&8; &7-a&8 = &7by amount", null, null));
        Msg.raw(player, Msg.button("/ms dump", "&a/ms dump\n&r&oDump inventory into Mass Storage", "/ms dump "),
                Msg.format(" &8-&r Dump inventory."));
        Msg.raw(player, Msg.button("/ms auto", "&a/ms auto\n&r&oToggle automatic storage", "/ms auto "),
                Msg.format(" &8-&r Toggle auto storage."));
    }

    void quickUsage(Player player) {
        ChatColor c = ChatColor.GOLD;
        Msg.raw(
            player,
            Msg.format(" &oClick here:&r "),
            Msg.button(c, "[MS]", "&a/ms [item]\n&r&oOpen Mass Storage Inventory", "/ms "),
            " ",
            Msg.button(c, "[?]", "&c/ms ?\n&r&oHelp Screen", "/ms ?"),
            " ",
            Msg.button(c, "[Info]", "&e/ms info\n&r&oShow some info", "/ms info"),
            " ",
            Msg.button(c, "[List]", "&9/ms list [item]\n&r&oList Mass Storage contents", "/ms list "),
            " ",
            Msg.button(c, "[Dump]", "&3/ms dump\n&r&oDump your inventory\ninto Mass Storage", "/ms dump"),
            " ",
            Msg.button(c, "[Auto]", "&a/ms auto\n&r&oToggle auto storage.\n&oYour inventory will\n&obe dumped whenever\n&oit gets close to full.", "/ms auto"));
    }

    void menuUsage(Player player) {
        Msg.info(player, "Mass Storage Menu");
        Msg.raw(player, " ", Msg.button(ChatColor.GRAY, "Left-click&8=&7Open item chest", null, null));
        Msg.raw(player, " ", Msg.button(ChatColor.GRAY, "Right-click&8=&7Info", null, null));
        Msg.raw(player, " ", Msg.button(ChatColor.GRAY, "Shift-click&8=&7Drop stack", null, null));
        Msg.raw(player, " ", Msg.button(ChatColor.GRAY, "Shift-right-click&8=&7Drop chest", null, null));
        Msg.raw(player, " ", Msg.button(ChatColor.GRAY, "Click outside of chest&8=&7Go back", null, null));
    }

    void sendItemList(Player player, List<NamedItem> items) {
        List<List<Object>> jsons = new ArrayList<>();
        for (NamedItem item: items) {
            List<Object> json = new ArrayList<>();
            Material mat = item.getMat();
            int amount = item.getAmount();
            int stacks = (amount - 1) / mat.getMaxStackSize() + 1;
            int doubleChests = (stacks - 1) / (6 * 9) + 1;
            json.add(Msg.button(ChatColor.WHITE,
                                " " + item.getAmount() + "&8x&r" + item.i18nName,
                                Msg.format("&r%s\n&8minecraft:%s\n&8--------------------\n&7In Storage:\n&8Items: &7%d\n&8Stacks: &7%d\n&8Double Chests: &7%d",
                                           item.getName(),
                                           mat.name().toLowerCase(),
                                           amount, stacks, doubleChests),
                                "/ms id " + item.mat));
            jsons.add(json);
        }
        getPlayerContext(player).clear();
        getPlayerContext(player).pages.addAll(Page.pagesOf(jsons));
        showPage(player, 0);
    }
}
