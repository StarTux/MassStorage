package com.cavetale.ms.session;

import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.ms.MassStoragePlugin;
import com.cavetale.ms.dialogue.ItemSortOrder;
import com.cavetale.ms.dialogue.MassStorageDialogue;
import com.cavetale.ms.sql.SQLPlayer;
import com.cavetale.ms.sql.SQLStorable;
import com.cavetale.ms.storable.*;

import java.util.*;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import static com.cavetale.core.font.Unicode.subscript;
import static com.cavetale.core.font.Unicode.tiny;
import static com.cavetale.ms.dialogue.MassStorageDialogue.TIMES;
import static com.cavetale.ms.session.MassStorageSessions.getAccessibleWorldContainer;
import static com.cavetale.mytems.util.Text.toCamelCase;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static org.bukkit.Sound.*;

public final class MassStorageSession {
    private final MassStoragePlugin plugin;
    private SQLPlayer playerRow;
    private final UUID uuid;
    private final int[] ids;
    private final int[] amounts;
    private final boolean[] autos;
    private final int[] favs;
    @Getter private boolean enabled;
    private MassStorageDialogue dialogue;
    @Getter @Setter private SessionAction action;
    private boolean stackingHand = false;
    private boolean fillingContainer = false;
    private List<StorableDisplay> storableDisplayList = new ArrayList<>();

    protected MassStorageSession(final MassStoragePlugin plugin, final UUID uuid) {
        this.plugin = plugin;
        this.uuid = uuid;
        final int size = plugin.getIndex().size();
        this.ids = new int[size];
        this.amounts = new int[size];
        this.autos = new boolean[size];
        this.favs = new int[size];
    }

    public void setup() {
        plugin.getDatabase().scheduleAsyncTask(() -> {
                setupNow();
                Bukkit.getScheduler().runTask(plugin, () -> {
                        enabled = true;
                        plugin.getDatabase().update(SQLStorable.class)
                            .set("updated", new Date())
                            .where(c -> c.eq("owner", uuid))
                            .async(null);
                    });
            });
    }

    public static MassStorageSession createAdminOnly(UUID uuid) {
        MassStorageSession result = new MassStorageSession(MassStoragePlugin.getInstance(), uuid);
        result.setupNow();
        return result;
    }

