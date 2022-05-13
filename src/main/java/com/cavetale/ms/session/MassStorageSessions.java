package com.cavetale.ms.session;

import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.event.block.PlayerBlockAbilityQuery;
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
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import static com.cavetale.core.font.Unicode.tiny;
import static com.cavetale.ms.dialogue.MassStorageDialogue.TIMES;
import static com.cavetale.ms.dialogue.MassStorageDialogue.insertResponse;
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

    public boolean ifAssistantEnabled(Player player, Consumer<MassStorageSession> callback) {
        MassStorageSession session = get(player);
        if (session != null && session.isEnabled() && session.isAssistantEnabled()) {
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onPlayerAttemptPickupItem(PlayerAttemptPickupItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem().getItemStack();
        StorableItem storable = plugin.getIndex().get(item);
        if (storable == null || !storable.canStore(item)) return;
        if (storable.canStack(player.getInventory(), item.getAmount())) return;
        ifAssistantEnabled(player, session -> {
                boolean on = session.insertAndSubtract(List.of(item), (rejects, map) -> {
                        if (!map.isEmpty()) insertResponse(player, rejects, map);
                    });
                if (on) {
                    event.setCancelled(true);
                    event.getItem().remove();
                }
            });
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        //event.getWhoClicked().sendMessage(event.getEventName() + " " + event.getClick() + " " + event.getView().getType());
        if (event.getView().getType() == InventoryType.CRAFTING && event.getClick() == ClickType.CONTROL_DROP) {
            ifAssistantEnabled(player, session -> {
                    ItemStack item = event.getCurrentItem();
                    session.insertAndSubtract(List.of(item), (rejects, map) -> insertResponse(player, rejects, map));
                    event.setCancelled(true);
                });
        }
    }

    private static Inventory getWorldContainerHelper(Player player, Block block) {
        if (!PlayerBlockAbilityQuery.Action.OPEN.query(player, block)) {
            player.sendActionBar(text("You cannot open containers here!", RED));
            return null;
        }
        if (!(block.getState() instanceof Container container)) {
            player.sendActionBar(text("There is no container here!", RED));
            return null;
        }
        return container.getInventory();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        MassStorageSession session = get(player);
        if (session == null || !session.isEnabled()) return;
        if (session.getAction() instanceof SessionWorldContainerAction sessionAction) {
            if ((event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK) && event.hasBlock()) {
                event.setCancelled(true);
                Inventory inventory = getWorldContainerHelper(player, event.getClickedBlock());
                if (inventory == null) {
                    player.playSound(player.getLocation(), BLOCK_CHEST_LOCKED, 1.0f, 1.25f);
                }
                if (sessionAction instanceof SessionFillWorldContainer fill) {
                    session.setAction(null);
                    final int has = session.getAmount(fill.storable);
                    StorableItem storable = fill.getStorable();
                    if (has == 0) {
                        player.sendMessage(join(noSeparators(), text("You are out of ", RED), storable.getDisplayName()));
                        return;
                    }
                    final int amount = storable.fit(inventory, has, false);
                    if (amount == 0) {
                        player.sendMessage(text("This container is full!", RED));
                        player.playSound(player.getLocation(), BLOCK_CHEST_LOCKED, 1.0f, 1.25f);
                        return;
                    }
                    session.retrieveAsync(storable, amount, success -> {
                            if (!success) {
                                player.sendMessage(join(noSeparators(), text("You are out of ", RED), storable.getDisplayName()));
                                player.playSound(player.getLocation(), BLOCK_CHEST_LOCKED, 1.0f, 1.25f);
                                return;
                            }
                            int stored = storable.fit(inventory, amount, true);
                            if (stored < amount) {
                                session.insertAsync(storable, amount - stored, null);
                            }
                            player.sendMessage(join(noSeparators(), text("Filled container with ", GREEN),
                                                    text(stored, WHITE), TIMES, storable.getDisplayName()));
                            session.getDialogue().open(player);
                            player.playSound(player.getLocation(), BLOCK_ENDER_CHEST_OPEN, 0.5f, 2.0f);
                        });
                } else if (sessionAction instanceof SessionDrainWorldContainer drain) {
                    session.setAction(null);
                    session.insertAndSubtract(inventory, (rejects, map) -> insertResponse(player, rejects, map));
                }
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
        ifAssistantEnabled(player, session -> {
                session.stackHand(player, event.getHand());
            });
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        ifAssistantEnabled(player, session -> {
                session.stackHand(player, event.getHand());
            });
    }
}
