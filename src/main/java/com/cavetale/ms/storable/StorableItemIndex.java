package com.cavetale.ms.storable;

import com.cavetale.ms.sql.SQLStorable;
import com.cavetale.mytems.Mytems;
import com.destroystokyo.paper.MaterialTags;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

public final class StorableItemIndex {
    private final UnstorableItem unstorableItem = new UnstorableItem();
    private final List<StorableItem> all = new ArrayList<>();
    private final Map<StorageType, Map<String, StorableItem>> sqlNameMap = new EnumMap<>(StorageType.class);
    protected final Map<Material, StorableBukkitItem> bukkitIndex = new EnumMap<>(Material.class);
    protected final Map<Mytems, StorableMytemsItem> mytemsIndex = new EnumMap<>(Mytems.class);
    private final Map<StorablePotion.Type, Map<PotionType, List<StorablePotion>>> potionIndex = new EnumMap<>(StorablePotion.Type.class);
    private final Map<Enchantment, List<StorableEnchantedBook>> enchantedBookIndex = new IdentityHashMap<>();
    private static final Set<Material> MATERIAL_BLACKLIST = Set
        .of(Material.AIR,
            Material.BEDROCK,
            Material.BARRIER,
            Material.CHAIN_COMMAND_BLOCK,
            Material.COMMAND_BLOCK,
            Material.COMMAND_BLOCK_MINECART,
            Material.DEBUG_STICK,
            Material.ENCHANTED_BOOK,
            Material.END_PORTAL_FRAME,
            Material.FILLED_MAP,
            Material.KNOWLEDGE_BOOK,
            Material.LINGERING_POTION,
            Material.POTION,
            Material.REPEATING_COMMAND_BLOCK,
            Material.SPAWNER,
            Material.SPLASH_POTION,
            Material.STRUCTURE_BLOCK,
            Material.STRUCTURE_VOID,
            Material.TIPPED_ARROW,
            Material.WRITTEN_BOOK,
            Material.JIGSAW,
            Material.LIGHT,
            Material.SCULK_SENSOR,
            Material.BUNDLE);

    /**
     * Initialize this index with all storable items.
     */
    public void populate() {
        for (StorageType storageType : StorageType.values()) {
            sqlNameMap.put(storageType, new HashMap<>());
        }
        for (Material material : Material.values()) {
            if (material.isAir()) continue;
            if (!material.isItem()) continue;
            if (material.isLegacy()) continue;
            if (MATERIAL_BLACKLIST.contains(material)) continue;
            if (MaterialTags.SPAWN_EGGS.isTagged(material)) continue;
            StorableBukkitItem value = new StorableBukkitItem(material, all.size());
            bukkitIndex.put(material, value);
            sqlNameMap.get(StorageType.BUKKIT).put(value.getSqlName(), value);
            all.add(value);
        }
        for (Mytems mytems : Mytems.values()) {
            if (!mytems.getMytem().isMassStorable()) continue;
            if (!mytems.getMytem().isAvailableToPlayers()) continue;
            StorableMytemsItem value = new StorableMytemsItem(mytems, all.size());
            mytemsIndex.put(mytems, value);
            sqlNameMap.get(StorageType.MYTEMS).put(value.getSqlName(), value);
            all.add(value);
        }
        for (StorablePotion.Type type : StorablePotion.Type.values()) {
            for (PotionType potionType : PotionType.values()) {
                List<StorablePotion> potionList = new ArrayList<>(3);
                potionList.add(null);
                potionList.add(null);
                potionList.add(null);
                potionIndex.computeIfAbsent(type, t -> new EnumMap<>(PotionType.class)).put(potionType, potionList);
                for (int i = 0; i < 3; i += 1) {
                    final PotionData potionData;
                    switch (i) {
                    case 1:
                        if (!potionType.isExtendable()) continue;
                        potionData = new PotionData(potionType, true, false);
                        break;
                    case 2:
                        if (!potionType.isUpgradeable()) continue;
                        potionData = new PotionData(potionType, false, true);
                        break;
                    default: potionData = new PotionData(potionType, false, false);
                    }
                    StorablePotion value = new StorablePotion(type, potionData, all.size());
                    potionList.set(i, value);
                    sqlNameMap.get(value.getStorageType()).put(value.getSqlName(), value);
                    all.add(value);
                }
            }
        }
        for (Enchantment enchantment : Enchantment.values()) {
            if (enchantment.isCursed()) {
                enchantedBookIndex.put(enchantment, List.of());
                continue;
            }
            List<StorableEnchantedBook> list = new ArrayList<>(enchantment.getMaxLevel());
            enchantedBookIndex.put(enchantment, list);
            for (int level = 1; level <= enchantment.getMaxLevel(); level += 1) {
                StorableEnchantedBook value = new StorableEnchantedBook(enchantment, level, all.size());
                list.add(value);
                sqlNameMap.get(StorageType.ENCHANTED_BOOK).put(value.getSqlName(), value);
                all.add(value);
            }
        }
    }

