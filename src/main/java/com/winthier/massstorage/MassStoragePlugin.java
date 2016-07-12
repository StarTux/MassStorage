package com.winthier.massstorage;

import com.winthier.massstorage.sql.*;
import com.winthier.massstorage.vault.VaultHandler;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.persistence.PersistenceException;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class MassStoragePlugin extends JavaPlugin {
    @Getter static MassStoragePlugin instance;
    final Map<UUID, Session> sessions = new HashMap<>();
    Set<Material> materialBlacklist = null;
    @Getter VaultHandler vaultHandler = null;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        instance = this;
        getCommand("massstorage").setExecutor(new MassStorageCommand(this));
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        // SQL
        try {
            for (Class<?> clazz: getDatabaseClasses()) {
                getDatabase().find(clazz).findRowCount();
            }
        } catch (PersistenceException pe) {
            getLogger().info("Installing database DDL");
            installDDL();
        }
    }

    @Override
    public void onDisable() {
    }

    @Override
    public List<Class<?>> getDatabaseClasses() {
        return Arrays.asList(
            SQLItem.class,
            SQLPlayer.class
            );
    }

    Session getSession(Player player) {
        final UUID uuid = player.getUniqueId();
        Session result = sessions.get(uuid);
        if (result == null) {
            result = new Session(uuid);
            sessions.put(uuid, result);
        }
        return result;
    }

    Set<Material> getMaterialBlacklist() {
        if (materialBlacklist == null) {
            materialBlacklist = EnumSet.noneOf(Material.class);
            for (String str: getConfig().getStringList("MaterialBlacklist")) {
                try {
                    Material mat = Material.valueOf(str.toUpperCase());
                    materialBlacklist.add(mat);
                } catch (IllegalArgumentException iae) {
                    getLogger().warning("Unknown material: " + str);
                }
            }
        }
        return materialBlacklist;
    }

    VaultHandler getVaultHandler() {
        if (vaultHandler == null) {
            if (null != getServer().getPluginManager().getPlugin("Vault")) {
                vaultHandler = new VaultHandler();
            } else {
                getLogger().warning("Could not find Vault!");
            }
        }
        return vaultHandler;
    }

    void reloadAll() {
        reloadConfig();
        materialBlacklist = null;
        if (sessions != null) {
            for (Session session: sessions.values()) {
                session.flush();
            }
        }
    }
}