    public void setupNow() {
        playerRow = plugin.getDatabase().find(SQLPlayer.class).eq("uuid", uuid).findUnique();
        if (playerRow == null) {
            playerRow = new SQLPlayer(uuid);
            if (plugin.getDatabase().insert(playerRow) == 0) {
                throw new IllegalStateException("Insert failed: " + playerRow);
            }
        }
        List<SQLStorable> invalidRows = new ArrayList<>();
        for (SQLStorable row : plugin.getDatabase().find(SQLStorable.class).eq("owner", uuid).findList()) {
            StorableItem storable = plugin.getIndex().get(row);
            if (!storable.isValid()) {
                if (row.getAmount() == 0) {
                    invalidRows.add(row);
                } else {
                    plugin.getLogger().severe("Invalid row: " + row);
                }
                continue;
            }
            int index = storable.getIndex();
            ids[index] = row.getId();
            amounts[index] = row.getAmount();
            autos[index] = row.isAuto();
            favs[index] = row.getFavorite();
        }
        if (!invalidRows.isEmpty()) {
            int count = plugin.getDatabase().delete(invalidRows);
            plugin.getLogger().info("Deleted " + count + " invalid rows: " + uuid);
        }
        List<SQLStorable> newRows = new ArrayList<>();
        for (int index = 0; index < ids.length; index += 1) {
            if (ids[index] != 0) continue;
            newRows.add(new SQLStorable(uuid, plugin.getIndex().get(index)));
        }
        if (!newRows.isEmpty()) {
            plugin.getDatabase().insertIgnore(newRows);
            for (SQLStorable row : newRows) {
                if (row.getId() == null) {
                    throw new IllegalStateException("Insert failed: " + row);
                }
                StorableItem storable = plugin.getIndex().get(row);
                int index = storable.getIndex();
                ids[index] = row.getId();
            }
        }
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public List<FavoriteSet> getFavorites() {
        Map<FavoriteSlot, FavoriteSet> map = new EnumMap<>(FavoriteSlot.class);
        FavoriteSlot[] values = FavoriteSlot.values();
        for (int i = 0; i < favs.length; i += 1) {
            int raw = favs[i];
            if (raw == 0) continue;
            FavoriteSlot slot = values[raw - 1];
            FavoriteSet set = map.computeIfAbsent(slot, FavoriteSet::new);
            StorableItem storable = plugin.getIndex().get(i);
            set.storables.add(storable);
        }
        List<FavoriteSet> result = new ArrayList<>(map.size());
        result.addAll(map.values());
        return result;
    }

    public boolean insertAndSubtract(Inventory inventory, ItemInsertionCause cause, ItemInsertionCallback callback) {
        List<ItemStack> items = new ArrayList<>(inventory.getSize());
        for (ItemStack item : inventory) {
            if (item == null || item.getType().isAir()) continue;
            items.add(item);
        }
        return insertAndSubtract(items, cause, callback);
    }

    private boolean insertAndSubtractHelper(ItemStack item,
                                            ItemInsertionCause cause,
                                            List<ItemStack> rejects,
                                            Map<StorableItem, Integer> storedItems) {
        if (item == null || item.getType().isAir()) return false;
        if (Tag.SHULKER_BOXES.isTagged(item.getType())) {
            if (!cause.drainShulkerBoxes()) {
                rejects.add(item);
                return false;
            }
            item.editMeta(m -> {
                    if (m instanceof BlockStateMeta meta) {
                        if (meta.getBlockState() instanceof ShulkerBox shulkerBox) {
                            Inventory inventory = shulkerBox.getInventory();
                            for (ItemStack item2 : inventory) {
                                insertAndSubtractHelper(item2, cause, rejects, storedItems);
                            }
                            meta.setBlockState(shulkerBox);
                        }
                    }
                });
        }
        StorableItem storable = plugin.getIndex().get(item);
        if (!storable.canStore(item)) {
            rejects.add(item);
            return false;
        }
        int value = storedItems.getOrDefault(storable, 0);
        int amount = item.getAmount();
        storedItems.put(storable, value + amount);
        item.subtract(amount);
        return true;
    }

    public boolean insertAndSubtract(List<ItemStack> items, ItemInsertionCause cause, ItemInsertionCallback callback) {
        final List<ItemStack> rejects = new ArrayList<>();
        final Map<StorableItem, Integer> map = new IdentityHashMap<>();
        for (ItemStack item : items) {
            insertAndSubtractHelper(item, cause, rejects, map);
        }
        plugin.getDatabase().scheduleAsyncTask(() -> {
                for (Map.Entry<StorableItem, Integer> entry : map.entrySet()) {
                    insert(entry.getKey(), entry.getValue());
                }
                if (callback != null) {
                    ItemInsertionResult result = new ItemInsertionResult(cause, rejects, map);
                    Bukkit.getScheduler().runTask(plugin, () -> callback.accept(result));
                }
            });
        return !map.isEmpty();
    }

    /**
     * Insert an item.  The storable must be valid and the amount
     * positive!
     *
     * An insert should never fail, so if it happens after all, we
     * spam console.
     */
    public boolean insert(StorableItem storable, int amount) {
        final int rowId = ids[storable.getIndex()];
        final int result = plugin.getDatabase().update(SQLStorable.class)
            .add("amount", amount)
            .where(c -> c.eq("id", rowId))
            .sync();
        final boolean success = result != 0;
        if (!success) {
            plugin.getLogger().severe("Insert failed!"
                                      + " rowId=" + rowId
                                      + " result=" + result
                                      + " player=" + uuid
                                      + " item=" + amount + "x" + storable);
        } else {
            amounts[storable.getIndex()] += amount;
            Bukkit.getScheduler().runTask(plugin, () -> addStorableDisplay(storable, amount));
        }
        return success;
    }

    public void insertAsync(StorableItem storable, int amount, Consumer<Boolean> callback) {
        plugin.getDatabase().scheduleAsyncTask(() -> {
                boolean result = insert(storable, amount);
                if (callback != null) {
                    Bukkit.getScheduler().runTask(plugin, () -> callback.accept(result));
                }
            });
    }

    public void setAmount(StorableItem storable, int amount) {
        final int rowId = ids[storable.getIndex()];
        final int result = plugin.getDatabase().update(SQLStorable.class)
            .set("amount", amount)
            .where(c -> c.eq("id", rowId))
            .sync();
        amounts[storable.getIndex()] = amount;
    }

    public void fillContainer(Player player, Block block, Inventory currentInventory, StorableItem storable) {
        if (fillingContainer) return;
        final InventoryType inventoryType = currentInventory.getType();
        switch (inventoryType) {
        case BARREL: case CHEST: case DISPENSER: case DROPPER: case HOPPER:
            break;
        case SHULKER_BOX:
            if (storable.isShulkerBox()) {
                player.sendActionBar(text("Cannot put a shulker in a shulker!", RED));
                player.playSound(player.getLocation(), BLOCK_CHEST_LOCKED, 1.0f, 1.25f);
                return;
            }
            break;
        default:
            player.sendActionBar(text("This container cannot be filled!", RED));
            player.playSound(player.getLocation(), BLOCK_CHEST_LOCKED, 1.0f, 1.25f);
            return;
        }
        final int has = getAmount(storable);
        if (has == 0) {
            player.sendActionBar(join(noSeparators(), text("You are out of ", RED), storable.getIconName()));
            return;
        }
        final int amount = storable.fit(currentInventory, has, false);
        if (amount == 0) {
            player.sendActionBar(text("This container is full!", RED));
            player.playSound(player.getLocation(), BLOCK_CHEST_LOCKED, 1.0f, 1.25f);
            return;
        }
        BlockData blockData = block.getBlockData();
        fillingContainer = true;
        retrieveAsync(storable, amount, success -> {
                fillingContainer = false;
                if (!success) {
                    player.sendActionBar(join(noSeparators(), text("You are out of ", RED), storable.getIconName()));
                    player.playSound(player.getLocation(), BLOCK_CHEST_LOCKED, 1.0f, 1.25f);
                    return;
                }
                if (!block.getBlockData().equals(blockData)) {
                    player.sendActionBar(text("Someting went wrong!", RED));
                    insertAsync(storable, amount, null);
                    return;
                }
                final Inventory inventory = getAccessibleWorldContainer(player, block);
                final int stored;
                if (inventory == null) {
                    stored = 0;
                } else {
                    stored = storable.fit(inventory, amount, true);
                }
                if (stored < amount) {
                    insertAsync(storable, amount - stored, null);
                }
                if (stored == 0) {
                    player.sendActionBar(text("This container is full!", RED));
                    player.playSound(player.getLocation(), BLOCK_CHEST_LOCKED, 1.0f, 1.25f);
                } else {
                    player.sendMessage(join(noSeparators(),
                                            text("Filled the " + toCamelCase(inventoryType, " ") + " with ", GREEN),
                                            text(stored, WHITE), TIMES, storable.getIconName()));
                    player.playSound(player.getLocation(), BLOCK_ENDER_CHEST_OPEN, 0.5f, 2.0f);
                }
            });
    }

    /**
     * Retrieval is expected to fail when the backing storage does not
     * have enough amount in store.  Thus, we anticipate the fail
     * state without an error.
     */
    public boolean retrieve(StorableItem storable, int amount) {
        final int index = storable.getIndex();
        final int result = plugin.getDatabase().update(SQLStorable.class)
            .subtract("amount", amount)
            .where(c -> c
                   .eq("id", ids[index])
                   .gte("amount", amount))
            .sync();
        boolean success = result != 0;
        if (success) {
            amounts[index] -= amount;
            Bukkit.getScheduler().runTask(plugin, () -> addStorableDisplay(storable, -amount));
        }
        return success;
    }

    public void retrieveAsync(StorableItem storable, int amount, Consumer<Boolean> callback) {
        plugin.getDatabase().scheduleAsyncTask(() -> {
                boolean result = retrieve(storable, amount);
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(result));
            });
    }

