package com.cavetale.ms.session;

import com.cavetale.core.font.VanillaItems;
import com.cavetale.ms.MassStoragePlugin;
import com.cavetale.ms.storable.StorableItem;
import com.cavetale.mytems.Mytems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import static com.cavetale.core.font.Unicode.tiny;
import static com.cavetale.ms.dialogue.MassStorageDialogue.TIMES;
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

public final record ItemInsertionResult(ItemInsertionCause cause,
                                        List<ItemStack> rejects,
                                        Map<StorableItem, Integer> storedItems) {
    public int getStoredAmount(StorableItem item) {
        return storedItems().getOrDefault(item, 0);
    }

    public boolean success() {
        return !storedItems().isEmpty();
    }

    public int totalStored() {
        int result = 0;
        for (Integer it : storedItems.values()) {
            result += it;
        }
        return result;
    }

    public int totalRejected() {
        int result = 0;
        for (ItemStack item : rejects()) {
            if (item == null || item.getType().isAir()) continue;
            result += item.getAmount();
        }
        return result;
    }

    public void feedback(Player player) {
        final int totalStored = totalStored();
        if (cause().failSilently() && totalStored == 0) return;
        final int totalRejected = totalRejected();
        Map<String, Integer> rejectedAmounts = new HashMap<>();
        Map<String, Component> rejectedDisplayNames = new HashMap<>();
        for (ItemStack item : rejects()) {
            if (item == null || item.getType().isAir()) continue;
            String name;
            Component displayName;
            Mytems mytems = Mytems.forItem(item);
            if (mytems != null) {
                name = mytems.id;
                displayName = join(noSeparators(), mytems.component, mytems.getMytem().getDisplayName());
            } else {
                name = item.getType().getKey().toString();
                displayName = join(noSeparators(), VanillaItems.componentOf(item.getType()),
                                   text(item.getI18NDisplayName(), item.getType().getItemRarity().getColor()));
            }
            int amount = rejectedAmounts.getOrDefault(name, 0);
            rejectedAmounts.put(name, amount + item.getAmount());
            rejectedDisplayNames.put(name, displayName);
        }
        Component message;
        if (totalStored == 0) {
            if (rejectedDisplayNames.size() == 1) {
                Component displayName = rejectedDisplayNames.values().iterator().next();
                message = join(noSeparators(), text("Cannot store ", RED), displayName);
            } else {
                message = text("No items could be stored!", RED);
            }
            fail(player);
        } else if (storedItems().size() == 1) {
            StorableItem storable = storedItems.keySet().iterator().next();
            int amount = getStoredAmount(storable);
            message = join(noSeparators(), text("Stored ", GREEN), text(amount, WHITE), TIMES, storable.getIconName());
        } else {
            message = text("Stored " + totalStored + " item" + (totalStored == 1 ? "" : "s"), GREEN);
            pickup(player, storedItems().size());
        }
        if (cause().sendChatMessage()) {
            List<Component> tooltip = new ArrayList<>();
            if (totalStored > 0) {
                tooltip.add(text(tiny("Stored " + totalStored + " item" + (totalStored == 1 ? "" : "s")), GREEN));
                List<StorableItem> list = new ArrayList<>(storedItems().keySet());
                Collections.sort(list, (a, b) -> Integer.compare(getStoredAmount(b), getStoredAmount(a)));
                for (StorableItem storable : list) {
                    int amount = getStoredAmount(storable);
                    tooltip.add(join(noSeparators(), Mytems.CHECKED_CHECKBOX.component,
                                     text(amount, GREEN), TIMES, storable.getIconName()));
                }
            }
            if (!rejectedAmounts.isEmpty()) {
                tooltip.add(text(tiny("Rejected " + totalRejected + " item" + (totalRejected == 1 ? "" : "s")), RED));
                List<String> list = new ArrayList<>(rejectedAmounts.keySet());
                Collections.sort(list, (a, b) -> Integer.compare(rejectedAmounts.get(b), rejectedAmounts.get(a)));
                for (String name : list) {
                    int amount = rejectedAmounts.get(name);
                    Component displayName = rejectedDisplayNames.get(name);
                    tooltip.add(join(noSeparators(), Mytems.CROSSED_CHECKBOX.component,
                                     text(amount, RED), TIMES, displayName));
                }
            }
            player.sendMessage((tooltip.isEmpty()
                                ? message
                                : message.hoverEvent(showText(join(separator(newline()), tooltip))))
                               .clickEvent(runCommand("/ms")));
        }
        if (cause().sendActionBarMessage()) {
            player.sendActionBar(message);
        }
    }

    private static void pickup(Player player, int amount) {
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
                }, (long) (i * 2));
        }
    }

    private static void fail(Player player) {
        player.playSound(player.getLocation(), UI_BUTTON_CLICK, MASTER, 0.5f, 0.5f);
    }
}
