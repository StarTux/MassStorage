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

public final class MassStorageAdminCommand extends AbstractCommand<MassStoragePlugin> {
    protected MassStorageAdminCommand(final MassStoragePlugin plugin) {
        super(plugin, "msadm");
    }

    @Override
    protected void onEnable() {
    }
}