    public int getAmount(StorableItem storable) {
        return amounts[storable.getIndex()];
    }

    public boolean getAutoPickup(StorableItem storable) {
        return autos[storable.getIndex()];
    }

    public void setAutoPickup(StorableItem storable, boolean value) {
        final int index = storable.getIndex();
        if (autos[index] == value) return;
        autos[index] = value;
        plugin.getDatabase().update(SQLStorable.class)
            .set("auto", value)
            .where(c -> c.eq("id", ids[index]))
            .async(null);
    }

    public FavoriteSlot getFavoriteSlot(StorableItem storable) {
        int raw = favs[storable.getIndex()];
        return raw == 0 ? null
            : FavoriteSlot.values()[raw - 1];
    }

    public void setFavoriteSlot(StorableItem storable, FavoriteSlot value) {
        final int index = storable.getIndex();
        final int raw = value == null ? 0 : value.ordinal() + 1;
        if (favs[index] == raw) return;
        favs[index] = raw;
        plugin.getDatabase().update(SQLStorable.class)
            .set("favorite", raw)
            .where(c -> c.eq("id", ids[index]))
            .async(null);
    }

    public void complete(List<String> result, String arg) {
        String lower = arg.toLowerCase();
        for (int i = 0; i < amounts.length; i += 1) {
            if (amounts[i] == 0) continue;
            StorableItem storable = plugin.getIndex().get(i);
            String lname = storable.getName().toLowerCase();
            if (lname.contains(lower)) {
                result.add(lname);
            }
        }

        // Autocomplete material tags
        // For block material tags
        for (Tag<Material> tag : Bukkit.getTags(Tag.REGISTRY_BLOCKS, Material.class)) {
            String name = tag.getKey().getKey().toLowerCase().replace('_', ' ');
            if (name.contains(lower)) {
                result.add(name);
            }
        }
        // For item material tags
        for (Tag<Material> tag : Bukkit.getTags(Tag.REGISTRY_ITEMS, Material.class)) {
            String name = tag.getKey().getKey().toLowerCase().replace('_', ' ');
            if (name.contains(lower)) {
                result.add(name);
            }
        }

        // Autocomplete categories
        for (StorableCategory cat : StorableCategory.values()) {
            String name = cat.getName().toLowerCase();
            if (name.contains(lower)) result.add(name);
        }
    }

