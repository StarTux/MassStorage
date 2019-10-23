package com.winthier.massstorage;

import com.winthier.massstorage.sql.SQLItem;
import com.winthier.massstorage.util.Msg;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

@Getter
public final class MenuInventory implements InventoryHolder {
    private final MassStoragePlugin plugin;
    private final Player player;
    private final Inventory inventory;
    private int size;
    private boolean itemView = false;
    private long lastClick = 0;
    private boolean silentClose;
    private int openCategory = -1;
    private InventoryView view = null;

    MenuInventory(MassStoragePlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = plugin.getServer().createInventory(this, 9 * 6, ChatColor.BLUE + "Mass Storage Menu");
    }

    InventoryView open() {
        if (this.view != null) throw new IllegalStateException("MenuInventory opened more than once!");
        this.view = player.openInventory(this.inventory);
        return this.view;
    }

    void prepareMain() {
        Session session = plugin.getSession(player);
        // Insert
        ItemStack icon = new ItemStack(Material.CHEST_MINECART);
        ItemMeta meta = icon.getItemMeta();
        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setDisplayName(ChatColor.BLUE + "Insert Items");
        icon.setItemMeta(meta);
        inventory.setItem(1, icon);
        // Info
        icon = new ItemStack(Material.BOOK);
        meta = icon.getItemMeta();
        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setDisplayName(ChatColor.YELLOW + "Info");
        icon.setItemMeta(meta);
        inventory.setItem(3, icon);
        // Dump
        icon = new ItemStack(Material.HOPPER_MINECART);
        meta = icon.getItemMeta();
        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setDisplayName(ChatColor.DARK_AQUA + "Dump Your Inventory");
        icon.setItemMeta(meta);
        inventory.setItem(5, icon);
        // Auto
        final Material mat;
        final String msg;
        if (session.isAutoStorageEnabled()) {
            mat = Material.SEA_LANTERN;
            msg = ChatColor.AQUA + "Auto Storage Enabled";
        } else {
            mat = Material.BARRIER;
            msg = ChatColor.DARK_RED + "Auto Storage Disabled";
        }
        icon = new ItemStack(mat);
        meta = icon.getItemMeta();
        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setDisplayName(msg);
        icon.setItemMeta(meta);
        inventory.setItem(7, icon);
        //
        int index = 9;
        for (Category category: plugin.getCategories()) {
            // if (category.misc && !session.isDebugModeEnabled()) continue;
            if (category.materials.isEmpty()) continue;
            inventory.setItem(index++, category.icon.clone());
        }
        size = index;
        itemView = false;
    }

    void prepareCategory(Category category) {
        Session session = plugin.getSession(player);
        List<ItemStack> items = new ArrayList<>();
        for (Map.Entry<Item, SQLItem> entry: session.getSQLItems().entrySet()) {
            Item item = entry.getKey();
            SQLItem sqlItem = entry.getValue();
            if (sqlItem.getAmount() > 0
                && (category.materials.contains(item.getMaterial()))) {
                items.add(entry.getKey().toItemStack(1));
            }
        }
        // Collections.sort(items, (a, b) -> {
        //         int c = Integer.compare(a.getType().getId(), b.getType().getId());
        //         if (c != 0) return c;
        //         return Short.compare(a.getDurability(), b.getDurability());
        //     });
        int index = 0;
        for (ItemStack item: items) {
            if (index >= 6 * 9) break;
            inventory.setItem(index++, item);
        }
        size = index;
        itemView = true;
    }

    void onInventoryOpen(InventoryOpenEvent event) {
        Session session = plugin.getSession(player);
        int cat = session.getOpenCategory();
        if (cat < 0 || cat >= plugin.getCategories().size()) {
            prepareMain();
            player.playSound(player.getEyeLocation(), Sound.BLOCK_CHEST_OPEN, SoundCategory.MASTER, 0.2f, 1.5f);
        } else {
            openCategory = cat;
            prepareCategory(plugin.getCategories().get(cat));
            player.playSound(player.getEyeLocation(), Sound.BLOCK_CHEST_CLOSE, SoundCategory.MASTER, 0.2f, 1.5f);
        }
        session.setOpenCategory(-1);
    }

    void onInventoryClose(InventoryCloseEvent event) {
        if (!silentClose) {
            player.playSound(player.getEyeLocation(), Sound.BLOCK_CHEST_CLOSE, SoundCategory.MASTER, 0.2f, 1.4f);
        }
    }

