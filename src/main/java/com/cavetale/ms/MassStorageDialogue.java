package com.cavetale.ms;

import com.cavetale.core.font.GuiOverlay;
import com.cavetale.ms.util.Gui;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.util.Items;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import static com.cavetale.core.font.Unicode.tiny;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;
import static org.bukkit.Sound.*;
import static org.bukkit.SoundCategory.*;

@RequiredArgsConstructor
public final class MassStorageDialogue {
    private final MassStoragePlugin plugin;
    private final MassStorageSession session;

    public void openOverview(Player player, int pageIndex) {
        List<StorableSet> list = new ArrayList<>();
        Map<StorableSet, Integer> amounts = new IdentityHashMap<>();
        int totalAmount = 0;
        for (StorableCategory it : StorableCategory.values()) {
            int amount = session.count(it.getStorables());
            if (amount == 0) continue;
            totalAmount += 1;
            amounts.put(it, amount);
            list.add(it);
        }
        if (totalAmount == 0) {
            openInsert(player);
            return;
        }
        final int size = 6 * 9;
        final int pageSize = 5 * 9;
        final int pageCount = (list.size() - 1) / pageSize + 1;
        Gui gui = new Gui(plugin).size(size);
        GuiOverlay.Builder builder = GuiOverlay.BLANK.builder(size, LIGHT_PURPLE)
            .layer(GuiOverlay.TOP_BAR, DARK_PURPLE)
            .title(join(noSeparators(),
                        (pageCount > 1
                         ? text((pageIndex + 1) + "/" + pageCount + " ", BLACK)
                         : empty()),
                        text("Mass Storage Menu", YELLOW)));
        for (int i = 0; i < pageSize; i += 1) {
            final int listIndex = pageIndex * pageSize + i;
            if (listIndex >= list.size()) continue;
            StorableSet item = list.get(listIndex);
            final int guiIndex = 9 + i;
            ItemStack icon = item.getIcon();
            List<StorableItem> storables = session.filter(item.getStorables());
            icon.editMeta(meta -> {
                    meta.addItemFlags(ItemFlag.values());
                    Items.text(meta, List.of(item.getTitle(),
                                             text("Category", DARK_GRAY, ITALIC),
                                             join(noSeparators(), text(tiny("items "), GRAY), text(storables.size(), WHITE)),
                                             join(noSeparators(), text(tiny("stored "), GRAY), text(amounts.get(item), WHITE))));
                });
            gui.setItem(guiIndex, icon, click -> {
                    if (click.isLeftClick()) {
                        openItems(player, item.getTitle(), storables, 0);
                        click(player);
                    }
                });
        }
        if (pageIndex > 0) {
            gui.setItem(0, Items.text(Mytems.ARROW_LEFT.createIcon(), List.of(text("Page " + pageIndex, GRAY))), click -> {
                    if (!click.isLeftClick()) return;
                    openOverview(player, pageIndex - 1);
                    click(player);
                });
        }
        if (pageIndex < pageCount - 1) {
            gui.setItem(8, Items.text(Mytems.ARROW_RIGHT.createIcon(), List.of(text("Page " + (pageIndex + 2), GRAY))), click -> {
                    if (!click.isLeftClick()) return;
                    openOverview(player, pageIndex + 1);
                    click(player);
                });
        }
        gui.title(builder.build());
        gui.open(player);
    }

    public void openItems(Player player, Component title, List<StorableItem> list, int pageIndex) {
        Collections.sort(list, (a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getName(), b.getName()));
        final int size = 6 * 9;
        final int pageSize = 5 * 9;
        final int pageCount = (list.size() - 1) / pageSize + 1;
        Gui gui = new Gui(plugin).size(size);
        GuiOverlay.Builder builder = GuiOverlay.BLANK.builder(size, GRAY)
            .layer(GuiOverlay.TOP_BAR, DARK_GRAY)
            .title(join(noSeparators(), text((pageIndex + 1) + "/" + pageCount + " ", BLACK), title));
        for (int i = 0; i < pageSize; i += 1) {
            final int listIndex = pageIndex * pageSize + i;
            if (listIndex >= list.size()) continue;
            StorableItem storable = list.get(listIndex);
            int amount = session.getAmount(storable);
            final int guiIndex = 9 + i;
            ItemStack icon = storable.createIcon();
            icon.editMeta(meta -> {
                    meta.addItemFlags(ItemFlag.values());
                    Items.text(meta, List.of(storable.getDisplayName(),
                                             join(noSeparators(), text(tiny("stored "), GRAY), text(amount, WHITE))));

                });
            gui.setItem(guiIndex, icon, click -> {
                    if (click.isLeftClick()) {
                        click(player);
                    }
                });
        }
        if (pageIndex > 0) {
            gui.setItem(0, Items.text(Mytems.ARROW_LEFT.createIcon(), List.of(text("Page " + pageIndex, GRAY))), click -> {
                    if (!click.isLeftClick()) return;
                    openItems(player, title, list, pageIndex - 1);
                    click(player);
                });
        }
        if (pageIndex < pageCount - 1) {
            gui.setItem(8, Items.text(Mytems.ARROW_RIGHT.createIcon(), List.of(text("Page " + (pageIndex + 2), GRAY))), click -> {
                    if (!click.isLeftClick()) return;
                    openItems(player, title, list, pageIndex + 1);
                    click(player);
                });
        }
        gui.setItem(Gui.OUTSIDE, null, click -> {
                openOverview(player, 0);
                click(player);
            });
        gui.title(builder.build());
        gui.open(player);
    }

    public void openInsert(Player player) {
        final int size = 6 * 9;
        Gui gui = new Gui(plugin).size(size);
        gui.title(text("Insert items into Mass Storage", DARK_GREEN));
        gui.setEditable(true);
        gui.onClose(evt -> {
                session.insertAndSubtract(gui.getInventory(), map -> {
                        int total = 0;
                        for (int i : map.values()) total += i;
                        if (total == 0) return;
                        player.sendMessage(text("Stored " + total + " item" + (total == 1 ? "" : "s"), GREEN));
                    });
                for (ItemStack item : gui.getInventory()) {
                    if (item == null || item.getType().isAir()) continue;
                    for (ItemStack drop : player.getInventory().addItem(item).values()) {
                        player.getWorld().dropItem(player.getEyeLocation(), drop).setOwner(player.getUniqueId());
                    }
                }
            });
        gui.open(player);
    }

    private void click(Player player) {
        player.playSound(player.getLocation(), UI_BUTTON_CLICK, MASTER, 0.5f, 1.0f);
    }
}
