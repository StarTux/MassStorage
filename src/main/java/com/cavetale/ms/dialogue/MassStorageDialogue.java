package com.cavetale.ms.dialogue;

import com.cavetale.core.font.GuiOverlay;
import com.cavetale.ms.MassStoragePlugin;
import com.cavetale.ms.session.MassStorageSession;
import com.cavetale.ms.session.SessionAction;
import com.cavetale.ms.session.SessionDrainWorldContainer;
import com.cavetale.ms.session.SessionFillWorldContainer;
import com.cavetale.ms.storable.StorableCategory;
import com.cavetale.ms.storable.StorableItem;
import com.cavetale.ms.storable.StorableSet;
import com.cavetale.ms.util.Gui;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.item.font.Glyph;
import com.cavetale.mytems.util.Items;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import static com.cavetale.core.font.Unicode.tiny;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
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
                        states.add(new DialogueCategory(item));
                        open(player);
                        click(player);
                    }
                });
        }
        gui.setItem(2, Mytems.LETTER_I
                    .createIcon(List.of(text(tiny("Hint: Click outside"), GRAY),
                                        text(tiny("the window to go"), GRAY),
                                        text(tiny("back to the"), GRAY),
                                        text(tiny("previous menu."), GRAY))), null);
        ItemStack insertIcon = Mytems.PLUS_BUTTON
            .createIcon(List.of(text("Insert items", LIGHT_PURPLE),
                                text("/mss", GREEN),
                                text(tiny("Opens empty chest."), GRAY),
                                text(tiny("You can also shift"), GRAY),
                                text(tiny("click any item in"), GRAY),
                                text(tiny("your inventory to"), GRAY),
                                text(tiny("try and store it."), GRAY)));
        gui.setItem(4, insertIcon, click -> {
                if (!click.isLeftClick()) return;
                openInsert(player);
                click(player);
            });
        ItemStack dumpIcon = Items
            .text(new ItemStack(Material.HOPPER_MINECART),
                  List.of(text("Dump inventory", LIGHT_PURPLE),
                          text("/ms dump", GREEN),
                          text(tiny("Try to store any"), GRAY),
                          text(tiny("item in your"), GRAY),
                          text(tiny("inventory except"), GRAY),
                          text(tiny("for the hotbar"), GRAY),
                          text(tiny("and armor slots."), GRAY)));
        gui.setItem(3, dumpIcon, click -> {
                if (!click.isLeftClick()) return;
                List<ItemStack> items = new ArrayList<>();
                for (int i = 9; i < 36; i += 1) {
                    items.add(player.getInventory().getItem(i));
                }
                session.insertAndSubtract(items, (rejects, map) -> {
                        insertResponse(player, rejects, map);
                        if (!map.isEmpty()) open(player);
                    });
            });
        final boolean auto = session.isAssistantEnabled();
        ItemStack autoIcon = (auto ? Mytems.ON : Mytems.OFF)
            .createIcon(List.of((auto
                                 ? text("Assistant ON", GREEN)
                                 : text("Assistant OFF", RED)),
                                text("/ms auto", GREEN),
                                text(tiny("The assistant will"), GRAY),
                                text(tiny("try to store items"), GRAY),
                                text(tiny("you pick up, and"), GRAY),
                                text(tiny("keep your hotbar"), GRAY),
                                text(tiny("stacked."), GRAY),
                                text(tiny("It allows storing"), GRAY),
                                text(tiny("via control-drop."), GRAY)));
        gui.setItem(6, autoIcon, click -> {
                if (!click.isLeftClick()) return;
                session.setAssistantEnabled(!auto);
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
                                text(tiny("into storage."), GRAY)));
        gui.setItem(5, drainIcon, click -> {
                if (!click.isLeftClick()) return;
                // Player session
                session.setAction(new SessionDrainWorldContainer());
                player.closeInventory();
                player.sendActionBar(text("Click a container to drain into Mass Storage", GREEN));
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
        gui.onClickBottom(this::clickBottom);
        gui.title(builder.build());
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
            icon.setAmount(Math.max(1, Math.min(64, (amount > 64 ? amount / 64 : amount))));
            icon.editMeta(meta -> {
                    meta.addItemFlags(ItemFlag.values());
                    List<Component> tooltip = new ArrayList<>();
                    tooltip.add(storable.getDisplayName());
                    tooltip.add(text(storable.getCategory(), BLUE));
                    tooltip.add(join(noSeparators(), text(tiny("stored "), GRAY), text(amount, WHITE)));
                    if (amount >= 1) {
                        tooltip.add(join(noSeparators(), text(tiny("click "), GREEN), text("Open item menu", GRAY)));
                        final int stackSize = Math.min(amount, storable.getMaxStackSize());
                        tooltip.add(join(noSeparators(),
                                         text(tiny("shift-left "), GREEN),
                                         text("Get " + stackSize + " item" + (stackSize == 1 ? "" : "s"), GRAY)));
                        if (stackSize > 1 && amount > stackSize) {
                            tooltip.add(join(noSeparators(), text(tiny("shift-right "), GREEN), text("Fill inventory", GRAY)));
                        }
                    }
                    Items.text(meta, tooltip);
                });
            if (amount > 64) builder.highlightSlot(guiIndex, GRAY);
            gui.setItem(guiIndex, icon, click -> {
                    switch (click.getClick()) {
                    case LEFT:
                        states.add(new DialogueItem(storable));
                        open(player);
                        click(player);
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
                builder.highlightSlot(it.slot, GRAY);
            }
            gui.setItem(it.slot, Items.text(it.createIcon(), List.of(text(it.description, GRAY))), click -> {
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
        gui.onClickBottom(this::clickBottom);
        gui.title(builder.build());
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
        Component storedLine = join(noSeparators(), text(tiny("stored "), GRAY), text(amount, WHITE));
        List<Glyph> glyphs = Glyph.toGlyphs("" + amount);
        for (int i = 0; i < 7; i += 1) {
            if (i >= glyphs.size()) break;
            gui.setItem(7 - i, glyphs.get(glyphs.size() - i - 1).mytems.createIcon(List.of(storedLine)));
        }
        int dropLine = 2;
        ItemStack oneIcon = Mytems.ARROW_DOWN
            .createIcon(List.of(text("Get 1 item", GRAY),
                                storedLine));
        gui.setItem(9 * dropLine + 5, oneIcon, click -> {
                if (!click.isLeftClick()) return;
                clickRetrieve(player, gui, storable, 1);
            });
        int stackSize = storable.getMaxStackSize();
        if (stackSize > 1 && amount >= stackSize) {
            ItemStack stackIcon = Mytems.ARROW_DOWN
                .createIcon(List.of(text("Get " + stackSize + " items", GRAY),
                                    storedLine));
            stackIcon.setAmount(stackSize);
            gui.setItem(9 * dropLine + 6, stackIcon, click -> {
                    if (!click.isLeftClick()) return;
                    clickRetrieve(player, gui, storable, stackSize);
                });
        }
        ItemStack fillInvIcon = Mytems.BOMB
            .createIcon(List.of(text("Fill your inventory", GRAY),
                                storedLine));
        gui.setItem(9 * dropLine + 2, fillInvIcon, click -> {
                if (!click.isLeftClick()) return;
                clickRetrieve(player, gui, storable, stackSize * 4 * 9);
            });
        ItemStack fillIcon = Items.text(new ItemStack(Material.CHEST),
                                        List.of(text("Fill container", GRAY),
                                                storedLine,
                                                text(tiny("Menu will close."), GRAY),
                                                text(tiny("Then, click a"), GRAY),
                                                text(tiny("container in your"), GRAY),
                                                text(tiny("world to fill it"), GRAY),
                                                text(tiny("with this item."), GRAY)));
        gui.setItem(9 * dropLine + 3, fillIcon, click -> {
                if (!click.isLeftClick()) return;
                if (session.getAmount(storable) == 0) {
                    player.sendMessage(join(noSeparators(), text("You are out of ", RED), storable.getDisplayName()));
                    fail(player);
                    return;
                }
                // Player session
                session.setAction(new SessionFillWorldContainer(storable));
                player.closeInventory();
                click(player);
                player.sendActionBar(join(noSeparators(),
                                          text("Click a container to fill with ", GREEN),
                                          storable.getDisplayName()));
            });
        gui.setItem(Gui.OUTSIDE, null, click -> {
                if (!click.isLeftClick()) return;
                popState();
                open(player);
                click(player);
            });
        gui.onClickBottom(this::clickBottom);
        gui.title(builder.build());
        gui.open(player);
    }

    private void clickRetrieve(Player player, Gui gui, StorableItem storable, int desired) {
        final int has = Math.min(session.getAmount(storable), desired);
        if (has == 0) {
            player.sendMessage(join(noSeparators(), text("You are out of ", RED), storable.getDisplayName()));
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
                    player.sendMessage(join(noSeparators(), text("You are out of ", RED), storable.getDisplayName()));
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
                player.sendMessage(join(noSeparators(), text("Retrieved ", GREEN), text(given, WHITE), TIMES, storable.getDisplayName()));
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
                session.insertAndSubtract(gui.getInventory(), (rejects, map) -> {
                        if (!rejects.isEmpty() || !map.isEmpty()) insertResponse(player, rejects, map);
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

    private static void click(Player player) {
        player.playSound(player.getLocation(), UI_BUTTON_CLICK, MASTER, 0.5f, 1.0f);
    }

    private static void fail(Player player) {
        player.playSound(player.getLocation(), UI_BUTTON_CLICK, MASTER, 0.5f, 0.5f);
    }

    public static void pickup(Player player, int amount) {
        int sounds;
        if (amount == 1) {
            sounds = 1;
        } else if (amount == 2) {
            sounds = 2;
        } else if (amount <= 4) {
            sounds = 3;
        } else if (amount <= 8) {
            sounds = 4;
        } else if (amount <= 16) {
            sounds = 5;
        } else if (amount <= 32) {
            sounds = 6;
        } else if (amount <= 64) {
            sounds = 7;
        } else {
            sounds = 8;
        }
        for (int i = 0; i < sounds; i += 1) {
            Bukkit.getScheduler().runTaskLater(MassStoragePlugin.getInstance(), () -> {
                    player.playSound(player.getLocation(), ENTITY_ITEM_PICKUP, 0.5f, 2.0f);
                }, (long) i);
        }
    }

    private void clickBottom(InventoryClickEvent event) {
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
            session.insertAndSubtract(List.of(item), (rejects, map) -> {
                    insertResponse(player, rejects, map);
                    if (!map.isEmpty()) open(player);
                });
        }
    }

    public static void insertResponse(Player player, List<ItemStack> items, Map<StorableItem, Integer> map) {
        int totalStored = 0;
        int totalRejected = 0;
        Map<String, Integer> rejectedAmounts = new HashMap<>();
        Map<String, Component> rejectedDisplayNames = new HashMap<>();
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) continue;
            totalRejected += item.getAmount();
            String name;
            Component displayName;
            Mytems mytems = Mytems.forItem(item);
            if (mytems != null) {
                name = mytems.id;
                displayName = mytems.getMytem().getDisplayName();
            } else {
                name = item.getI18NDisplayName();
                displayName = text(name, item.getType().getItemRarity().getColor());
            }
            int amount = rejectedAmounts.getOrDefault(name, 0);
            rejectedAmounts.put(name, amount + item.getAmount());
            rejectedDisplayNames.put(name, displayName);
        }
        for (Map.Entry<StorableItem, Integer> entry : map.entrySet()) {
            totalStored += entry.getValue();
        }
        Component message;
        if (totalStored == 0) {
            if (items.size() == 1) {
                message = text("Item could not be stored!", RED);
            } else {
                message = text("No items could be stored!", RED);
            }
            fail(player);
        } else {
            message = text("Stored " + totalStored + " item" + (totalStored == 1 ? "" : "s"), GREEN);
            pickup(player, totalStored);
        }
        List<Component> tooltip = new ArrayList<>();
        if (!map.isEmpty()) {
            tooltip.add(text(tiny("Stored " + totalStored + " item" + (totalStored == 1 ? "" : "s")), GREEN));
            List<StorableItem> list = new ArrayList<>(map.keySet());
            Collections.sort(list, (a, b) -> Integer.compare(map.get(b), map.get(a)));
            for (StorableItem storable : list) {
                int amount = map.get(storable);
                tooltip.add(join(noSeparators(), text(amount, GREEN), TIMES, storable.getDisplayName()));
            }
        }
        if (!rejectedAmounts.isEmpty()) {
            tooltip.add(text(tiny("Rejected " + totalRejected + " item" + (totalRejected == 1 ? "" : "s")), RED));
            List<String> list = new ArrayList<>(rejectedAmounts.keySet());
            Collections.sort(list, (a, b) -> Integer.compare(rejectedAmounts.get(b), rejectedAmounts.get(a)));
            for (String name : list) {
                int amount = rejectedAmounts.get(name);
                Component displayName = rejectedDisplayNames.get(name);
                tooltip.add(join(noSeparators(), text(amount, RED), TIMES, displayName));
            }
        }
        player.sendMessage((tooltip.isEmpty()
                            ? message
                            : message.hoverEvent(showText(join(separator(newline()), tooltip))))
                           .clickEvent(runCommand("/ms")));
    }
}
