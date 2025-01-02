package com.cavetale.ms.dialogue;

import com.cavetale.core.event.player.PluginPlayerEvent;
import com.cavetale.core.font.GuiOverlay;
import com.cavetale.core.menu.MenuItemEvent;
import com.cavetale.ms.MassStoragePlugin;
import com.cavetale.ms.session.FavoriteSet;
import com.cavetale.ms.session.FavoriteSlot;
import com.cavetale.ms.session.MassStorageSession;
import com.cavetale.ms.session.SessionAction;
import com.cavetale.ms.session.SessionDrainWorldContainer;
import com.cavetale.ms.session.SessionFillWorldContainer;
import com.cavetale.ms.storable.StorableBukkitItem;
import com.cavetale.ms.storable.StorableCategory;
import com.cavetale.ms.storable.StorableItem;
import com.cavetale.ms.storable.StorableSet;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.item.font.Glyph;
import com.cavetale.mytems.util.Gui;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import static com.cavetale.core.font.Unicode.tiny;
import static com.cavetale.ms.session.ItemInsertionCause.*;
import static com.cavetale.mytems.util.Items.tooltip;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;
import static org.bukkit.Sound.*;
import static org.bukkit.SoundCategory.*;

@RequiredArgsConstructor
public final class MassStorageDialogue {
    private final MassStoragePlugin plugin;
    private final MassStorageSession session;
    private final List<DialogueState> states = new ArrayList<>();
    public static final Component TIMES = text("\u00D7", DARK_GRAY);
    protected static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("#,###");

    /**
     * All open actions should be funelled through this so we can do
     * the necessary action checks.
     */
    public void open(Player player) {
        // Player session
        SessionAction action = session.getAction();
        if (action != null) {
            session.setAction(null);
            action.onCancel(player);
        }
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
        PluginPlayerEvent.Name.OPEN_MASS_STORAGE.call(plugin, player);
    }

