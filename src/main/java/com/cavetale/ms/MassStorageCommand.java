package com.cavetale.ms;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandContext;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.font.Unicode;
import com.cavetale.ms.session.MassStorageSession;
import com.cavetale.ms.session.SessionDrainWorldContainer;
import com.cavetale.ms.storable.StorableCategory;
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
        rootNode.completers(this::complete);
        rootNode.playerCaller(this::massStorage);
        rootNode.addChild("list").alias("all").denyTabCompletion()
            .description("View all items")
            .playerCaller(this::list);
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
            .description("Toggle inventory assistant")
            .playerCaller(this::auto);
    }

    private List<String> complete(CommandContext context, CommandNode node, String arg) {
        if (arg.isEmpty() || !context.isPlayer()) {
            return node.completeChildren(context, arg);
        }
        List<String> result = new ArrayList<>();
        result.addAll(node.completeChildren(context, arg));
        MassStorageSession session = plugin.sessions.get(context.player);
        if (session != null && session.isEnabled()) {
            session.complete(result, arg);
        }
        return result;
    }

    private boolean massStorage(Player player, String[] args) {
        MassStorageSession session = plugin.sessions.require(player);
        if (args.length == 0) {
            session.getDialogue().openOverview(player);
            return true;
        }
        String term = String.join(" ", args);
        if (term.isEmpty()) return false;
        List<StorableItem> storables = session.storables(term);
        if (storables.isEmpty()) {
            throw new CommandWarn("Nothing found: " + Unicode.tiny(term.toLowerCase()));
        }
        session.getDialogue().openSearchResult(player, term, storables);
        return true;
    }

    private void list(Player player) {
        plugin.sessions.require(player).getDialogue().openCategory(player, StorableCategory.ALL);
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
        session.setAction(new SessionDrainWorldContainer());
        player.sendActionBar(text("Click a container to drain into Mass Storage", GREEN));
    }

    private void auto(Player player) {
        MassStorageSession session = plugin.sessions.require(player);
        boolean auto = !session.isAssistantEnabled();
        session.setAssistantEnabled(auto);
        if (auto) {
            player.sendMessage(join(noSeparators(), Mytems.ON.component, text("Inventory assistant enabled", GREEN)));
        } else {
            player.sendMessage(join(noSeparators(), Mytems.OFF.component, text("Inventory assistant disabled", RED)));
        }
    }
}
