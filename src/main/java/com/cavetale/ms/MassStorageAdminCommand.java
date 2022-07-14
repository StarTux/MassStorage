package com.cavetale.ms;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.ms.session.FavoriteSlot;
import com.cavetale.ms.session.MassStorageSession;
import com.cavetale.ms.storable.StorableCategory;
import com.cavetale.ms.storable.StorableItem;
import com.cavetale.ms.storable.StorageType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class MassStorageAdminCommand extends AbstractCommand<MassStoragePlugin> {
    protected MassStorageAdminCommand(final MassStoragePlugin plugin) {
        super(plugin, "msadm");
    }

    @Override
    protected void onEnable() {
        CommandNode storableNode = rootNode.addChild("storable")
            .description("Storable subcommands");
        storableNode.addChild("info").arguments("<type> <sqlName>")
            .description("Print storable infos")
            .completers(CommandArgCompleter.enumLowerList(StorageType.class),
                        CommandArgCompleter.supplyList(() -> plugin.index.allSqlNames()))
            .senderCaller(this::storableInfo);
        storableNode.addChild("serialize").denyTabCompletion()
            .description("Serialize item in hand")
            .playerCaller(this::storableSerialize);
        CommandNode playerNode = rootNode.addChild("player")
            .description("Player subcommands");
        playerNode.addChild("list").arguments("<player>")
            .completers(PlayerCache.NAME_COMPLETER)
            .description("List player items")
            .senderCaller(this::playerList);
        playerNode.addChild("give").arguments("<player> <type> <name> <amount>")
            .description("Add items to player's storage")
            .completers(PlayerCache.NAME_COMPLETER,
                        CommandArgCompleter.enumLowerList(StorageType.class),
                        CommandArgCompleter.supplyList(() -> plugin.index.allSqlNames()),
                        CommandArgCompleter.integer(i -> i > 0))
            .senderCaller(this::playerGive);
        playerNode.addChild("giveone").arguments("<player>")
            .description("Give one of each storable")
            .completers(PlayerCache.NAME_COMPLETER)
            .senderCaller(this::playerGiveOne);
        playerNode.addChild("transfer").arguments("<from> <to>")
            .description("Account transfer")
            .completers(PlayerCache.NAME_COMPLETER,
                        PlayerCache.NAME_COMPLETER)
            .senderCaller(this::playerTransfer);
        rootNode.addChild("category").arguments("<category>")
            .completers(CommandArgCompleter.enumLowerList(StorableCategory.class))
            .description("List item category")
            .senderCaller(this::category);
    }

    private boolean storableInfo(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        StorageType type = StorageType.require(args[0]);
        String sqlName = args[1];
        StorableItem storable = plugin.index.get(type, sqlName);
        if (!storable.isValid()) throw new CommandWarn("Invalid storable: " + type + ", " + sqlName);
        sender.sendMessage(text(storable.toString(), AQUA));
        return true;
    }

    private void storableSerialize(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        String serialized = item.getType().getKey() + (item.hasItemMeta() ? item.getItemMeta().getAsString() : "");
        player.sendMessage(text(serialized, AQUA)
                           .hoverEvent(showText(text(serialized, AQUA)))
                           .insertion(serialized));
    }

    private boolean playerGive(CommandSender sender, String[] args) {
        if (args.length != 4) return false;
        PlayerCache target = PlayerCache.require(args[0]);
        MassStorageSession session = plugin.sessions.get(target.uuid);
        if (session == null || !session.isEnabled()) {
            throw new CommandWarn("Session not loaded: " + target.name);
        }
        StorageType type = StorageType.require(args[1]);
        String sqlName = args[2];
        StorableItem storable = plugin.index.get(type, sqlName);
        if (!storable.isValid()) throw new CommandWarn("Invalid storable: " + type + ", " + sqlName);
        int amount = CommandArgCompleter.requireInt(args[3], i -> i > 0);
        session.insert(storable, amount);
        sender.sendMessage(join(noSeparators(),
                                text("Gave " + amount + "x", AQUA),
                                storable.getDisplayName(),
                                text(" to " + target.name, AQUA)));
        return true;
    }

    private boolean playerList(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        PlayerCache target = PlayerCache.require(args[0]);
        MassStorageSession session = MassStorageSession.createAdminOnly(target.uuid);
        List<Component> list = new ArrayList<>();
        int total = 0;
        for (StorableItem storable : plugin.getIndex().all()) {
            int amount = session.getAmount(storable);
            if (amount == 0) continue;
            total += amount;
            list.add(join(noSeparators(), text(amount), text("\u00D7", DARK_GRAY), storable.getIconName()));
        }
        sender.sendMessage(join(noSeparators(), text("Items(" + total + ") ", GRAY),
                                join(separator(space()), list)));
        return true;
    }

    private boolean playerGiveOne(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        PlayerCache target = PlayerCache.require(args[0]);
        MassStorageSession session = plugin.sessions.get(target.uuid);
        if (session == null || !session.isEnabled()) {
            throw new CommandWarn("Session not loaded: " + target.name);
        }
        int count = 0;
        for (StorableItem storable : plugin.index.all()) {
            if (session.getAmount(storable) == 0) {
                session.insert(storable, 1);
                count += 1;
            }
        }
        sender.sendMessage(text("Gave " + count + " storable" + (count == 1 ? "s" : "") + " to " + target.name, AQUA));
        return true;
    }

    private boolean playerTransfer(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        PlayerCache fromPlayer = PlayerCache.require(args[0]);
        PlayerCache toPlayer = PlayerCache.require(args[1]);
        if (fromPlayer.equals(toPlayer)) throw new CommandWarn("Players are identical");
        MassStorageSession from = MassStorageSession.createAdminOnly(fromPlayer.uuid);
        MassStorageSession to = MassStorageSession.createAdminOnly(toPlayer.uuid);
        long total = 0;
        for (StorableItem storable : plugin.index.all()) {
            int amount = from.getAmount(storable);
            if (amount != 0) {
                from.setAmount(storable, 0);
                to.insert(storable, amount);
                total += (long) amount;
            }
            FavoriteSlot slot = from.getFavoriteSlot(storable);
            if (slot != null) {
                from.setFavoriteSlot(storable, null);
                to.setFavoriteSlot(storable, slot);
            }
            if (from.getAutoPickup(storable)) {
                from.setAutoPickup(storable, false);
                to.setAutoPickup(storable, true);
            }
        }
        sender.sendMessage(text(total + " items transferred from " + fromPlayer.name + " to " + toPlayer.name, AQUA));
        return true;
    }

    private boolean category(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        StorableCategory category;
        try {
            category = StorableCategory.valueOf(args[0].toUpperCase());
        } catch (IllegalArgumentException iae) {
            throw new CommandWarn("Unknown category: " + args[0]);
        }
        HashMap<String, Component> map = new HashMap<>();
        for (StorableItem storable : category.getStorables()) {
            map.put(storable.getName(), storable.getIconName());
        }
        List<String> names = new ArrayList<>(map.keySet());
        names.sort(String.CASE_INSENSITIVE_ORDER);
        List<Component> list = new ArrayList<>(names.size() + 1);
        list.add(join(noSeparators(), category.getTitle(), text("(" + names.size() + ")")));
        for (String name : names) {
            list.add(map.get(name));
        }
        sender.sendMessage(join(separator(space()), list));
        return true;
    }
}
