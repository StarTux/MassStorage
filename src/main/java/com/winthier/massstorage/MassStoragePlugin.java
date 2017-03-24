package com.winthier.massstorage;

import com.winthier.massstorage.sql.SQLItem;
import com.winthier.massstorage.sql.SQLPlayer;
import com.winthier.massstorage.vault.VaultHandler;
import com.winthier.sql.SQLDatabase;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class MassStoragePlugin extends JavaPlugin {
    @Getter private static MassStoragePlugin instance;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private Set<Material> materialBlacklist = null;
    private VaultHandler vaultHandler = null;
    private SQLDatabase db;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        instance = this;
        getCommand("massstorage").setExecutor(new MassStorageCommand(this));
        getCommand("massstorageadmin").setExecutor(new AdminCommand(this));
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        db = new SQLDatabase(this);
        db.registerTables(SQLItem.class, SQLPlayer.class);
        db.createAllTables();
    }

    @Override
    public void onDisable() {
        for (Session session: sessions.values()) session.close();
        sessions.clear();
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
        saveDefaultConfig();
        reloadConfig();
        materialBlacklist = null;
        if (sessions != null) {
            for (Session session: sessions.values()) {
                session.flush();
            }
        }
    }

    boolean permitNonStackingItems() {
        return getConfig().getBoolean("PermitNonStackingItems", true);
    }
}
