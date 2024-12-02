package com.cavetale.ms;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandContext;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.font.Unicode;
import com.cavetale.ms.session.MassStorageSession;
import com.cavetale.ms.session.SessionDrainWorldContainer;
import com.cavetale.ms.storable.StorableItem;
import com.cavetale.mytems.Mytems;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import static com.cavetale.ms.session.ItemInsertionCause.*;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class MassStorageCommand extends AbstractCommand<MassStoragePlugin> {
    protected MassStorageCommand(final MassStoragePlugin plugin) {
        super(plugin, "ms");
    }

    @Override
    protected void onEnable() {
        rootNode.arguments("[item]")
            .description("Search for items")
            .playerCaller(this::massStorage);
        rootNode.addChild("search").arguments("<item/category/tag>")
            .description("Search for items, tags or item categories")
            .completers(this::completeSearch)
            .playerCaller(this::search);
        rootNode.addChild("insert").denyTabCompletion()
            .description("Insert items")
            .playerCaller(this::insert);
        rootNode.addChild("dump").denyTabCompletion()
            .description("Dump your inventory")
            .playerCaller(this::dump);
        rootNode.addChild("drain").denyTabCompletion()
            .description("Drain a container")
            .playerCaller(this::drain);
        rootNode.addChild("auto").denyTabCompletion()
            .description("Toggle Inventory Assist")
            .playerCaller(this::auto);
        rootNode.addChild("open").denyTabCompletion()
            .description("Open previous menu location")
            .playerCaller(this::open);
        rootNode.addChild("help").denyTabCompletion()
            .description("Print help")
            .playerCaller(this::help);
    }

    private boolean massStorage(Player player, String[] args) {
        if (args.length == 0) {
            MassStorageSession session = plugin.sessions.require(player);
            session.getDialogue().openOverview(player);
            return true;
        }
        return search(player, args);
    }

    private boolean search(Player player, String[] args) {
        MassStorageSession session = plugin.sessions.require(player);
        if (args.length == 0) return false;
        String term = String.join(" ", args);
        if (term.isEmpty()) return false;
        List<StorableItem> storables = session.storables(term);
        if (storables.isEmpty()) {
            throw new CommandWarn("Nothing found: " + Unicode.tiny(term.toLowerCase()));
        }
        session.getDialogue().openSearchResult(player, term, storables);
        return true;
    }

    private List<String> completeSearch(CommandContext context, CommandNode node, String arg) {
        List<String> result = new ArrayList<>();
        MassStorageSession session = plugin.sessions.get(context.player);
        if (session != null && session.isEnabled()) {
            session.complete(result, arg);
        }
        return result;
    }

    protected void insert(Player player) {
        plugin.sessions.require(player).getDialogue().openInsert(player);
    }

    private void dump(Player player) {
        if (player.getOpenInventory().getType() != InventoryType.CRAFTING) {
            player.sendMessage(text("Not now!", RED));
            return;
        }
        MassStorageSession session = plugin.sessions.require(player);
        List<ItemStack> items = new ArrayList<>();
        for (int i = 9; i < 36; i += 1) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || item.getType().isAir()) continue;
            items.add(item);
        }
        session.insertAndSubtract(items, DUMP, result -> result.feedback(player));
    }

    private void drain(Player player) {
        MassStorageSession session = plugin.sessions.require(player);
        if (session.getAction() instanceof SessionDrainWorldContainer drain) {
            session.setAction(null);
            drain.onCancel(player);
        } else {
            session.setAction(new SessionDrainWorldContainer());
            player.sendActionBar(text("Click a container to drain into Mass Storage", GREEN));
        }
    }

    private void auto(Player player) {
        MassStorageSession session = plugin.sessions.require(player);
        boolean auto = !session.isAssistEnabled();
        session.setAssistEnabled(auto);
        if (auto) {
            player.sendMessage(join(noSeparators(), Mytems.ON.component, text("Inventory Assist enabled", GREEN)));
        } else {
            player.sendMessage(join(noSeparators(), Mytems.OFF.component, text("Inventory Assist disabled", RED)));
        }
    }

    private void open(Player player) {
        plugin.sessions.require(player).getDialogue().open(player);
    }

    private boolean help(Player player) {
        return rootNode.sendHelp(player);
    }
}
