package com.cavetale.ms.session;

import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class SessionDrainWorldContainer extends SessionWorldContainerAction {
    @Override
    public void onCancel(Player player) {
        player.sendMessage(text("Container drain cancelled", RED)
                           .hoverEvent(showText(text("/ms", RED)))
                           .clickEvent(runCommand("/ms")));
    }
}
