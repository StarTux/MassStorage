package com.winthier.massstorage;

import com.winthier.massstorage.util.Msg;
import com.winthier.sql.SQLDatabase;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.persistence.PersistenceException;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Container;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class MassStoragePlugin extends JavaPlugin {
    final Map<UUID, Session> sessions = new HashMap<>();
    final List<Category> categories = new ArrayList<>();
    Set<Material> materialBlacklist = null;
    SQLDatabase db;
    final MassStorageCommand massStorageCommand = new MassStorageCommand(this);
    boolean saveAsync = false;
    Set<Material> miscMaterials = EnumSet.noneOf(Material.class);

    @Override
    public void onEnable() {
        reloadAll();
        getCommand("massstorage").setExecutor(massStorageCommand);
        getCommand("massstorageadmin").setExecutor(new AdminCommand(this));
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        db = new SQLDatabase(this);
        db.registerTables(SQLItem.class);
        db.createAllTables();
        getServer().getScheduler().runTaskTimer(this, () -> on20Ticks(), 20, 20);
        this.saveAsync = true;
    }

    @Override
    public void onDisable() {
        this.saveAsync = false;
        for (Session session : sessions.values()) {
            session.close();
        }
        for (Player player : getServer().getOnlinePlayers()) {
            InventoryView playerView = player.getOpenInventory();
            if (playerView == null) continue;
            if (playerView.getTopInventory().getHolder() instanceof MenuInventory) {
                player.closeInventory();
            }
        }
        sessions.clear();
    }

    void saveToDatabase(Object rows) {
        if (this.saveAsync) {
            db.saveAsync(rows, null);
        } else {
            try {
                db.save(rows);
            } catch (PersistenceException pe) {
                pe.printStackTrace();
            }
        }
    }

    Session getSession(Player player) {
        final UUID uuid = player.getUniqueId();
        Session result = sessions.get(uuid);
        if (result == null) {
            result = new Session(this, uuid);
            sessions.put(uuid, result);
        }
        return result;
    }

    Set<Material> getMaterialBlacklist() {
        if (materialBlacklist == null) {
            materialBlacklist = EnumSet.noneOf(Material.class);
            for (String str : getConfig().getStringList("MaterialBlacklist")) {
                Material mat = materialOf(str.toUpperCase());
                if (mat == null) {
                    continue;
                } else if (!mat.isItem() || mat.isLegacy()) {
                    getLogger().warning("MaterialBlacklist: Obsolete material: " + mat);
                    continue;
                }
                materialBlacklist.add(mat);
            }
        }
        return materialBlacklist;
    }

    void reloadAll() {
        saveDefaultConfig();
        if (!new File(getDataFolder(), "menu.yml").exists()) {
            saveResource("menu.yml", false);
        }
        reloadConfig();
        materialBlacklist = null;
        if (sessions != null) {
            for (Session session : sessions.values()) {
                session.flush();
            }
        }
        categories.clear();
        for (Material mat : Material.values()) {
            if (getMaterialBlacklist().contains(mat)) continue;
            if (!mat.isItem()) continue;
            if (mat.isLegacy() || mat.name().startsWith("LEGACY_")) continue;
            if (mat.name().endsWith("_SPAWN_EGG")) continue;
            miscMaterials.add(mat);
        }
        ConfigurationSection menuConfig;
        File file = new File(getDataFolder(), "menu.yml");
        if (file.isFile()) {
            menuConfig = YamlConfiguration.loadConfiguration(file);
        } else {
            menuConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(getResource("menu.yml")));
        }
        for (Map<?, ?> map : menuConfig.getMapList("Categories")) {
            ConfigurationSection section = menuConfig.createSection("tmp", map);
            try {
                boolean misc = section.getBoolean("Misc");
                Collection<Material> materials;
                String name = section.getString("Name");
                if (misc) {
                    materials = miscMaterials;
                } else {
                    materials = new ArrayList<>();
                    for (String str : section.getStringList("Materials")) {
                        Material mat = materialOf(str);
                        if (mat == null) {
                            continue;
                        } else if (!mat.isItem() || mat.isLegacy()) {
                            getLogger().warning("Categories: Obsolete material: " + mat);
                            continue;
                        }
                        if (!materials.contains(mat)) {
                            materials.add(mat);
                        }
                    }
                    miscMaterials.removeAll(materials);
                }
                Material iconMat = materialOf(section.getString("Icon"));
                if (iconMat == null) iconMat = Material.SMOOTH_STONE;
                ItemStack icon = new ItemStack(iconMat);
                ItemMeta meta = icon.getItemMeta();
                meta.setDisplayName(ChatColor.RESET + name);
                List<String> lore = new ArrayList<>();
                lore.add("" + ChatColor.GRAY + materials.size() + " items");
                lore.add("");
                lore.add(Msg.format("Left-click &7Open item list"));
                lore.add(Msg.format("Click outside chest &7Go back"));
                meta.setLore(lore);
                meta.addItemFlags(ItemFlag.values());
                icon.setItemMeta(meta);
                Category category = new Category(name, icon, misc, materials);
                categories.add(category);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Material materialOf(String str) {
        try {
            return Material.valueOf(str);
        } catch (IllegalArgumentException iae) {
            getLogger().warning("Invalid material: " + str);
            return null;
        }
    }

    boolean permitNonStackingItems() {
        return getConfig().getBoolean("PermitNonStackingItems", true);
    }

    void on20Ticks() {
        long now = System.currentTimeMillis();
        for (Session session : sessions.values()) {
            if (!session.isAutoStorageEnabled()) continue;
            Player player = session.getPlayer();
            if (player == null) continue;
            if (player.getGameMode() != GameMode.SURVIVAL
                && player.getGameMode() != GameMode.ADVENTURE) continue;
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
        String name = item.getType().name();
        String[] arr = name.split("_");
        if (arr.length == 0) return name;
        for (int i = 0; i < arr.length; i += 1) {
            arr[i] = arr[i].substring(0, 1) + arr[i].substring(1).toLowerCase();
        }
        StringBuilder sb = new StringBuilder(arr[0]);
        for (int i = 1; i < arr.length; i += 1) {
            sb.append(" ").append(arr[i]);
        }
        return sb.toString();
    }

    boolean canStore(ItemStack itemStack) {
        Material mat = itemStack.getType();
        if (getMaterialBlacklist().contains(mat)) return false;
        if (mat.name().endsWith("_SPAWN_EGG")) return false;
        if (mat.getMaxStackSize() == 1 && !permitNonStackingItems()) return false;
        if (mat.getMaxDurability() > 0 && itemStack.getDurability() > 0) return false;
        ItemStack dfl = new ItemStack(itemStack.getType());
        if (dfl.isSimilar(itemStack)) return true;
        if (Tag.SHULKER_BOXES.isTagged(itemStack.getType())) {
            BlockStateMeta meta = (BlockStateMeta) itemStack.getItemMeta();
            if (meta.hasDisplayName() || meta.hasLore() || meta.hasCustomModelData()
                || meta.hasAttributeModifiers() || meta.hasPlaceableKeys() || meta.hasEnchants()
                || meta.isUnbreakable() || !meta.getPersistentDataContainer().isEmpty()) {
                return false;
            }
            Container container = (Container) meta.getBlockState();
            return container.getInventory().isEmpty();
        }
        switch (itemStack.getType()) {
        case FIREWORK_ROCKET: {
            FireworkMeta meta = (FireworkMeta) dfl.getItemMeta();
            meta.setPower(1); // Default power
            dfl.setItemMeta(meta);
            return dfl.isSimilar(itemStack);
        }
        case BLACK_BANNER:
        case BLUE_BANNER:
        case BROWN_BANNER:
        case CYAN_BANNER:
        case GRAY_BANNER:
        case GREEN_BANNER:
        case LIGHT_BLUE_BANNER:
        case LIGHT_GRAY_BANNER:
        case LIME_BANNER:
        case MAGENTA_BANNER:
        case ORANGE_BANNER:
        case PINK_BANNER:
        case PURPLE_BANNER:
        case RED_BANNER:
        case WHITE_BANNER:
        case YELLOW_BANNER: {
            BannerMeta meta = (BannerMeta) dfl.getItemMeta();
            return meta.equals(itemStack.getItemMeta());
        }
        default: return false;
        }
    }

    public NamedItem getNamedItem(SQLItem row) {
        Material mat = row.getMat();
        ItemStack stack = new ItemStack(mat);
        String i18n = stack.getI18NDisplayName();
        return new NamedItem(mat, row.amount, getItemName(stack), i18n);
    }
}
