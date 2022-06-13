package com.cavetale.ms.session;

import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.event.block.PlayerBlockAbilityQuery;
import com.cavetale.core.event.item.PlayerAbsorbItemEvent;
import com.cavetale.ms.MassStoragePlugin;
import com.cavetale.ms.storable.StorableItem;
import com.cavetale.sidebar.PlayerSidebarEvent;
import com.cavetale.sidebar.Priority;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.Lootable;
import static com.cavetale.core.font.Unicode.tiny;
import static com.cavetale.ms.session.ItemInsertionCause.*;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static org.bukkit.Sound.*;

@RequiredArgsConstructor
public final class MassStorageSessions implements Listener {
    private final MassStoragePlugin plugin;
    private final Map<UUID, MassStorageSession> sessionsMap = new HashMap<>();

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        for (Player player : Bukkit.getOnlinePlayers()) {
            of(player).setup();
        }
    }

    private MassStorageSession of(Player player) {
        return sessionsMap.computeIfAbsent(player.getUniqueId(), u -> new MassStorageSession(plugin, u));
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        of(event.getPlayer()).setup();
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event) {
        sessionsMap.remove(event.getPlayer().getUniqueId());
    }

    public MassStorageSession get(Player player) {
        return sessionsMap.get(player.getUniqueId());
    }

    public MassStorageSession get(UUID uuid) {
        return sessionsMap.get(uuid);
    }

    public boolean ifAssistEnabled(Player player, Consumer<MassStorageSession> callback) {
        MassStorageSession session = get(player);
        if (session != null && session.isEnabled() && session.isAssistEnabled()) {
            callback.accept(session);
            return true;
        } else {
            return false;
        }
    }

    public boolean apply(Player player, Consumer<MassStorageSession> callback) {
        MassStorageSession session = sessionsMap.get(player.getUniqueId());
        if (session == null || !session.isEnabled()) return false;
        callback.accept(session);
        return true;
    }

    public MassStorageSession require(Player player) {
        MassStorageSession session = sessionsMap.get(player.getUniqueId());
        if (session == null || !session.isEnabled()) {
            throw new CommandWarn("Your session is not ready. Please try again later.");
        }
        return session;
    }

    private boolean checkGameMode(Player player) {
        switch (player.getGameMode()) {
        case SURVIVAL:
        case ADVENTURE:
            return player.hasPermission("massstorage.ms");
        case CREATIVE:
        case SPECTATOR:
        default:
            return false;
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onPlayerAttemptPickupItem(PlayerAttemptPickupItemEvent event) {
        Player player = event.getPlayer();
        if (!checkGameMode(player)) return;
        MassStorageSession session = get(player);
        if (session == null || !session.isEnabled()) return;
        final boolean assist = session.isAssistEnabled();
        ItemStack item = event.getItem().getItemStack();
        StorableItem storable = plugin.getIndex().get(item);
        if (storable == null || !storable.canStore(item)) return;
        boolean auto = session.getAutoPickup(storable);
        if (!assist && !auto) return;
        final int amount = item.getAmount();
        if (amount == 0) return;
        final int canStack = storable.canStack(player.getInventory(), amount, !auto);
        if (canStack >= amount) return;
        final int insertAmount = amount - canStack;
        session.insertAsync(storable, insertAmount, null);
        new PlayerAbsorbItemEvent(player, event.getItem(), insertAmount).callEvent();
        if (insertAmount >= amount) {
            event.setCancelled(true);
            event.getItem().remove();
        } else {
            ItemStack itemStack = event.getItem().getItemStack();
            itemStack.subtract(insertAmount);
            event.getItem().setItemStack(itemStack);
        }
        new ItemInsertionResult(ItemInsertionCause.PICKUP, List.of(), Map.of(storable, insertAmount))
            .feedback(player);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!checkGameMode(player)) return;
        if (event.getView().getType() == InventoryType.CRAFTING && event.getClick() == ClickType.CONTROL_DROP) {
            ifAssistEnabled(player, session -> {
                    ItemStack item = event.getCurrentItem();
                    session.insertAndSubtract(List.of(item), ASSIST_CONTROL_DROP, result -> result.feedback(player));
                    event.setCancelled(true);
                });
        }
    }

    protected static Inventory getAccessibleWorldContainer(Player player, Block block) {
        if (!PlayerBlockAbilityQuery.Action.OPEN.query(player, block)) {
            player.sendActionBar(text("You cannot open containers here!", RED));
            return null;
        }
        if (!(block.getState() instanceof Container container)) {
            player.sendActionBar(text("There is no container here!", RED));
            return null;
        }
        if (container.isLocked()) {
            // Apparently this is cancelled beforehand.
            player.sendActionBar(text("This container is locked!", RED));
            return null;
        }
        if (container instanceof Lootable lootable && lootable.hasLootTable()) {
            // Apparently looting happens prior to this.
            player.sendActionBar(text("This container has yet to be looted!", RED));
            return null;
        }
        return container.getInventory();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onPlayerInteractContainerAction(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!checkGameMode(player)) return;
        MassStorageSession session = get(player);
        if (session == null || !session.isEnabled()) return;
        if (session.getAction() instanceof SessionWorldContainerAction sessionAction) {
            switch (event.getAction()) {
            case RIGHT_CLICK_BLOCK:
            case LEFT_CLICK_BLOCK:
                break;
            default: return;
            }
            if (!event.hasBlock()) return;
            Block block = event.getClickedBlock();
            Inventory inventory = getAccessibleWorldContainer(player, block);
            if (inventory == null) {
                player.playSound(player.getLocation(), BLOCK_CHEST_LOCKED, 1.0f, 1.25f);
                return;
            }
            if (sessionAction instanceof SessionFillWorldContainer fill) {
                event.setCancelled(true);
                session.setAction(null);
                session.fillContainer(player, block, inventory, fill.getStorable());
            } else if (sessionAction instanceof SessionDrainWorldContainer drain) {
                event.setCancelled(true);
                session.setAction(null);
                session.insertAndSubtract(inventory, CONTAINER_DRAIN, result -> result.feedback(player));
            }
        }
    }

    @EventHandler
    private void onPlayerSidebar(PlayerSidebarEvent event) {
        Player player = event.getPlayer();
        MassStorageSession session = get(player);
        if (session == null || !session.isEnabled()) return;
        if (session.getAction() instanceof SessionWorldContainerAction sessionAction) {
            if (sessionAction instanceof SessionFillWorldContainer fill) {
                event.add(plugin, Priority.HIGHEST, List.of(join(noSeparators(), text("/ms ", YELLOW), text(tiny("Container fill"), AQUA)),
                                                            join(noSeparators(), text(tiny("mode: "), AQUA), fill.getStorable().getDisplayName())));
            } else if (sessionAction instanceof SessionDrainWorldContainer drain) {
                event.add(plugin, Priority.HIGHEST, List.of(join(noSeparators(), text(tiny("/ms "), YELLOW), text(tiny("Container drain"), AQUA)),
                                                            text(tiny("mode"), AQUA)));
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!checkGameMode(player)) return;
        ifAssistEnabled(player, session -> {
                session.stackHand(player, event.getHand());
            });
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (!checkGameMode(player)) return;
        ifAssistEnabled(player, session -> {
                session.stackHand(player, event.getHand());
            });
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (!checkGameMode(player)) return;
        EquipmentSlot hand = player.getInventory().getItemInMainHand().equals(event.getItem())
            ? EquipmentSlot.HAND
            : EquipmentSlot.OFF_HAND;
        ifAssistEnabled(player, session -> {
                session.stackHand(player, hand);
            });
    }
}