    /**
     * Find a StorableItem which is either capable of storing the
     * given ItemStack, or not valid.
     */
    public @NonNull StorableItem get(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) return unstorableItem;
        // Mytems
        Mytems mytems = Mytems.forItem(itemStack);
        if (mytems != null) {
            StorableMytemsItem smi = mytemsIndex.get(mytems);
            if (smi != null && smi.canStore(itemStack)) return smi;
        }
        // Bukkit
        StorableBukkitItem sbi = bukkitIndex.get(itemStack.getType());
        if (sbi != null && sbi.canStore(itemStack)) return sbi;
        // Potion
        StorablePotion.Type storablePotionType = StorablePotion.Type.get(itemStack.getType());
        if (storablePotionType != null && itemStack.hasItemMeta() && itemStack.getItemMeta() instanceof PotionMeta potionMeta) {
            PotionData potionData = potionMeta.getBasePotionData();
            List<StorablePotion> list = potionIndex.get(storablePotionType).get(potionData.getType());
            StorablePotion storablePotion = list.get(potionData.isExtended() ? 1 : potionData.isUpgraded() ? 2 : 0);
            if (storablePotion != null && storablePotion.canStore(itemStack)) return storablePotion;
        }
        // Enchanted Book
        if (itemStack.getType() == Material.ENCHANTED_BOOK
            && itemStack.hasItemMeta()
            && itemStack.getItemMeta() instanceof EnchantmentStorageMeta meta
            && meta.getStoredEnchants().size() == 1) {
            Map.Entry<Enchantment, Integer> entry = meta.getStoredEnchants().entrySet().iterator().next();
            Enchantment enchantment = entry.getKey();
            int level = entry.getValue();
            List<StorableEnchantedBook> list = enchantedBookIndex.get(enchantment);
            if (level > 0 && level <= list.size()) {
                StorableEnchantedBook value = list.get(level - 1);
                if (value.canStore(itemStack)) return value;
            }
        }
        return unstorableItem;
    }

    public @NonNull StorableItem get(SQLStorable row) {
        return get(row.getStorageType(), row.getName());
    }

    public @NonNull StorableItem get(StorageType type, String sqlName) {
        return sqlNameMap.get(type).getOrDefault(sqlName, unstorableItem);
    }

    public @NonNull StorableItem get(int index) {
        return all.get(index);
    }

    public List<StorableItem> all() {
        return all;
    }

    public List<StorableItem> of(StorageType storageType) {
        return List.copyOf(sqlNameMap.get(storageType).values());
    }

    public int size() {
        return all.size();
    }

    public List<String> allNames() {
        List<String> result = new ArrayList<>(all.size());
        for (StorableItem it : all) {
            result.add(it.getName());
        }
        return result;
    }

    public List<String> allSqlNames() {
        List<String> result = new ArrayList<>(all.size());
        for (StorableItem it : all) {
            result.add(it.getSqlName());
        }
        return result;
    }
}
