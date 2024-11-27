package com.cavetale.ms.storable;

import com.cavetale.ms.sql.SQLStorable;
import com.cavetale.mytems.Mytems;
import com.destroystokyo.paper.MaterialTags;
import io.papermc.paper.registry.RegistryKey;
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
import org.bukkit.potion.PotionType;
import static io.papermc.paper.registry.RegistryAccess.registryAccess;

public final class StorableItemIndex {
    private final UnstorableItem unstorableItem = new UnstorableItem();
    private final List<StorableItem> all = new ArrayList<>();
    private final Map<StorageType, Map<String, StorableItem>> sqlNameMap = new EnumMap<>(StorageType.class);
    protected final Map<Material, StorableBukkitItem> bukkitIndex = new EnumMap<>(Material.class);
    protected final Map<Mytems, StorableMytemsItem> mytemsIndex = new EnumMap<>(Mytems.class);
    private final Map<StorablePotion.Type, Map<PotionType, StorablePotion>> potionIndex = new EnumMap<>(StorablePotion.Type.class);
    private final Map<Enchantment, List<StorableEnchantedBook>> enchantedBookIndex = new IdentityHashMap<>();
    private static final Set<Material> MATERIAL_BLACKLIST = Set.of(new Material[] {
            Material.AIR,
            Material.BARRIER,
            Material.BEDROCK,
            Material.CHAIN_COMMAND_BLOCK,
            Material.COMMAND_BLOCK,
            Material.COMMAND_BLOCK_MINECART,
            Material.DEBUG_STICK,
            Material.ENCHANTED_BOOK,
            Material.FILLED_MAP,
            Material.GOAT_HORN,
            Material.JIGSAW,
            Material.KNOWLEDGE_BOOK,
            Material.LIGHT,
            Material.LINGERING_POTION,
            Material.POTION,
            Material.REPEATING_COMMAND_BLOCK,
            Material.SPAWNER,
            Material.SPLASH_POTION,
            Material.STRUCTURE_BLOCK,
            Material.STRUCTURE_VOID,
            Material.TIPPED_ARROW,
            Material.TRIAL_SPAWNER,
            Material.VAULT,
            Material.WRITTEN_BOOK,
        });

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
                StorablePotion storablePotion = new StorablePotion(type, potionType, all.size());
                potionIndex.computeIfAbsent(type, t -> new EnumMap<>(PotionType.class)).put(potionType, storablePotion);
                sqlNameMap.get(storablePotion.getStorageType()).put(storablePotion.getSqlName(), storablePotion);
                all.add(storablePotion);
            }
        }
        for (Enchantment enchantment : registryAccess().getRegistry(RegistryKey.ENCHANTMENT)) {
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
            StorablePotion storablePotion = potionIndex.get(storablePotionType).get(potionMeta.getBasePotionType());
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