    public void openSearchResult(Player player, String searchTerm, List<StorableItem> storables) {
        states.clear();
        states.add(new DialogueSearchResult(searchTerm, storables));
        open(player);
        PluginPlayerEvent.Name.SEARCH_MASS_STORAGE.make(plugin, player)
            .detail(PluginPlayerEvent.Detail.NAME, searchTerm)
            .callEvent();
        for (StorableItem storable : storables) {
            if (storable instanceof StorableBukkitItem bukkit) {
                PluginPlayerEvent.Name.FIND_MASS_STORAGE.make(plugin, player)
                    .detail(PluginPlayerEvent.Detail.MATERIAL, bukkit.getMaterial())
                    .detail(PluginPlayerEvent.Detail.NAME, searchTerm)
                    .callEvent();
            }
        }
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
            list.add(it);
        }
        list.addAll(session.getFavorites());
        for (Iterator<StorableSet> iter = list.iterator(); iter.hasNext();) {
            StorableSet it = iter.next();
            int amount = session.count(it.getStorables());
            if (amount == 0) {
                iter.remove();
                continue;
            }
            amounts.put(it, amount);
            totalAmount += amount;
        }
        if (totalAmount == 0) {
            openInsert(player);
            return;
        }
        // Player session
        final int size = 6 * 9;
        final int pageSize = 5 * 9;
        final int pageCount = (list.size() - 1) / pageSize + 1;
        final Gui gui = new Gui(plugin)
            .size(size)
            .layer(GuiOverlay.BLANK, LIGHT_PURPLE)
            .layer(GuiOverlay.TOP_BAR, DARK_PURPLE)
            .title(textOfChildren((pageCount > 1
                                   ? text((state.pageIndex + 1) + "/" + pageCount + " ", BLACK)
                                   : empty()),
                                  text("Mass Storage Menu", YELLOW)));
        for (int i = 0; i < pageSize; i += 1) {
            final int listIndex = state.pageIndex * pageSize + i;
            if (listIndex >= list.size()) continue;
            StorableSet item = list.get(listIndex);
            final int guiIndex = 9 + i;
            ItemStack icon = item.getIcon();
            List<StorableItem> storables = session.filter(item.getStorables());
            icon.editMeta(meta -> {
                    if (!(item instanceof FavoriteSet)) {
                        meta.addItemFlags(ItemFlag.values());
                    }
                    String storablesFormat = AMOUNT_FORMAT.format(storables.size());
                    String amountFormat = AMOUNT_FORMAT.format(amounts.getOrDefault(item, 0));
                    tooltip(meta, List.of(item.getTitle(),
                                          text("Group", DARK_GRAY, ITALIC),
                                          textOfChildren(text(tiny("items "), GRAY), text(storablesFormat, WHITE)),
                                          textOfChildren(text(tiny("stored "), GRAY), text(amountFormat, WHITE)),
                                          empty(),
                                          textOfChildren(Mytems.MOUSE_LEFT, text(" Open Group Menu", GRAY))));
                });
            gui.setItem(guiIndex, icon, click -> {
                    if (click.isLeftClick()) {
                        states.add(new DialogueCategory(item));
                        open(player);
                        click(player);
                    }
                });
        }
        ItemStack insertIcon = Mytems.PLUS_BUTTON
            .createIcon(List.of(text("Insert items", LIGHT_PURPLE),
                                text("/mss", GREEN),
                                text(tiny("Opens empty chest."), GRAY),
                                text(tiny("You can also shift"), GRAY),
                                text(tiny("click any item in"), GRAY),
                                text(tiny("your inventory to"), GRAY),
                                text(tiny("try and store it."), GRAY),
                                empty(),
                                textOfChildren(Mytems.MOUSE_LEFT, text(" Open chest", GRAY))));
        gui.setItem(4, insertIcon, click -> {
                if (!click.isLeftClick()) return;
                openInsert(player);
                click(player);
            });
        ItemStack dumpIcon = tooltip(new ItemStack(Material.HOPPER_MINECART),
                                     List.of(text("Dump inventory", LIGHT_PURPLE),
                                             text("/ms dump", GREEN),
                                             text(tiny("Try to store any"), GRAY),
                                             text(tiny("item in your"), GRAY),
                                             text(tiny("inventory except"), GRAY),
                                             text(tiny("for the hotbar"), GRAY),
                                             text(tiny("and armor slots."), GRAY),
                                             empty(),
                                             textOfChildren(Mytems.MOUSE_LEFT, text(" Dump now", GRAY))));
        gui.setItem(3, dumpIcon, click -> {
                if (!click.isLeftClick()) return;
                List<ItemStack> items = new ArrayList<>();
                for (int i = 9; i < 36; i += 1) {
                    items.add(player.getInventory().getItem(i));
                }
                session.insertAndSubtract(items, DUMP, result -> {
                        result.feedback(player);
                        if (result.success()) open(player);
                    });
            });
        final boolean auto = session.isAssistEnabled();
        ItemStack autoIcon = (auto ? Mytems.ON : Mytems.OFF)
            .createIcon(List.of((auto
                                 ? text("Inventory Assist ON", GREEN)
                                 : text("Inventory Assist OFF", RED)),
                                text("/ms auto", GREEN),
                                text(tiny("The assistant will"), GRAY),
                                text(tiny("try to store items"), GRAY),
                                text(tiny("you pick up, and"), GRAY),
                                text(tiny("keep your hotbar"), GRAY),
                                text(tiny("stacked."), GRAY),
                                text(tiny("It allows storing"), GRAY),
                                text(tiny("via control-drop."), GRAY),
                                empty(),
                                textOfChildren(Mytems.MOUSE_LEFT, text(" Toggle", GRAY))));
        gui.setItem(6, autoIcon, click -> {
                if (!click.isLeftClick()) return;
                session.setAssistEnabled(!auto);
                open(player);
                click(player);
            });
        ItemStack drainIcon = Mytems.MAGNET
            .createIcon(List.of(text("Drain Container", LIGHT_PURPLE),
                                text("/ms drain", GREEN),
                                text(tiny("Menu will close."), GRAY),
                                text(tiny("Then, click a"), GRAY),
                                text(tiny("container in your"), GRAY),
                                text(tiny("world to try and"), GRAY),
                                text(tiny("remove all items"), GRAY),
                                text(tiny("from and put them"), GRAY),
                                text(tiny("into storage."), GRAY),
                                empty(),
                                textOfChildren(Mytems.MOUSE_LEFT, text(" Activate", GRAY))));
        gui.setItem(2, drainIcon, click -> {
                if (!click.isLeftClick()) return;
                // Player session
                session.setAction(new SessionDrainWorldContainer());
                player.closeInventory();
                player.sendActionBar(text("Click a container to drain into Mass Storage", GREEN));
                click(player);
            });
        if (state.pageIndex > 0) {
            gui.setItem(0, tooltip(Mytems.ARROW_LEFT.createIcon(), List.of(text("Page " + state.pageIndex, GRAY))), click -> {
                    if (!click.isLeftClick()) return;
                    state.pageIndex -= 1;
                    open(player);
                    click(player);
                });
        }
        if (state.pageIndex < pageCount - 1) {
            gui.setItem(8, tooltip(Mytems.ARROW_RIGHT.createIcon(), List.of(text("Page " + (state.pageIndex + 2), GRAY))), click -> {
                    if (!click.isLeftClick()) return;
                    state.pageIndex += 1;
                    open(player);
                    click(player);
                });
        }
        gui.setItem(Gui.OUTSIDE, null, click -> {
                if (!click.isLeftClick()) return;
                popState();
                if (states.isEmpty()) {
                    MenuItemEvent.openMenu(player);
                } else {
                    open(player);
                }
                click(player);
            });
        gui.onClick(this::clickBottom);
        gui.open(player);
    }

    private void openItems(Player player, DialogueItems state) {
        List<StorableItem> list = session.filter(state.getStorables());
        // Player session
        final ItemSortOrder itemSortOrder = session.getItemSortOrder();
        Collections.sort(list, (a, b) -> itemSortOrder.compare(session, a, b));
        final int size = 6 * 9;
        final int pageSize = 5 * 9;
        final int pageCount = (list.size() - 1) / pageSize + 1;
        final Gui gui = new Gui(plugin)
            .size(size)
            .layer(GuiOverlay.BLANK, GRAY)
            .layer(GuiOverlay.TOP_BAR, DARK_GRAY)
            .title(textOfChildren(text((state.pageIndex + 1) + "/" + pageCount + " ", BLACK), state.getTitle()));
        for (int i = 0; i < pageSize; i += 1) {
            final int listIndex = state.pageIndex * pageSize + i;
            if (listIndex >= list.size()) continue;
            StorableItem storable = list.get(listIndex);
            final int amount = session.getAmount(storable);
            final int guiIndex = 9 + i;
            ItemStack icon = amount > 0 ? storable.createIcon() : Mytems.INVISIBLE_ITEM.createIcon();
            final Denomination denomination = Denomination.ofAmount(amount);
            if (denomination.getColor() != null) {
                gui.highlight(guiIndex, denomination.getColor());
            }
            icon.editMeta(meta -> {
                    meta.addItemFlags(ItemFlag.values());
                    List<Component> tooltip = new ArrayList<>();
                    tooltip.add(storable.getIconName());
                    tooltip.add(text(storable.getCategory(), BLUE));
                    tooltip.add(textOfChildren(text(tiny("stored "), GRAY), text(AMOUNT_FORMAT.format(amount), WHITE)));
                    if (amount >= 1) {
                        tooltip.add(empty());
                        tooltip.add(textOfChildren(Mytems.MOUSE_LEFT, text(" Open item menu", GRAY)));
                        tooltip.add(textOfChildren(Mytems.MOUSE_RIGHT, text(" Get 1 item", GRAY)));
                        final int stackSize = Math.min(amount, storable.getMaxStackSize());
                        tooltip.add(textOfChildren(Mytems.SHIFT_KEY, Mytems.MOUSE_LEFT, text(" Get one stack of " + stackSize, GRAY)));
                        if (stackSize > 1 && amount > stackSize) {
                            tooltip.add(textOfChildren(Mytems.SHIFT_KEY, Mytems.MOUSE_RIGHT, text(" Fill inventory", GRAY)));
                        }
                    }
                    tooltip(meta, tooltip);
                    meta.setMaxStackSize(99);
                });
            icon.setAmount(Math.max(1, Math.min(99, amount / (denomination.getValue()))));
            gui.setItem(guiIndex, icon, click -> {
                    switch (click.getClick()) {
                    case LEFT:
                        states.add(new DialogueItem(storable));
                        open(player);
                        click(player);
                        break;
                    case RIGHT:
                        clickRetrieve(player, gui, storable, 1);
                        break;
                    case SHIFT_LEFT:
                        clickRetrieve(player, gui, storable, storable.getMaxStackSize());
                        break;
                    case SHIFT_RIGHT:
                        clickRetrieve(player, gui, storable, storable.getMaxStackSize() * 4 * 9);
                        break;
                    default: break;
                    }
                });
        }
        for (ItemSortOrder it : ItemSortOrder.values()) {
            if (it == itemSortOrder) {
                gui.highlight(it.slot, GRAY);
            }
            gui.setItem(it.slot, tooltip(it.createIcon(), List.of(text(it.description, GRAY))), click -> {
                    if (!click.isLeftClick()) return;
                    if (it == itemSortOrder) {
                        fail(player);
                        return;
                    }
                    // Player session
                    session.setItemSortOrder(it);
                    open(player);
                    click(player);
                });
        }
        if (state.pageIndex > 0) {
            gui.setItem(0, tooltip(Mytems.ARROW_LEFT.createIcon(), List.of(text("Page " + state.pageIndex, GRAY))), click -> {
                    if (!click.isLeftClick()) return;
                    state.pageIndex -= 1;
                    open(player);
                    click(player);
                });
        }
        if (state.pageIndex < pageCount - 1) {
            gui.setItem(8, tooltip(Mytems.ARROW_RIGHT.createIcon(), List.of(text("Page " + (state.pageIndex + 2), GRAY))), click -> {
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
                PluginPlayerEvent.Name.MASS_STORAGE_GO_BACK.call(plugin, player);
            });
        gui.onClick(this::clickBottom);
        gui.open(player);
    }

    private void openItem(Player player, DialogueItem state) {
        StorableItem storable = state.getStorable();
        final int size = 6 * 9;
        Gui gui = new Gui(plugin).size(size);
        GuiOverlay.Builder builder = GuiOverlay.BLANK.builder(size, DARK_GRAY)
            .layer(GuiOverlay.TOP_BAR, BLACK)
            .title(storable.getDisplayName());
        gui.setItem(8, storable.createItemStack(1));
        final int amount = session.getAmount(storable);
        Component storedLine = textOfChildren(text(tiny("stored "), GRAY), text(AMOUNT_FORMAT.format(amount), WHITE));
        List<Glyph> glyphs = Glyph.toGlyphs("" + amount);
        for (int i = 0; i < 7; i += 1) {
            if (i >= glyphs.size()) break;
            gui.setItem(7 - i, glyphs.get(glyphs.size() - i - 1).mytems.createIcon(List.of(storedLine)));
        }
        final int autoIndex = 27 + 4;
        if (session.getAutoPickup(storable)) {
            List<Component> tooltip = List.of(text("Auto Pickup enabled", GREEN),
                                              textOfChildren(Mytems.MOUSE_LEFT, text(" Toggle off", WHITE)),
                                              text(tiny("When picked up, any"), GRAY),
                                              text(tiny("item of this type"), GRAY),
                                              text(tiny("will go straight"), GRAY),
                                              text(tiny("into Mass Storage."), GRAY),
                                              empty(),
                                              text(tiny("Even if Inventory"), GRAY),
                                              text(tiny("Assist is disabled"), GRAY));
            gui.setItem(autoIndex, Mytems.ON.createIcon(tooltip), click -> {
                    if (!click.isLeftClick()) return;
                    session.setAutoPickup(storable, false);
                    open(player);
                    click(player);
                });
        } else {
            List<Component> tooltip = List.of(text("Auto Pickup disabled", RED),
                                              textOfChildren(Mytems.MOUSE_LEFT, text(" Toggle on", WHITE)),
                                              text(tiny("You will pick up"), GRAY),
                                              text(tiny("items as usual."), GRAY),
                                              empty(),
                                              text(tiny("If Inventory Assist"), GRAY),
                                              text(tiny("is enabled, it will"), GRAY),
                                              text(tiny("let you pick up one"), GRAY),
                                              text(tiny("stack of this item."), GRAY),
                                              text(tiny("anything beyond will"), GRAY),
                                              text(tiny("be stored."), GRAY));
            gui.setItem(autoIndex, Mytems.OFF.createIcon(tooltip), click -> {
                    if (!click.isLeftClick()) return;
                    session.setAutoPickup(storable, true);
                    open(player);
                    click(player);
                });
        }
        int dropLine = 1; // The line index where all the item drop buttons go.
        int getterIndex = 5;
        ItemStack oneIcon = Mytems.ARROW_DOWN
            .createIcon(List.of(textOfChildren(Mytems.MOUSE_LEFT, text(" Get 1 item", GRAY)), storedLine));
        gui.setItem(9 * dropLine + getterIndex++, oneIcon, click -> {
                if (!click.isLeftClick()) return;
                clickRetrieve(player, gui, storable, 1);
            });
        int stackSize = storable.getMaxStackSize();
        if (stackSize == 64) {
            ItemStack stackIcon = Mytems.ARROW_DOWN
                .createIcon(List.of(textOfChildren(Mytems.MOUSE_LEFT, text(" Get 32 items", GRAY)),
                                    storedLine));
            stackIcon.setAmount(32);
            gui.setItem(9 * dropLine + getterIndex++, stackIcon, click -> {
                    if (!click.isLeftClick()) return;
                    clickRetrieve(player, gui, storable, 32);
                });
        }
        if (stackSize > 1 && amount >= stackSize) {
            ItemStack stackIcon = Mytems.ARROW_DOWN
                .createIcon(List.of(textOfChildren(Mytems.MOUSE_LEFT, text(" Get " + stackSize + " items", GRAY)),
                                    storedLine));
            stackIcon.setAmount(stackSize);
            gui.setItem(9 * dropLine + getterIndex++, stackIcon, click -> {
                    if (!click.isLeftClick()) return;
                    clickRetrieve(player, gui, storable, stackSize);
                });
        }
        ItemStack fillInvIcon = Mytems.BOMB
            .createIcon(List.of(textOfChildren(Mytems.MOUSE_LEFT, text(" Fill your inventory", GRAY)),
                                storedLine));
        gui.setItem(9 * dropLine + 2, fillInvIcon, click -> {
                if (!click.isLeftClick()) return;
                clickRetrieve(player, gui, storable, stackSize * 4 * 9);
            });
        ItemStack fillIcon = tooltip(new ItemStack(Material.CHEST),
                                     List.of(textOfChildren(Mytems.MOUSE_LEFT, text(" Fill container", GRAY)),
                                             storedLine,
                                             text(tiny("Menu will close."), GRAY),
                                             text(tiny("Then, click a"), GRAY),
                                             text(tiny("container in your"), GRAY),
                                             text(tiny("world to fill it"), GRAY),
                                             text(tiny("with this item."), GRAY)));
        gui.setItem(9 * dropLine + 3, fillIcon, click -> {
                if (!click.isLeftClick()) return;
                if (session.getAmount(storable) == 0) {
                    player.sendMessage(textOfChildren(text("You are out of ", RED), storable.getIconName()));
                    fail(player);
                    return;
                }
                // Player session
                session.setAction(new SessionFillWorldContainer(storable));
                player.closeInventory();
                click(player);
                player.sendActionBar(textOfChildren(text("Click a container to fill with ", GREEN),
                                                    storable.getIconName()));
            });
        for (FavoriteSlot fav : FavoriteSlot.values()) {
            // Player session
            boolean isFav = session.getFavoriteSlot(storable) == fav;
            if (isFav) {
                builder.highlightSlot(fav.guiSlot, fav.blockColor.textColor);
            }
            ItemStack icon = isFav
                ? session.getFavorite(fav).getIcon()
                : fav.createDisabledIcon();
            List<Component> tooltip = List.of(textOfChildren((isFav ? text("Remove from ", RED) : text("Put in ", GREEN)),
                                                             fav.getDisplayName()));
            gui.setItem(fav.guiSlot, tooltip(icon, tooltip), click -> {
                    if (!click.isLeftClick()) return;
                    session.setFavoriteSlot(storable, isFav ? null : fav);
                    open(player);
                    click(player);
                });
        }
        gui.setItem(Gui.OUTSIDE, null, click -> {
                if (!click.isLeftClick()) return;
                popState();
                open(player);
                click(player);
                PluginPlayerEvent.Name.MASS_STORAGE_GO_BACK.call(plugin, player);
            });
        gui.onClick(this::clickBottom);
        gui.title(builder.build());
        gui.open(player);
    }

    private void clickRetrieve(Player player, Gui gui, StorableItem storable, int desired) {
        final int has = Math.min(session.getAmount(storable), desired);
        if (has == 0) {
            player.sendMessage(textOfChildren(text("You are out of ", RED), storable.getIconName()));
            return;
        }
        final int amount = storable.fit(player.getInventory(), has, false);
        if (amount == 0) {
            player.sendMessage(text("Your inventory is full!", RED));
            fail(player);
            return;
        }
        gui.setLocked(true);
        session.retrieveAsync(storable, amount, success -> {
                gui.setLocked(false);
                if (!success) {
                    player.sendMessage(textOfChildren(text("You are out of ", RED), storable.getIconName()));
                    fail(player);
                    return;
                }
                final int given;
                if (!player.isOnline() || player.isDead()) {
                    given = 0;
                } else {
                    given = storable.fit(player.getInventory(), amount, true);
                }
                if (given < amount) {
                    session.insertAsync(storable, amount - given, null);
                }
                player.sendMessage(textOfChildren(text("Retrieved ", GREEN), text(given, WHITE), TIMES, storable.getIconName()));
                open(player);
                click(player);
            });
    }

    public void openInsert(Player player) {
        final int size = 6 * 9;
        Gui gui = new Gui(plugin).size(size);
        gui.title(text("Insert items into Mass Storage", DARK_PURPLE));
        gui.setEditable(true);
        gui.onClose(evt -> {
                session.insertAndSubtract(gui.getInventory(), INSERT_MENU, result -> result.feedback(player));
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
                PluginPlayerEvent.Name.MASS_STORAGE_GO_BACK.call(plugin, player);
            });
        gui.open(player);
    }

    private static void click(Player player) {
        player.playSound(player.getLocation(), UI_BUTTON_CLICK, MASTER, 0.5f, 1.0f);
    }

    private static void fail(Player player) {
        player.playSound(player.getLocation(), UI_BUTTON_CLICK, MASTER, 0.5f, 0.5f);
    }

    private void clickBottom(InventoryClickEvent event) {
        if (!event.getView().getBottomInventory().equals(event.getClickedInventory())) {
            return;
        }
        if (event.getClick() == ClickType.DROP) {
            event.setCancelled(false);
        }
        if (event.getClick() == ClickType.CONTROL_DROP) {
            event.setCancelled(false);
        }
        if (event.getClick() == ClickType.SHIFT_LEFT) {
            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType().isAir()) return;
            Player player = (Player) event.getWhoClicked();
            session.insertAndSubtract(List.of(item), SHIFT_CLICK_MENU, result -> {
                    result.feedback(player);
                    if (result.success()) open(player);
                });
        }
    }
}
