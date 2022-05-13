package com.cavetale.ms;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.ms.storable.StorableItem;
import com.cavetale.ms.storable.StorageType;
import org.bukkit.command.CommandSender;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class MassStorageAdminCommand extends AbstractCommand<MassStoragePlugin> {
    protected MassStorageAdminCommand(final MassStoragePlugin plugin) {
        super(plugin, "msadm");
    }

    @Override
    protected void onEnable() {
        CommandNode storable = rootNode.addChild("storable")
            .description("Storable subcommands");
        storable.addChild("info").arguments("<type> <sqlName>")
            .description("Print storable infos")
            .completers(CommandArgCompleter.enumLowerList(StorageType.class),
                        CommandArgCompleter.supplyList(() -> plugin.index.allSqlNames()))
            .senderCaller(this::storableInfo);
    }

    private boolean storableInfo(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        StorageType type = StorageType.require(args[0]);
        String sqlName = args[1];
        StorableItem item = plugin.index.get(type, sqlName);
        if (!item.isValid()) throw new CommandWarn("Invalid item: " + type + ", " + sqlName);
        sender.sendMessage(text(item.toString(), AQUA));
        return true;
    }
}
