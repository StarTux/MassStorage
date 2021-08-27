package com.winthier.massstorage;

import com.cavetale.core.event.player.PluginPlayerEvent;
import com.winthier.massstorage.util.Msg;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
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

public final class MenuInventory implements InventoryHolder {
    protected static final int SLOT_INSERT = 0;
    protected static final int SLOT_INFO = 2;
    protected static final int SLOT_DUMP = 4;
    protected static final int SLOT_SHOWALL = 6;
    protected static final int SLOT_AUTO = 8;
    final MassStoragePlugin plugin;
    final Player player;
    @Getter final Inventory inventory;
    int size;
    boolean itemView = false;
    long lastClick = 0;
    boolean silentClose;
    int openCategory = -1;
    InventoryView view = null;

    MenuInventory(final MassStoragePlugin plugin, final Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = plugin.getServer().createInventory(this, 9 * 6, ChatColor.BLUE + "Mass Storage Menu");
    }

    InventoryView open() {
        if (this.view != null) throw new IllegalStateException("MenuInventory opened more than once!");
        this.view = player.openInventory(this.inventory);
        PluginPlayerEvent.Name.OPEN_MASS_STORAGE.call(plugin, player);
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
        inventory.setItem(SLOT_INSERT, icon);
        // Info
        icon = new ItemStack(Material.BOOK);
        meta = icon.getItemMeta();
        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setDisplayName(ChatColor.YELLOW + "Info");
        icon.setItemMeta(meta);
        inventory.setItem(SLOT_INFO, icon);
        // Dump
        icon = new ItemStack(Material.HOPPER_MINECART);
        meta = icon.getItemMeta();
        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setDisplayName(ChatColor.DARK_AQUA + "Dump Your Inventory");
        icon.setItemMeta(meta);
        inventory.setItem(SLOT_DUMP, icon);
        // Show All
        do {
            final Material mat;
            final String msg;
            if (session.isShowAll()) {
                mat = Material.CHEST;
                msg = ChatColor.GREEN + "Show All Items";
            } else {
                mat = Material.BARRIER;
                msg = ChatColor.RED + "Show Only Owned Items";
            }
            icon = new ItemStack(mat);
            meta = icon.getItemMeta();
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.setDisplayName(msg);
            icon.setItemMeta(meta);
            inventory.setItem(SLOT_SHOWALL, icon);
        } while (false);
        // Auto
        do {
            final Material mat;
            final String msg;
            if (session.isAutoStorageEnabled()) {
                mat = Material.LANTERN;
                msg = ChatColor.AQUA + "Auto Storage Enabled";
            } else {
                mat = Material.SOUL_LANTERN;
                msg = ChatColor.DARK_RED + "Auto Storage Disabled";
            }
            icon = new ItemStack(mat);
            meta = icon.getItemMeta();
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.setDisplayName(msg);
            icon.setItemMeta(meta);
            inventory.setItem(SLOT_AUTO, icon);
        } while (false);
        //
        int index = 9;
        for (Category category: plugin.getCategories()) {
            if (category.materials.isEmpty()) continue;
            inventory.setItem(index++, category.icon.clone());
        }
        size = index;
        itemView = false;
    }

