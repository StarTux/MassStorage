package com.cavetale.ms;

import com.cavetale.core.command.AbstractCommand;

public final class MassStorageInsertCommand extends AbstractCommand<MassStoragePlugin> {
    protected MassStorageInsertCommand(final MassStoragePlugin plugin) {
        super(plugin, "mss");
    }

    @Override
    protected void onEnable() {
        rootNode.denyTabCompletion()
            .description("Insert items")
            .playerCaller(plugin.command::insert);
    }
}