    void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) {
            if (itemView) {
                inventory.clear();
                prepareMain();
                player.playSound(player.getEyeLocation(), Sound.BLOCK_CHEST_CLOSE, SoundCategory.MASTER, 0.2f, 1.5f);
            }
            return;
        }
        if (!event.getClickedInventory().equals(event.getView().getTopInventory())) return;
        int slot = event.getSlot();
        if (slot < 0 || slot > size) return;
        if (event.getCurrentItem() == null) return;
        if (event.getCurrentItem().getType() == Material.AIR) return;
        Session session = plugin.getSession(player);
        long now = System.currentTimeMillis();
        if (!itemView) {
            if (slot < 9) {
                if (lastClick + 300L > now) return;
                lastClick = now;
                switch (slot) {
                case 1:
                    plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getSession(player).openInventory());
                    session.setOpenCategory(-1);
                    silentClose = true;
                    return;
                case 3:
                    plugin.getServer().getScheduler().runTask(plugin, () -> player.performCommand("ms info"));
                    player.playSound(player.getEyeLocation(), Sound.BLOCK_LEVER_CLICK, SoundCategory.MASTER, 0.2f, 2.0f);
                    return;
                case 5:
                    Session.StorageResult result = session.storePlayerInventory(player);
                    result.setShouldReportEmpty(true);
                    session.reportStorageResult(player, result);
                    player.playSound(player.getEyeLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.MASTER, 0.2f, 1.25f);
                    return;
                case 7:
                    boolean newVal = !session.isAutoStorageEnabled();
                    session.setAutoStorageEnabled(newVal);
                    if (newVal) {
                        result = session.storePlayerInventory(player);
                        session.reportStorageResult(player, result);
                        player.playSound(player.getEyeLocation(), Sound.BLOCK_LEVER_CLICK, SoundCategory.MASTER, 0.2f, 1.5f);
                    } else {
                        player.playSound(player.getEyeLocation(), Sound.BLOCK_LEVER_CLICK, SoundCategory.MASTER, 0.2f, 0.5f);
                    }
                    inventory.clear();
                    prepareMain();
                    return;
                default:
                    return;
                }
            }
            int categoryIndex = slot - 9;
            if (categoryIndex > plugin.getCategories().size()) return;
            Category category = plugin.getCategories().get(categoryIndex);
            inventory.clear();
            openCategory = categoryIndex;
            prepareCategory(category);
            if (size <= 0) {
                Msg.info(player, "%s: No items found", category.name);
            } else {
                Msg.info(player, "%s: %d unique items found", category.name, size);
            }
            player.playSound(player.getEyeLocation(), Sound.BLOCK_CHEST_OPEN, SoundCategory.MASTER, 0.2f, 1.5f);
        } else {
            if (lastClick + 300L > now) return;
            lastClick = now;
            ItemStack item = inventory.getItem(slot);
            if (item == null) return;
            if (event.isShiftClick()) {
                Item key = Item.of(item);
                SQLItem sqlItem = session.getSQLItems().get(key);
                int times = event.isRightClick() ? 3 * 9 : 1;
                if (sqlItem.getAmount() <= 0) {
                    inventory.setItem(slot, null);
                    return;
                }
                for (int i = 0; i < times; i += 1) {
                    int amount = Math.min(sqlItem.getAmount(), key.getMaterial().getMaxStackSize());
                    sqlItem.setAmount(sqlItem.getAmount() - amount);
                    ItemStack stack = key.toItemStack(amount);
                    for (ItemStack drop: player.getInventory().addItem(stack).values()) {
                        player.getWorld().dropItem(player.getEyeLocation(), drop).setPickupDelay(0);
                    }
                    if (sqlItem.getAmount() <= 0) {
                        inventory.setItem(slot, null);
                        break;
                    }
                }
                plugin.getDb().save(sqlItem);
                player.playSound(player.getEyeLocation(), Sound.BLOCK_DISPENSER_DISPENSE, SoundCategory.MASTER, 0.2f, 2.0f);
            } else if (event.isRightClick()) {
                Item key = Item.of(item);
                SQLItem sqlItem = session.getSQLItems().get(key);
                NamedItem named = sqlItem.getNamedItem();
                int stacks = (named.getAmount() - 1) / key.getMaterial().getMaxStackSize() + 1;
                Msg.info(player, "&r%d&8x&r%s &7(%d stacks)", named.getAmount(), named.getName(), stacks);
                player.playSound(player.getEyeLocation(), Sound.BLOCK_LEVER_CLICK, SoundCategory.MASTER, 0.2f, 2.0f);
            } else {
                plugin.getServer().getScheduler().runTask(plugin, () -> player.performCommand("ms id " + item.getType().name().toLowerCase()));
                session.setOpenCategory(openCategory);
                silentClose = true;
            }
        }
    }

    void onInventoryDrag(InventoryDragEvent event) {
        event.setCancelled(true);
    }
}