    public List<StorableItem> allStorables() {
        List<StorableItem> result = new ArrayList<>();
        for (int i = 0; i < amounts.length; i += 1) {
            if (amounts[i] == 0) continue;
            StorableItem storable = plugin.getIndex().get(i);
            result.add(storable);
        }
        return result;
    }

    public List<StorableItem> storables(String arg) {
        String lower = arg.toLowerCase();
        List<StorableItem> result = new ArrayList<>();
        for (int i = 0; i < amounts.length; i += 1) {
            if (amounts[i] == 0) continue;
            StorableItem storable = plugin.getIndex().get(i);
            if (storable.getName().toLowerCase().contains(lower)) {
                result.add(storable);
                continue;
            }

            // Return material tag if arg is (part of) a material tag
            Material m = null;
            if (storable instanceof StorableBukkitItem s) m = s.getMaterial();
            if (storable instanceof StorableEnchantedBook s) m = s.getMaterial();
            if (storable instanceof StorablePotion s) m = s.getMaterial();
            if (m != null) {
                // For block material tags
                for (Tag<Material> tag : Bukkit.getTags(Tag.REGISTRY_BLOCKS, Material.class)) {
                    if (tag.getKey().getKey().toLowerCase().contains(lower) && tag.isTagged(m)) {
                        result.add(storable);
                    }
                }
                // For item material tags
                for (Tag<Material> tag : Bukkit.getTags(Tag.REGISTRY_ITEMS, Material.class)) {
                    if (tag.getKey().getKey().toLowerCase().contains(lower) && tag.isTagged(m)) {
                        result.add(storable);
                    }
                }
            }

            // Return category members if arg is (part of) a category
            for (StorableCategory cat : StorableCategory.values()) {
                if (cat.getName().toLowerCase().contains(lower) && cat.getStorables().contains(storable)) {
                    result.add(storable);
                    break;
                }
            }
        }
        return result;
    }

