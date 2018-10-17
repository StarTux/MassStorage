package com.winthier.massstorage;

import com.winthier.generic_events.GenericEvents;
import com.winthier.massstorage.sql.SQLItem;
import com.winthier.massstorage.sql.SQLPlayer;
import com.winthier.sql.SQLDatabase;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class MassStoragePlugin extends JavaPlugin {
    @Getter private static MassStoragePlugin instance;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final List<Category> categories = new ArrayList<>();
    private Set<Material> materialBlacklist = null;
    private SQLDatabase db;
    private final MassStorageCommand massStorageCommand = new MassStorageCommand(this);

    @Override
    public void onEnable() {
        instance = this;
        reloadAll();
        getCommand("massstorage").setExecutor(massStorageCommand);
        getCommand("massstorageadmin").setExecutor(new AdminCommand(this));
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        db = new SQLDatabase(this);
        db.registerTables(SQLItem.class, SQLPlayer.class);
        db.createAllTables();
        getServer().getScheduler().runTaskTimer(this, () -> on20Ticks(), 20, 20);
    }

    @Override
    public void onDisable() {
        for (Session session: sessions.values()) session.close();
        for (Player player: getServer().getOnlinePlayers()) {
            InventoryView playerView = player.getOpenInventory();
            if (playerView == null) continue;
            if (!(playerView.getTopInventory().getHolder() instanceof MenuInventory)) continue;
            player.closeInventory();
        }
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

    void reloadAll() {
        saveDefaultConfig();
        reloadConfig();
        materialBlacklist = null;
        if (sessions != null) {
            for (Session session: sessions.values()) {
                session.flush();
            }
        }
        categories.clear();
        Set<Material> miscMaterials = EnumSet.noneOf(Material.class);
        for (Material mat: Material.values()) {
            if (!getMaterialBlacklist().contains(mat) && mat.isItem() && !mat.isLegacy() && !mat.name().startsWith("LEGACY_")) {
                miscMaterials.add(mat);
            }
        }
        ConfigurationSection menuConfig;
        File file = new File(getDataFolder(), "menu.yml");
        if (file.isFile()) {
            menuConfig = YamlConfiguration.loadConfiguration(file);
        } else {
            menuConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(getResource("menu.yml")));
        }
        for (Map<?, ?> map: menuConfig.getMapList("Categories")) {
            ConfigurationSection section = menuConfig.createSection("tmp", map);
            try {
                boolean misc = section.getBoolean("Misc");
                Set<Material> materials;
                if (misc) {
                    materials = miscMaterials;
                } else {
                    materials = EnumSet.noneOf(Material.class);
                    for (String str: section.getStringList("Materials")) {
                        try {
                            materials.add(Material.valueOf(str));
                        } catch (IllegalArgumentException iae) {
                            iae.printStackTrace();
                            continue;
                        }
                    }
                    miscMaterials.removeAll(materials);
                }
                String name = section.getString("Name");
                ItemStack icon;
                try {
                    icon = new ItemStack(Material.valueOf(section.getString("Icon")));
                } catch (IllegalArgumentException iae) {
                    iae.printStackTrace();
                    icon = new ItemStack(Material.SMOOTH_STONE);
                }
                ItemMeta meta = icon.getItemMeta();
                meta.setDisplayName(ChatColor.RESET + name);
                icon.setItemMeta(meta);
                Category category = new Category(name, icon, misc, materials);
                categories.add(category);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    boolean permitNonStackingItems() {
        return getConfig().getBoolean("PermitNonStackingItems", true);
    }

    void on20Ticks() {
        long now = System.currentTimeMillis();
        for (Session session: sessions.values()) {
            if (!session.isAutoStorageEnabled()) continue;
            Player player = session.getPlayer();
            if (player == null) continue;
            if (session.getLastAutoStorage() + 1000L >= now) continue;
            int emptySlots = 0;
            PlayerInventory inv = player.getInventory();
            for (int i = 9; i < 36; i += 1) {
                ItemStack item = inv.getItem(i);
                if (item == null || item.getAmount() == 0) {
                    emptySlots += 1;
                    if (emptySlots > 4) break;
                }
            }
            if (emptySlots > 4) continue;
            session.setLastAutoStorage(now);
            Session.StorageResult result = session.storePlayerInventory(player);
            session.reportStorageResult(player, result);
        }
    }

    String getItemName(ItemStack item) {
        String genName = GenericEvents.getItemName(item);
        if (genName != null) return genName;
        String name = item.getType().name();
        String[] arr = name.split("_");
        if (arr.length == 0) return name;
        for (int i = 0; i < arr.length; ++i) {
            arr[i] = arr[i].substring(0, 1) + arr[i].substring(1).toLowerCase();
        }
        StringBuilder sb = new StringBuilder(arr[0]);
        for (int i = 1; i < arr.length; ++i) {
            sb.append(" ").append(arr[i]);
        }
        return sb.toString();
    }
}

@RequiredArgsConstructor
class Category {
    final String name;
    final ItemStack icon;
    final boolean misc;
    final Set<Material> materials;
}
