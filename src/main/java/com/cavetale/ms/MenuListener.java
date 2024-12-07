package com.cavetale.ms;

import com.cavetale.core.menu.MenuItemClickEvent;
import com.cavetale.core.menu.MenuItemEvent;
import com.cavetale.ms.session.MassStorageSession;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import static com.cavetale.mytems.util.Items.tooltip;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@RequiredArgsConstructor
public final class MenuListener implements Listener {
    public static final String MENU_KEY = "massstorage:massstorage";
    public static final String MENU_PERMISSION = "massstorage.ms";
    private final MassStoragePlugin plugin;

    protected void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    private void onMenuItem(MenuItemEvent event) {
        if (!event.getPlayer().hasPermission(MENU_PERMISSION)) return;
        event.addItem(builder -> builder
                      .key(MENU_KEY)
                      .icon(tooltip(new ItemStack(Material.HOPPER_MINECART),
                                    List.of(text("Mass Storage", LIGHT_PURPLE)))));
    }

    @EventHandler
    private void onMenuItemClick(MenuItemClickEvent event) {
        if (MENU_KEY.equals(event.getEntry().getKey())) {
            if (!event.getPlayer().hasPermission(MENU_PERMISSION)) {
                return;
            }
            final MassStorageSession session = plugin.getSessions().get(event.getPlayer());
            if (session == null || !session.isEnabled()) {
                return;
            }
            session.getDialogue().openOverview(event.getPlayer());
        }
    }
}
