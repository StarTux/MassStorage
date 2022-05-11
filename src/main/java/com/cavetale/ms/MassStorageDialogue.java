package com.cavetale.ms;

import com.cavetale.core.font.GuiOverlay;
import com.cavetale.ms.util.Gui;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.item.font.Glyph;
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
    private final List<DialogueState> states = new ArrayList<>();
    private ItemSortOrder itemSortOrder = ItemSortOrder.NAME;

    public void open(Player player) {
        if (states.isEmpty()) {
            states.add(new DialogueOverview());
        }
        DialogueState state = states.get(states.size() - 1);
        if (state instanceof DialogueOverview overview) {
            openOverview(player, overview);
        } else if (state instanceof DialogueItems items) {
            openItems(player, items);
        } else if (state instanceof DialogueItem item) {
            openItem(player, item);
        }
    }

    private DialogueState popState() {
        if (states.isEmpty()) return null;
        return states.remove(states.size() - 1);
    }

    public void openOverview(Player player) {
        states.clear();
        open(player);
    }

    public void openSearchResult(Player player, String searchTerm, List<StorableItem> storables) {
        states.clear();
        states.add(new DialogueSearchResult(searchTerm, storables));
        open(player);
    }

    public void openCategory(Player player, StorableCategory category) {
        states.clear();
        states.add(new DialogueCategory(category));
        open(player);
    }

    private void openOverview(Player player, DialogueOverview state) {
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
                         ? text((state.pageIndex + 1) + "/" + pageCount + " ", BLACK)
                         : empty()),
                        text("Mass Storage Menu", YELLOW)));
        for (int i = 0; i < pageSize; i += 1) {
            final int listIndex = state.pageIndex * pageSize + i;
            if (listIndex >= list.size()) continue;
            StorableSet item = list.get(listIndex);
            final int guiIndex = 18 + i;
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
                        states.add(new DialogueCategory(item));
                        open(player);
                        click(player);
                    }
                });
        }
        gui.setItem(4, Items.text(Mytems.PLUS_BUTTON.createIcon(), List.of(text("Insert items", LIGHT_PURPLE))), click -> {
                if (!click.isLeftClick()) return;
                openInsert(player);
                click(player);
            });
        if (state.pageIndex > 0) {
            gui.setItem(0, Items.text(Mytems.ARROW_LEFT.createIcon(), List.of(text("Page " + state.pageIndex, GRAY))), click -> {
                    if (!click.isLeftClick()) return;
                    state.pageIndex -= 1;
                    open(player);
                    click(player);
                });
        }
        if (state.pageIndex < pageCount - 1) {
            gui.setItem(8, Items.text(Mytems.ARROW_RIGHT.createIcon(), List.of(text("Page " + (state.pageIndex + 2), GRAY))), click -> {
                    if (!click.isLeftClick()) return;
                    state.pageIndex += 1;
                    open(player);
                    click(player);
                });
        }
        gui.setItem(Gui.OUTSIDE, null, click -> {
                if (!click.isLeftClick()) return;
                popState();
                open(player);
                click(player);
            });
        gui.title(builder.build());
        gui.open(player);
    }

    private void openItems(Player player, DialogueItems state) {
        List<StorableItem> list = session.filter(state.getStorables());
        switch (itemSortOrder) {
        case NAME:
            Collections.sort(list, (a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getName(), b.getName()));
            break;
        case AMOUNT:
            Collections.sort(list, (a, b) -> Integer.compare(session.getAmount(b), session.getAmount(a)));
            break;
        default: break;
        }
        final int size = 6 * 9;
        final int pageSize = 5 * 9;
        final int pageCount = (list.size() - 1) / pageSize + 1;
        Gui gui = new Gui(plugin).size(size);
        GuiOverlay.Builder builder = GuiOverlay.BLANK.builder(size, GRAY)
            .layer(GuiOverlay.TOP_BAR, DARK_GRAY)
            .title(join(noSeparators(), text((state.pageIndex + 1) + "/" + pageCount + " ", BLACK), state.getTitle()));
        for (int i = 0; i < pageSize; i += 1) {
            final int listIndex = state.pageIndex * pageSize + i;
            if (listIndex >= list.size()) continue;
            StorableItem storable = list.get(listIndex);
            final int amount = session.getAmount(storable);
            final int guiIndex = 9 + i;
            ItemStack icon = amount > 0 ? storable.createIcon() : Mytems.INVISIBLE_ITEM.createIcon();
            icon.editMeta(meta -> {
                    meta.addItemFlags(ItemFlag.values());
                    List<Component> tooltip = new ArrayList<>();
                    tooltip.add(storable.getDisplayName());
                    tooltip.add(join(noSeparators(), text(tiny("stored "), GRAY), text(amount, WHITE)));
                    if (amount >= 1) {
                        tooltip.add(join(noSeparators(), text(tiny("click "), GREEN), text("Open item menu", GRAY)));
                        tooltip.add(join(noSeparators(), text(tiny("shift-left "), GREEN), text("Get 1 item", GRAY)));
                        int stackSize = storable.getMaxStackSize();
                        if (stackSize > 1 && amount >= stackSize) {
                            tooltip.add(join(noSeparators(), text(tiny("shift-right "), GREEN), text("Get " + stackSize + " items", GRAY)));
                        }
                    }
                    Items.text(meta, tooltip);
                });
            gui.setItem(guiIndex, icon, click -> {
                    switch (click.getClick()) {
                    case LEFT:
                        states.add(new DialogueItem(storable));
                        open(player);
                        click(player);
                        break;
                    case SHIFT_LEFT:
                        clickRetrieve(player, gui, storable, 1);
                        break;
                    case SHIFT_RIGHT:
                        clickRetrieve(player, gui, storable, storable.getMaxStackSize());
                        break;
                    default: break;
                    }
                });
        }
        for (ItemSortOrder it : ItemSortOrder.values()) {
            int slot = it.ordinal() + 4;
            if (it == itemSortOrder) {
                builder.highlightSlot(slot, LIGHT_PURPLE);
            }
            gui.setItem(slot, Items.text(it.createIcon(), List.of(text(it.description, GRAY))), click -> {
                    if (!click.isLeftClick()) return;
                    if (it == itemSortOrder) {
                        fail(player);
                        return;
                    }
                    itemSortOrder = it;
                    open(player);
                    click(player);
                });
        }
        if (state.pageIndex > 0) {
            gui.setItem(0, Items.text(Mytems.ARROW_LEFT.createIcon(), List.of(text("Page " + state.pageIndex, GRAY))), click -> {
                    if (!click.isLeftClick()) return;
                    state.pageIndex -= 1;
                    open(player);
                    click(player);
                });
        }
        if (state.pageIndex < pageCount - 1) {
            gui.setItem(8, Items.text(Mytems.ARROW_RIGHT.createIcon(), List.of(text("Page " + (state.pageIndex + 2), GRAY))), click -> {
                    if (!click.isLeftClick()) return;
                    state.pageIndex += 1;
                    open(player);
                    click(player);
                });
        }
        gui.setItem(Gui.OUTSIDE, null, click -> {
                if (!click.isLeftClick()) return;
                popState();
                open(player);
                click(player);
            });
        gui.title(builder.build());
        gui.open(player);
    }

    private void openItem(Player player, DialogueItem state) {
        final int size = 6 * 9;
        Gui gui = new Gui(plugin).size(size);
        GuiOverlay.Builder builder = GuiOverlay.BLANK.builder(size, DARK_GRAY)
            .title(state.storable.getDisplayName());
        gui.setItem(4, state.storable.createItemStack(1));
        final int amount = session.getAmount(state.storable);
        Component storedLine = join(noSeparators(), text(tiny("stored "), GRAY), text(amount, WHITE));
        List<Glyph> glyphs = Glyph.toGlyphs("" + amount);
        for (int i = 0; i < 8; i += 1) {
            if (i >= glyphs.size()) break;
            gui.setItem(17 - i, glyphs.get(glyphs.size() - i - 1).mytems.createIcon(List.of(storedLine)));
        }
        if (amount >= 1) {
            ItemStack icon = Mytems.ARROW_DOWN.createIcon(List.of(text("Get 1 item", GRAY), storedLine));
            gui.setItem(22, icon, click -> {
                    if (!click.isLeftClick()) return;
                    clickRetrieve(player, gui, state.storable, 1);
                });
            int stackSize = Math.min(amount, state.storable.getMaxStackSize());
            if (stackSize > 1 && amount >= stackSize) {
                icon = Mytems.ARROW_DOWN.createIcon(List.of(text("Get " + stackSize + " items", GRAY), storedLine));
                icon.setAmount(stackSize);
                gui.setItem(23, icon, click -> {
                        if (!click.isLeftClick()) return;
                        clickRetrieve(player, gui, state.storable, stackSize);
                    });
            }
        }
        gui.setItem(Gui.OUTSIDE, null, click -> {
                if (!click.isLeftClick()) return;
                popState();
                open(player);
                click(player);
            });
        gui.title(builder.build());
        gui.open(player);
    }

    private void clickRetrieve(Player player, Gui gui, StorableItem storable, int desired) {
        final int amount = Math.min(session.getAmount(storable), desired);
        if (amount == 0) return;
        gui.setLocked(true);
        session.retrieveAsync(storable, amount, success -> {
                gui.setLocked(false);
                if (!success) {
                    fail(player);
                } else {
                    int todo = amount;
                    while (todo > 0) {
                        int stackSize = Math.min(todo, storable.getMaxStackSize());
                        ItemStack item = storable.createItemStack(stackSize);
                        for (ItemStack drop : player.getInventory().addItem(item).values()) {
                            player.getWorld().dropItem(player.getEyeLocation(), drop).setOwner(player.getUniqueId());
                        }
                        todo -= stackSize;
                    }
                    open(player);
                    click(player);
                }
            });
    }

    public void openInsert(Player player) {
        final int size = 6 * 9;
        Gui gui = new Gui(plugin).size(size);
        gui.title(text("Insert items into Mass Storage", DARK_PURPLE));
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
        gui.setItem(Gui.OUTSIDE, null, click -> {
                open(player);
                click(player);
            });
        gui.open(player);
    }

    private void click(Player player) {
        player.playSound(player.getLocation(), UI_BUTTON_CLICK, MASTER, 0.5f, 1.0f);
    }

    private void fail(Player player) {
        player.playSound(player.getLocation(), UI_BUTTON_CLICK, MASTER, 0.5f, 0.5f);
    }
}