    public List<StorableItem> filter(List<StorableItem> in) {
        List<StorableItem> result = new ArrayList<>();
        for (StorableItem it : in) {
            if (amounts[it.getIndex()] > 0) {
                result.add(it);
            }
        }
        return result;
    }

    public int count(List<StorableItem> in) {
        int result = 0;
        for (StorableItem it : in) {
            result += amounts[it.getIndex()];
        }
        return result;
    }

    public MassStorageDialogue getDialogue() {
        if (dialogue == null) {
            dialogue = new MassStorageDialogue(plugin, this);
        }
        return dialogue;
    }

    public boolean isAssistEnabled() {
        return playerRow.isAuto();
    }

    public void setAssistEnabled(boolean value) {
        if (value == playerRow.isAuto()) return;
        playerRow.setAuto(value);
        plugin.getDatabase().updateAsync(playerRow, null, "auto");
    }

    public ItemSortOrder getItemSortOrder() {
        return ItemSortOrder.values()[playerRow.getSortOrder()];
    }

    public void setItemSortOrder(ItemSortOrder value) {
        if (value.ordinal() == playerRow.getSortOrder()) return;
        playerRow.setSortOrder(value.ordinal());
        plugin.getDatabase().updateAsync(playerRow, null, "sortOrder");
    }

    protected void stackHand(Player player, EquipmentSlot hand, Runnable callback) {
        if (stackingHand) return;
        final GameMode gameMode = player.getGameMode();
        if (gameMode != GameMode.SURVIVAL && gameMode != GameMode.ADVENTURE) return;
        PlayerInventory inventory = player.getInventory();
        int index = hand == EquipmentSlot.HAND ? player.getInventory().getHeldItemSlot() : 40;
        final ItemStack item = inventory.getItem(index);
        if (item == null || item.getType().isAir()) return;
        final StorableItem storable = plugin.getIndex().get(item);
        if (storable == null) return;
        if (!storable.canStack(item)) return;
        if (getAmount(storable) == 0) return;
        final int oldAmount = item.getAmount();
        final Material itemMaterial = item.getType();
        final Material insertMaterial = switch (itemMaterial) {
        case POTION -> Material.GLASS_BOTTLE;
        case MILK_BUCKET -> Material.BUCKET;
        case MUSHROOM_STEW, RABBIT_STEW, SUSPICIOUS_STEW, BEETROOT_SOUP -> Material.BOWL;
        default -> null;
        };
        stackingHand = true;
        Bukkit.getScheduler().runTask(plugin, () -> {
                stackingHand = false;
                if (!player.isOnline() || player.isDead() || player.getGameMode() != gameMode) return;
                final ItemStack item2 = player.getInventory().getItem(index);
                final int amount;
                if (item2 == null || item2.getType().isAir()) {
                    amount = Math.min(getAmount(storable), itemMaterial.getMaxStackSize());
                } else if (insertMaterial != null && item2.getType() == insertMaterial) {
                    // Remove empty bowl, bucket, or bottle
                    final StorableItem storable2 = plugin.getIndex().get(item2);
                    if (storable2 == null || !storable2.canStack(item2)) return;
                    insert(storable2, item2.getAmount());
                    player.getInventory().setItem(index, null);
                    amount = Math.min(getAmount(storable), itemMaterial.getMaxStackSize());
                } else {
                    if (!storable.canStack(item2)) return;
                    if (item2.getAmount() == oldAmount) return;
                    amount = Math.min(getAmount(storable), item.getMaxStackSize() - item2.getAmount());
                }
                if (amount <= 0) return;
                stackingHand = true;
                retrieveAsync(storable, amount, success -> {
                        stackingHand = false;
                        if (!success) return;
                        final ItemStack item3 = player.getInventory().getItem(index);
                        if (!player.isOnline() || player.isDead() || player.getGameMode() != gameMode) {
                            insertAsync(storable, amount, null);
                        } else if (item3 == null || item3.getType().isAir()) {
                            player.getInventory().setItem(index, storable.createItemStack(amount));
                        } else {
                            final int given;
                            if (!storable.canStack(item3)) {
                                given = 0;
                            } else {
                                given = Math.min(amount, itemMaterial.getMaxStackSize() - item3.getAmount());
                                item3.add(given);
                            }
                            if (given < amount) {
                                insertAsync(storable, amount - given, null);
                            }
                            callback.run();
                        }
                    });
            });
    }

