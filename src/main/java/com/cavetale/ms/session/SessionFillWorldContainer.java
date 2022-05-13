package com.cavetale.ms.session;

import com.cavetale.ms.storable.StorableItem;
import lombok.Getter;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@Getter
public final class SessionFillWorldContainer extends SessionWorldContainerAction {
    protected final StorableItem storable;

    public SessionFillWorldContainer(final StorableItem storable) {
        this.storable = storable;
    }

    @Override
    public void onCancel(Player player) {
        player.sendMessage(text("Container fill cancelled", RED)
                           .hoverEvent(showText(text("/ms", RED)))
                           .clickEvent(runCommand("/ms")));
    }
}