    void prepareCategory(Category category) {
        Session session = plugin.getSession(player);
        List<ItemStack> items = new ArrayList<>();
        for (Material mat : category.materials) {
            SQLItem sqlItem = session.getSQLItems().get(mat);
            final int amount = sqlItem != null ? sqlItem.getAmount() : 0;
            int stackSize = Math.max(1, Math.min(mat.getMaxStackSize(), amount));
            ItemStack itemStack = amount > 0 || session.isShowAll()
                ? new ItemStack(mat, stackSize)
                : new ItemStack(Material.BARRIER);
            ItemMeta meta = itemStack.getItemMeta();
            if (amount == 0) {
                meta.setDisplayName(ChatColor.DARK_RED + new ItemStack(mat).getI18NDisplayName());
            }
            int stacks = (amount == 0) ? 0 : (amount - 1) / mat.getMaxStackSize() + 1;
            int doubleChests = (amount == 0) ? 0 : (stacks - 1) / (6 * 9) + 1;
            meta.setLore(Arrays.asList(Msg.format("&7In Storage:"),
                                       Msg.format("&8Items: &7%d", amount),
                                       Msg.format("&8Stacks: &7%d", stacks),
                                       Msg.format("&8Double Chests: &7%d", doubleChests),
                                       "",
                                       Msg.format("Left-click &7Open item chest"),
                                       Msg.format("Right-click &7Info"),
                                       Msg.format("Shift-click &7Drop stack"),
                                       Msg.format("Shift-right-click &7Drop chest"),
                                       Msg.format("Click outside chest &7Go back")));
            meta.addItemFlags(ItemFlag.values());
            itemStack.setItemMeta(meta);
            items.add(itemStack);
        }
        int index = 0;
        for (ItemStack item : items) {
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
                case SLOT_INSERT:
                    plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getSession(player).openInventory());
                    session.setOpenCategory(-1);
                    silentClose = true;
                    return;
                case SLOT_INFO:
                    plugin.getServer().getScheduler().runTask(plugin, () -> player.performCommand("ms info"));
                    player.playSound(player.getEyeLocation(), Sound.BLOCK_LEVER_CLICK, SoundCategory.MASTER, 0.2f, 2.0f);
                    return;
                case SLOT_DUMP:
                    Session.StorageResult result = session.storePlayerInventory(player);
                    result.setShouldReportEmpty(true);
                    session.reportStorageResult(player, result);
                    player.playSound(player.getEyeLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.MASTER, 0.2f, 1.25f);
                    return;
                case SLOT_SHOWALL: {
                    boolean newVal = !session.isShowAll();
                    session.setShowAll(newVal);
                    if (newVal) {
                        player.playSound(player.getEyeLocation(), Sound.BLOCK_LEVER_CLICK, SoundCategory.MASTER, 0.5f, 1.5f);
                    } else {
                        player.playSound(player.getEyeLocation(), Sound.BLOCK_LEVER_CLICK, SoundCategory.MASTER, 0.5f, 0.5f);
                    }
                    inventory.clear();
                    prepareMain();
                    return;
                }
                case SLOT_AUTO: {
                    boolean newVal = !session.isAutoStorageEnabled();
                    session.setAutoStorageEnabled(newVal);
                    if (newVal) {
                        result = session.storePlayerInventory(player);
                        session.reportStorageResult(player, result);
                        player.playSound(player.getEyeLocation(), Sound.BLOCK_LEVER_CLICK, SoundCategory.MASTER, 0.5f, 1.5f);
                    } else {
                        player.playSound(player.getEyeLocation(), Sound.BLOCK_LEVER_CLICK, SoundCategory.MASTER, 0.5f, 0.5f);
                    }
                    inventory.clear();
                    prepareMain();
                    return;
                }
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
                if (now < session.shiftClickCooldown) {
                    player.playSound(player.getEyeLocation(), Sound.BLOCK_DISPENSER_DISPENSE, SoundCategory.MASTER, 0.2f, 0.5f);
                    return;
                }
                Material mat = item.getType();
                SQLItem sqlItem = session.getSQLItems().get(mat);
                final int times;
                if (event.isRightClick()) {
                    Location location = player.getLocation();
                    World world = location.getWorld();
                    int nearbyItems = world.getNearbyEntitiesByType(Item.class, player.getLocation(), 2.0, 2.0, 2.0).size();
                    times = Math.max(1, 3 * 9 - nearbyItems);
                } else {
                    times = 1;
                }
                if (sqlItem == null || sqlItem.getAmount() <= 0) {
                    return;
                }
                session.shiftClickCooldown = now + 5000L;
                for (int i = 0; i < times; i += 1) {
                    int amount = Math.min(sqlItem.getAmount(), mat.getMaxStackSize());
                    sqlItem.setAmount(sqlItem.getAmount() - amount);
                    ItemStack stack = new ItemStack(mat, amount);
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
                Material mat = item.getType();
                SQLItem sqlItem = session.getSQLItems().get(mat);
                if (sqlItem == null) return;
                NamedItem named = plugin.getNamedItem(sqlItem);
                int stacks = (named.getAmount() - 1) / mat.getMaxStackSize() + 1;
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