    protected void onPlayerHud(PlayerHudEvent event) {
        if (action instanceof SessionWorldContainerAction sessionAction) {
            if (sessionAction instanceof SessionFillWorldContainer fill) {
                event.sidebar(PlayerHudPriority.HIGHEST,
                              List.of(join(noSeparators(), text("/ms ", YELLOW), text(tiny("Container fill"), AQUA)),
                                      join(noSeparators(), text(tiny("mode: "), AQUA), fill.getStorable().getDisplayName())));
            } else if (sessionAction instanceof SessionDrainWorldContainer drain) {
                event.sidebar(PlayerHudPriority.HIGHEST,
                              List.of(join(noSeparators(), text(tiny("/ms "), YELLOW), text(tiny("Container drain"), AQUA)),
                                      text(tiny("mode"), AQUA)));
            }
        }
        if (!storableDisplayList.isEmpty()) {
            final int maxLineLength = 16;
            List<Component> lines = new ArrayList<>();
            lines.add(text(tiny("mass storage"), YELLOW));
            List<Component> components = new ArrayList<>();
            int lineLength = 0;
            final long now = System.currentTimeMillis();
            for (Iterator<StorableDisplay> iter = storableDisplayList.iterator(); iter.hasNext();) {
                StorableDisplay storableDisplay = iter.next();
                final int amt = getAmount(storableDisplay.storable);
                final String amount = amt < 100
                    ? "" + amt
                    : "";
                final String num = storableDisplay.changedAmount >= 0
                    ? "+" + storableDisplay.changedAmount
                    : "" + storableDisplay.changedAmount;
                final Component component = join(noSeparators(),
                                                 storableDisplay.storable.getIcon(),
                                                 text(amount),
                                                 text(subscript(num), GRAY));
                final int length = 1 + amount.length() + (num.length() * 2) / 3;
                if (lineLength + (lineLength == 0 ? 0 : 1) + length >= maxLineLength && !components.isEmpty()) {
                    lines.add(join(separator(space()), components));
                    components.clear();
                    lineLength = 0;
                }
                components.add(component);
                lineLength += length;
                if (storableDisplay.timeout < now) {
                    iter.remove();
                }
            }
            if (!components.isEmpty()) {
                lines.add(join(separator(space()), components));
            }
            event.sidebar(PlayerHudPriority.LOW, lines);
        }
    }

    protected void addStorableDisplay(StorableItem storable, int amount) {
        final long now = System.currentTimeMillis();
        StorableDisplay storableDisplay = null;
        for (Iterator<StorableDisplay> iter = storableDisplayList.iterator(); iter.hasNext();) {
            StorableDisplay it = iter.next();
            if (it.storable == storable) {
                storableDisplay = it;
                break;
            }
            if (it.timeout < now) {
                iter.remove();
            }
        }
        if (storableDisplay == null) {
            storableDisplay = new StorableDisplay(storable);
            storableDisplayList.add(storableDisplay);
        }
        storableDisplay.changedAmount += amount;
        storableDisplay.timeout = now + 10_000L;
    }
}
