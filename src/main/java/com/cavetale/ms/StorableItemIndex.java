package com.cavetale.ms;

import com.cavetale.mytems.Mytems;
import com.destroystokyo.paper.MaterialTags;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class StorableItemIndex {
    protected final UnstorableItem unstorableItem = new UnstorableItem();
    protected final List<StorableItem> all = new ArrayList<>();

    protected final Map<Material, StorableBukkitItem> bukkitIndex = new EnumMap<>(Material.class);
    protected final Map<String, StorableBukkitItem> bukkitSqlNameMap = new HashMap<>();

    protected final Map<Mytems, StorableMytemsItem> mytemsIndex = new EnumMap<>(Mytems.class);
    protected final Map<String, StorableMytemsItem> mytemsSqlNameMap = new HashMap<>();

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
            Material.BUNDLE,
            Material.BUDDING_AMETHYST);

    protected void populate() {
        for (Material material : Material.values()) {
            if (material.isAir()) continue;
            if (!material.isItem()) continue;
            if (material.isLegacy()) continue;
            if (MATERIAL_BLACKLIST.contains(material)) continue;
            if (MaterialTags.SPAWN_EGGS.isTagged(material)) continue;
            StorableBukkitItem value = new StorableBukkitItem(material, all.size());
            bukkitIndex.put(material, value);
            bukkitSqlNameMap.put(value.getSqlName(), value);
            all.add(value);
        }
        for (Mytems mytems : Mytems.values()) {
            if (!mytems.getMytem().isMassStorable()) continue;
            if (!mytems.getMytem().isAvailableToPlayers()) continue;
            StorableMytemsItem value = new StorableMytemsItem(mytems, all.size());
            mytemsIndex.put(mytems, value);
            mytemsSqlNameMap.put(value.getSqlName(), value);
            all.add(value);
        }
    }

    /**
     * Find a StorableItem which is either capable of storing the
     * given ItemStack, or not valid.
     */
    public @NonNull StorableItem get(ItemStack itemStack) {
        Mytems mytems = Mytems.forItem(itemStack);
        if (mytems != null) {
            StorableMytemsItem smi = mytemsIndex.get(mytems);
            if (smi != null && smi.canStore(itemStack)) return smi;
        }
        StorableBukkitItem sbi = bukkitIndex.get(itemStack.getType());
        if (sbi != null && sbi.canStore(itemStack)) return sbi;
        return unstorableItem;
    }

    public @NonNull StorableItem get(SQLMassStorage row) {
        switch (row.getStorageType()) {
        case BUKKIT:
            StorableBukkitItem bukkit = bukkitSqlNameMap.get(row.getName());
            return bukkit != null ? bukkit : unstorableItem;
        case MYTEMS:
            StorableMytemsItem mytems = mytemsSqlNameMap.get(row.getName());
            return mytems != null ? mytems : unstorableItem;
        case INVALID:
        default:
            return unstorableItem;
        }
    }

    public @NonNull StorableItem get(int index) {
        return all.get(index);
    }

    public List<StorableItem> all() {
        return all;
    }

    public int size() {
        return all.size();
    }
}
