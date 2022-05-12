package com.cavetale.ms.session;

import com.cavetale.core.command.CommandWarn;
import com.cavetale.ms.MassStoragePlugin;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

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
}
