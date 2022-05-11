package com.cavetale.ms;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandContext;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.font.Unicode;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import static net.kyori.adventure.text.Component.text;
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
            session.getDialogue().openOverview(player, 0);
            return true;
        }
        String term = String.join(" ", args);
        if (term.isEmpty()) return false;
        List<StorableItem> storables = session.storables(term);
        MassStorageDialogue dialogue = session.getDialogue();
        final int max = 26;
        String titleTerm = term.length() <= max ? term : term.substring(0, max) + "...";
        Component title = text(Unicode.tiny(titleTerm), WHITE);
        dialogue.openItems(player, title, storables, 0);
        return true;
    }

    private void list(Player player) {
        MassStorageSession session = plugin.sessions.require(player);
        List<StorableItem> storables = session.allStorables();
        MassStorageDialogue dialogue = session.getDialogue();
        Component title = text("All Items", WHITE);
        dialogue.openItems(player, title, storables, 0);
    }

    protected void insert(Player player) {
        plugin.sessions.require(player).getDialogue().openInsert(player);
        player.sendMessage(text("Insert items into Mass Storage", GREEN));
    }

    private void dump(Player player) {
        MassStorageSession session = plugin.sessions.require(player);
        List<ItemStack> items = new ArrayList<>();
        for (int i = 9; i < 36; i += 1) {
            items.add(player.getInventory().getItem(i));
        }
        session.insertAndSubtract(items, map -> {
                if (map.isEmpty()) {
                    throw new CommandWarn("No items could be stored!");
                }
                int total = 0;
                for (int i : map.values()) total += i;
                player.sendMessage(text("Stored " + total + " item" + (total == 1 ? "" : "s"), GREEN));
            });
    }
}
