package com.cavetale.ms.storable;

import com.cavetale.ms.MassStoragePlugin;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.MytemsCategory;
import com.cavetale.mytems.MytemsTag;
import com.cavetale.mytems.util.Text;
import com.destroystokyo.paper.MaterialTags;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.CreativeCategory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public enum StorableCategory implements StorableSet {
    ALL(Mytems.EARTH::createIcon) {
        @Override protected boolean isSpecial() {
            return true;
        }
    },
    BUILDING_BLOCKS(() -> new ItemStack(Material.BRICKS)) {
        @Override protected Material[] getMaterials() {
            return new Material[] {
                Material.AMETHYST_BLOCK,
                Material.ANDESITE,
                Material.BAMBOO_BLOCK,
                Material.BAMBOO_MOSAIC,
                Material.BAMBOO_PLANKS,
                Material.BASALT,
                Material.BEEHIVE,
                Material.BEE_NEST,
                Material.BLACKSTONE,
                Material.BLAST_FURNACE,
                Material.BLUE_ICE,
                Material.BONE_BLOCK,
                Material.BOOKSHELF,
                Material.BOOKSHELF,
                Material.BRICKS,
                Material.BUDDING_AMETHYST,
                Material.CALCITE,
                Material.CARTOGRAPHY_TABLE,
                Material.CARVED_PUMPKIN,
                Material.CHISELED_BOOKSHELF,
                Material.CHISELED_DEEPSLATE,
                Material.CHISELED_NETHER_BRICKS,
                Material.CHISELED_POLISHED_BLACKSTONE,
                Material.CHISELED_QUARTZ_BLOCK,
                Material.CHISELED_RED_SANDSTONE,
                Material.CHISELED_RESIN_BRICKS,
                Material.CHISELED_SANDSTONE,
                Material.CHISELED_STONE_BRICKS,
                Material.CHISELED_TUFF,
                Material.CHISELED_TUFF_BRICKS,
                Material.CLAY,
                Material.COAL_BLOCK,
                Material.COARSE_DIRT,
                Material.COBBLED_DEEPSLATE,
                Material.COBBLESTONE,
                Material.CRACKED_DEEPSLATE_BRICKS,
                Material.CRACKED_DEEPSLATE_TILES,
                Material.CRACKED_NETHER_BRICKS,
                Material.CRACKED_POLISHED_BLACKSTONE_BRICKS,
                Material.CRACKED_STONE_BRICKS,
                Material.CRAFTER,
                Material.CRAFTING_TABLE,
                Material.CREAKING_HEART,
                Material.CRIMSON_HYPHAE,
                Material.CRIMSON_NYLIUM,
                Material.CRIMSON_STEM,
                Material.CRYING_OBSIDIAN,
                Material.CUT_COPPER,
                Material.CUT_RED_SANDSTONE,
                Material.CUT_SANDSTONE,
                Material.DARK_PRISMARINE,
                Material.DEEPSLATE,
                Material.DEEPSLATE_BRICKS,
                Material.DEEPSLATE_TILES,
                Material.DIAMOND_BLOCK,
                Material.DIORITE,
                Material.DIRT,
                Material.DIRT_PATH,
                Material.DRIPSTONE_BLOCK,
                Material.EMERALD_BLOCK,
                Material.END_STONE,
                Material.END_STONE_BRICKS,
                Material.FARMLAND,
                Material.FLETCHING_TABLE,
                Material.FURNACE,
                Material.GILDED_BLACKSTONE,
                Material.GOLD_BLOCK,
                Material.GRANITE,
                Material.GRASS_BLOCK,
                Material.GRAVEL,
                Material.HAY_BLOCK,
                Material.HONEYCOMB_BLOCK,
                Material.HONEY_BLOCK,
                Material.ICE,
                Material.INFESTED_CHISELED_STONE_BRICKS,
                Material.INFESTED_COBBLESTONE,
                Material.INFESTED_CRACKED_STONE_BRICKS,
                Material.INFESTED_DEEPSLATE,
                Material.INFESTED_MOSSY_STONE_BRICKS,
                Material.INFESTED_STONE,
                Material.INFESTED_STONE_BRICKS,
                Material.IRON_BLOCK,
                Material.JACK_O_LANTERN,
                Material.JUKEBOX,
                Material.LAPIS_BLOCK,
                Material.LODESTONE,
                Material.LOOM,
                Material.MAGMA_BLOCK,
                Material.MANGROVE_ROOTS,
                Material.MELON,
                Material.MOSSY_COBBLESTONE,
                Material.MOSSY_STONE_BRICKS,
                Material.MOSS_BLOCK,
                Material.MUD,
                Material.MUDDY_MANGROVE_ROOTS,
                Material.MUD_BRICKS,
                Material.MUSHROOM_STEM,
                Material.MYCELIUM,
                Material.NETHERITE_BLOCK,
                Material.NETHERRACK,
                Material.NETHER_BRICKS,
                Material.NETHER_WART_BLOCK,
                Material.NOTE_BLOCK,
                Material.OBSIDIAN,
                Material.OCHRE_FROGLIGHT,
                Material.PACKED_ICE,
                Material.PACKED_MUD,
                Material.PALE_MOSS_BLOCK,
                Material.PEARLESCENT_FROGLIGHT,
                Material.PODZOL,
                Material.POLISHED_ANDESITE,
                Material.POLISHED_BASALT,
                Material.POLISHED_BLACKSTONE,
                Material.POLISHED_BLACKSTONE_BRICKS,
                Material.POLISHED_DEEPSLATE,
                Material.POLISHED_DIORITE,
                Material.POLISHED_GRANITE,
                Material.POLISHED_TUFF,
                Material.PRISMARINE,
                Material.PRISMARINE_BRICKS,
                Material.PUMPKIN,
                Material.PURPUR_BLOCK,
                Material.PURPUR_PILLAR,
                Material.QUARTZ_BLOCK,
                Material.QUARTZ_BRICKS,
                Material.QUARTZ_PILLAR,
                Material.RAW_COPPER_BLOCK,
                Material.RAW_GOLD_BLOCK,
                Material.RAW_IRON_BLOCK,
                Material.REDSTONE_BLOCK,
                Material.RED_NETHER_BRICKS,
                Material.RED_SAND,
                Material.RED_SANDSTONE,
                Material.REINFORCED_DEEPSLATE,
                Material.RESIN_BLOCK,
                Material.RESIN_BRICKS,
                Material.RESPAWN_ANCHOR,
                Material.ROOTED_DIRT,
                Material.SAND,
                Material.SANDSTONE,
                Material.SCULK,
                Material.SCULK_CATALYST,
                Material.SLIME_BLOCK,
                Material.SMITHING_TABLE,
                Material.SMOKER,
                Material.SMOOTH_BASALT,
                Material.SMOOTH_QUARTZ,
                Material.SMOOTH_RED_SANDSTONE,
                Material.SMOOTH_SANDSTONE,
                Material.SMOOTH_STONE,
                Material.SNOW_BLOCK,
                Material.SOUL_SAND,
                Material.SOUL_SOIL,
                Material.SPONGE,
                Material.STONE,
                Material.STONE_BRICKS,
                Material.STRIPPED_BAMBOO_BLOCK,
                Material.STRIPPED_CRIMSON_HYPHAE,
                Material.STRIPPED_CRIMSON_STEM,
                Material.STRIPPED_WARPED_HYPHAE,
                Material.STRIPPED_WARPED_STEM,
                Material.SUSPICIOUS_GRAVEL,
                Material.SUSPICIOUS_SAND,
                Material.TERRACOTTA,
                Material.TINTED_GLASS,
                Material.TUFF,
                Material.TUFF_BRICKS,
                Material.VERDANT_FROGLIGHT,
                Material.WARPED_HYPHAE,
                Material.WARPED_NYLIUM,
                Material.WARPED_STEM,
                Material.WARPED_WART_BLOCK,
                Material.WET_SPONGE,
            };
        }
        @Override protected List<Tag<Material>> getTags() {
            return List.of(Tag.PLANKS,
                           Tag.LOGS,
                           Tag.LEAVES,
                           Tag.WOOL,
                           MaterialTags.GLASS,
                           MaterialTags.GLAZED_TERRACOTTA,
                           MaterialTags.STAINED_TERRACOTTA,
                           MaterialTags.TERRACOTTA,
                           MaterialTags.CONCRETES,
                           MaterialTags.CONCRETE_POWDER,
                           MaterialTags.CORAL_BLOCKS,
                           MaterialTags.COPPER_BLOCKS,
                           MaterialTags.EXPOSED_COPPER_BLOCKS,
                           MaterialTags.OXIDIZED_COPPER_BLOCKS,
                           MaterialTags.UNAFFECTED_COPPER_BLOCKS,
                           MaterialTags.UNWAXED_COPPER_BLOCKS,
                           MaterialTags.WAXED_COPPER_BLOCKS,
                           MaterialTags.WEATHERED_COPPER_BLOCKS,
                           MaterialTags.CUT_COPPER_BLOCKS,
                           MaterialTags.FULL_COPPER_BLOCKS);
        }
        @Override protected Mytems[] getMytems() {
            return new Mytems[] {
                Mytems.NETHERITE_PARITY_TABLE,
            };
        }
    },
    SLABS(() -> new ItemStack(Material.COBBLESTONE_SLAB)) {
        @Override protected List<Tag<Material>> getTags() {
            return List.of(Tag.SLABS);
        }
    },
    STAIRS(() -> new ItemStack(Material.COBBLESTONE_STAIRS)) {
        @Override protected List<Tag<Material>> getTags() {
            return List.of(Tag.STAIRS);
        }
    },
    WALLS(() -> new ItemStack(Material.COBBLESTONE_WALL)) {
        @Override protected List<Tag<Material>> getTags() {
            return List.of(Tag.FENCES,
                           Tag.FENCE_GATES,
                           Tag.WALLS,
                           MaterialTags.GLASS_PANES,
                           Tag.DOORS,
                           Tag.TRAPDOORS);
        }
        @Override protected Material[] getMaterials() {
            return new Material[] {
                Material.IRON_BARS,
                Material.SCAFFOLDING,
            };
        }
    },
    LIGHTING(() -> new ItemStack(Material.CAMPFIRE)) {
        @Override protected Material[] getMaterials() {
            return new Material[] {
                Material.BEACON,
                Material.CONDUIT,
                Material.END_ROD,
                Material.GLOWSTONE,
                Material.GLOW_BERRIES,
                Material.GLOW_INK_SAC,
                Material.GLOW_LICHEN,
                Material.HEAVY_CORE,
                Material.JACK_O_LANTERN,
                Material.MAGMA_BLOCK,
                Material.OCHRE_FROGLIGHT,
                Material.PEARLESCENT_FROGLIGHT,
                Material.REDSTONE_LAMP,
                Material.REDSTONE_TORCH,
                Material.SEA_LANTERN,
                Material.SHROOMLIGHT,
                Material.SOUL_TORCH,
                Material.TORCH,
                Material.VERDANT_FROGLIGHT,

                // Copper Bulbs
                Material.COPPER_BULB,
                Material.EXPOSED_COPPER_BULB,
                Material.OXIDIZED_COPPER_BULB,
                Material.WAXED_COPPER_BULB,
                Material.WAXED_EXPOSED_COPPER_BULB,
                Material.WAXED_OXIDIZED_COPPER_BULB,
                Material.WAXED_WEATHERED_COPPER_BULB,
                Material.WEATHERED_COPPER_BULB,
            };
        }
        @Override protected List<Tag<Material>> getTags() {
            return List.of(Tag.CANDLES,
                           Tag.CAMPFIRES,
                           MaterialTags.LANTERNS);
        }
    },
    DECORATION(() -> new ItemStack(Material.PAINTING)) {
        @Override protected Material[] getMaterials() {
            return new Material[] {
                Material.ARMOR_STAND,
                Material.BELL,
                Material.BREWING_STAND,
                Material.CAULDRON,
                Material.CHAIN,
                Material.COBWEB,
                Material.COMPOSTER,
                Material.DECORATED_POT,
                Material.DRAGON_EGG,
                Material.DRAGON_HEAD,
                Material.DRIED_GHAST,
                Material.ENCHANTING_TABLE,
                Material.END_CRYSTAL,
                Material.END_PORTAL_FRAME,
                Material.FLOWER_POT,
                Material.FROGSPAWN,
                Material.GLOW_ITEM_FRAME,
                Material.GRINDSTONE,
                Material.ITEM_FRAME,
                Material.LADDER,
                Material.LECTERN,
                Material.LIGHTNING_ROD,
                Material.PAINTING,
                Material.POINTED_DRIPSTONE,
                Material.SCULK_VEIN,
                Material.SNIFFER_EGG,
                Material.SNOW,
                Material.STONECUTTER,
                Material.TURTLE_EGG,
            };
        }
        @Override protected List<Tag<Material>> getTags() {
            return List.of(MaterialTags.SKULLS,
                           Tag.ANVIL);
        }
        @Override protected MytemsTag[] getMytemsTags() {
            return new MytemsTag[] {
                MytemsTag.FURNITURE,
            };
        }
    },
    MULTICOLOR(() -> new ItemStack(Material.PINK_GLAZED_TERRACOTTA)) {
        @Override protected List<Tag<Material>> getTags() {
            return List.of(MaterialTags.CONCRETES,
                           MaterialTags.CONCRETE_POWDER,
                           MaterialTags.DYES,
                           MaterialTags.GLAZED_TERRACOTTA,
                           MaterialTags.STAINED_GLASS,
                           MaterialTags.STAINED_GLASS_PANES,
                           MaterialTags.STAINED_TERRACOTTA,
                           MaterialTags.TERRACOTTA,
                           Tag.BANNERS,
                           Tag.BEDS,
                           Tag.CANDLES,
                           Tag.SHULKER_BOXES,
                           Tag.TERRACOTTA,
                           Tag.WOOL,
                           Tag.WOOL_CARPETS);
        }
    },
    SIGNS(() -> new ItemStack(Material.OAK_SIGN)) {
        @Override protected List<Tag<Material>> getTags() {
            return List.of(Tag.ALL_SIGNS,
                           // Tag.ALL_HANGING_SIGNS, // Throws exception
                           Tag.SIGNS);
        }
    },
    VEGETATION(() -> new ItemStack(Material.FLOWERING_AZALEA)) {
        @Override protected Material[] getMaterials() {
            return new Material[] {
                Material.AZALEA,
                Material.BAMBOO,
                Material.BEETROOT_SEEDS,
                Material.BIG_DRIPLEAF,
                Material.BROWN_MUSHROOM,
                Material.BROWN_MUSHROOM_BLOCK,
                Material.BUSH,
                Material.CACTUS,
                Material.CHORUS_FLOWER,
                Material.CHORUS_FRUIT,
                Material.CHORUS_PLANT,
                Material.COCOA_BEANS,
                Material.CRIMSON_FUNGUS,
                Material.CRIMSON_ROOTS,
                Material.DEAD_BUSH,
                Material.FERN,
                Material.FIREFLY_BUSH,
                Material.FLOWERING_AZALEA,
                Material.HANGING_ROOTS,
                Material.LARGE_FERN,
                Material.LEAF_LITTER,
                Material.LILY_PAD,
                Material.MELON_SEEDS,
                Material.MOSS_CARPET,
                Material.NETHER_SPROUTS,
                Material.NETHER_WART,
                Material.PALE_HANGING_MOSS,
                Material.PALE_MOSS_CARPET,
                Material.PITCHER_POD,
                Material.PUMPKIN_SEEDS,
                Material.RED_MUSHROOM,
                Material.RED_MUSHROOM_BLOCK,
                Material.SEAGRASS,
                Material.SEA_PICKLE,
                Material.SHORT_DRY_GRASS,
                Material.SHORT_GRASS,
                Material.SMALL_DRIPLEAF,
                Material.SPORE_BLOSSOM,
                Material.SUGAR_CANE,
                Material.TALL_DRY_GRASS,
                Material.TALL_GRASS,
                Material.TORCHFLOWER_SEEDS,
                Material.TWISTING_VINES,
                Material.VINE,
                Material.WARPED_FUNGUS,
                Material.WARPED_ROOTS,
                Material.WEEPING_VINES,
                Material.WHEAT_SEEDS,
            };
        }
        @Override protected List<Tag<Material>> getTags() {
            return List.of(Tag.SAPLINGS);
        }
        @Override protected MytemsCategory[] getMytemsCategories() {
            return new MytemsCategory[] {
                MytemsCategory.TREE_SEED,
            };
        }
    },
    FLOWERS(() -> new ItemStack(Material.PEONY)) {
        @Override protected List<Tag<Material>> getTags() {
            return List.of(Tag.FLOWERS);
        }
    },
    CORALS(() -> new ItemStack(Material.TUBE_CORAL)) {
        @Override protected List<Tag<Material>> getTags() {
            return List.of(MaterialTags.CORAL,
                           MaterialTags.CORAL_FANS,
                           MaterialTags.CORAL_BLOCKS);
        }
    },
    MINING(() -> new ItemStack(Material.DIAMOND)) {
        @Override protected Material[] getMaterials() {
            return new Material[] {
                Material.ANCIENT_DEBRIS,
                Material.CHARCOAL,
                Material.COAL,
                Material.COPPER_INGOT,
                Material.DIAMOND,
                Material.EMERALD,
                Material.GOLD_INGOT,
                Material.GOLD_NUGGET,
                Material.IRON_INGOT,
                Material.IRON_NUGGET,
                Material.LAPIS_LAZULI,
                Material.NETHERITE_INGOT,
                Material.NETHERITE_SCRAP,
                Material.QUARTZ,
                Material.REDSTONE,
            };
        }
        @Override protected List<Tag<Material>> getTags() {
            return List.of(Tag.COAL_ORES,
                           Tag.REDSTONE_ORES,
                           Tag.LAPIS_ORES,
                           Tag.COPPER_ORES,
                           Tag.IRON_ORES,
                           Tag.GOLD_ORES,
                           Tag.EMERALD_ORES,
                           Tag.DIAMOND_ORES,
                           MaterialTags.ORES,
                           MaterialTags.RAW_ORES,
                           MaterialTags.DEEPSLATE_ORES);
        }
    },
    REDSTONE(() -> new ItemStack(Material.REDSTONE)) {
        @Override protected List<Tag<Material>> getTags() {
            return List.of(Tag.BUTTONS,
                           Tag.PRESSURE_PLATES);
        }
        @Override protected Material[] getMaterials() {
            return new Material[] {
                Material.ACTIVATOR_RAIL,
                Material.CALIBRATED_SCULK_SENSOR,
                Material.COMPARATOR,
                Material.CRAFTER,
                Material.DAYLIGHT_DETECTOR,
                Material.DETECTOR_RAIL,
                Material.DISPENSER,
                Material.DROPPER,
                Material.HOPPER,
                Material.LEVER,
                Material.OBSERVER,
                Material.PISTON,
                Material.POWERED_RAIL,
                Material.REDSTONE,
                Material.REDSTONE_BLOCK,
                Material.REDSTONE_LAMP,
                Material.REDSTONE_TORCH,
                Material.REPEATER,
                Material.SCULK_SENSOR,
                Material.SCULK_SHRIEKER,
                Material.STICKY_PISTON,
                Material.TARGET,
                Material.TNT,
                Material.TRAPPED_CHEST,
                Material.TRIPWIRE_HOOK,

                // Copper Bulbs
                Material.COPPER_BULB,
                Material.EXPOSED_COPPER_BULB,
                Material.OXIDIZED_COPPER_BULB,
                Material.WAXED_COPPER_BULB,
                Material.WAXED_EXPOSED_COPPER_BULB,
                Material.WAXED_OXIDIZED_COPPER_BULB,
                Material.WAXED_WEATHERED_COPPER_BULB,
                Material.WEATHERED_COPPER_BULB,
            };
        }
        @Override protected CreativeCategory getCreativeCategory() {
            return CreativeCategory.REDSTONE;
        }
    },
    TRANSPORTATION(() -> new ItemStack(Material.OAK_BOAT)) {
        @Override protected Material[] getMaterials() {
            return new Material[] {
                Material.MINECART,
                Material.SADDLE,
                Material.ACACIA_BOAT,
                Material.BIRCH_BOAT,
                Material.DARK_OAK_BOAT,
                Material.JUNGLE_BOAT,
                Material.OAK_BOAT,
                Material.SPRUCE_BOAT,
                Material.TNT_MINECART,
                Material.HOPPER_MINECART,
                Material.FURNACE_MINECART,
                Material.CHEST_MINECART,
                Material.CARROT_ON_A_STICK,
                Material.WARPED_FUNGUS_ON_A_STICK,
                Material.ELYTRA,
            };
        }
        @Override protected List<Tag<Material>> getTags() {
            return List.of(Tag.ITEMS_BOATS,
                           Tag.ITEMS_CHEST_BOATS,
                           Tag.RAILS);
        }
        @Override protected CreativeCategory getCreativeCategory() {
            return CreativeCategory.TRANSPORTATION;
        }
    },
    FOOD(() -> new ItemStack(Material.APPLE)) {
        @Override protected Material[] getMaterials() {
            return new Material[] {
                Material.APPLE,
                Material.BAKED_POTATO,
                Material.BEEF,
                Material.BEETROOT,
                Material.BEETROOT_SOUP,
                Material.BOWL,
                Material.BREAD,
                Material.CAKE,
                Material.CARROT,
                Material.CHICKEN,
                Material.COD,
                Material.COOKED_BEEF,
                Material.COOKED_CHICKEN,
                Material.COOKED_COD,
                Material.COOKED_MUTTON,
                Material.COOKED_PORKCHOP,
                Material.COOKED_RABBIT,
                Material.COOKED_SALMON,
                Material.COOKIE,
                Material.DRIED_KELP,
                Material.DRIED_KELP_BLOCK,
                Material.ENCHANTED_GOLDEN_APPLE,
                Material.GOLDEN_APPLE,
                Material.GOLDEN_CARROT,
                Material.KELP,
                Material.MELON,
                Material.MELON_SLICE,
                Material.MUSHROOM_STEW,
                Material.MUTTON,
                Material.POISONOUS_POTATO,
                Material.PORKCHOP,
                Material.POTATO,
                Material.PUMPKIN,
                Material.PUMPKIN_PIE,
                Material.RABBIT,
                Material.RABBIT_STEW,
                Material.ROTTEN_FLESH,
                Material.SALMON,
                Material.SPIDER_EYE,
                Material.SUSPICIOUS_STEW,
                Material.SWEET_BERRIES,
                Material.TROPICAL_FISH,
                Material.HONEY_BOTTLE,
            };
        }
        @Override protected MytemsCategory[] getMytemsCategories() {
            return new MytemsCategory[] {
                MytemsCategory.FOOD,
                MytemsCategory.DRINK,
            };
        }
        @Override public CreativeCategory getCreativeCategory() {
            return CreativeCategory.FOOD;
        }
    },
    UTILITIES(() -> new ItemStack(Material.CLOCK)) {
        @Override protected Material[] getMaterials() {
            return new Material[] {
                Material.AMETHYST_CLUSTER,
                Material.AMETHYST_SHARD,
                Material.ARMADILLO_SCUTE,
                Material.BLAZE_ROD,
                Material.BLUE_EGG,
                Material.BONE,
                Material.BONE_MEAL,
                Material.BOOK,
                Material.BREEZE_ROD,
                Material.BRICK,
                Material.BROWN_EGG,
                Material.CLAY_BALL,
                Material.CLOCK,
                Material.COMPASS,
                Material.DISC_FRAGMENT_5,
                Material.ECHO_SHARD,
                Material.EGG,
                Material.ENDER_EYE,
                Material.ENDER_PEARL,
                Material.EXPERIENCE_BOTTLE,
                Material.FEATHER,
                Material.FIREWORK_ROCKET,
                Material.FIREWORK_STAR,
                Material.FIRE_CHARGE,
                Material.FISHING_ROD,
                Material.FLINT,
                Material.FLINT_AND_STEEL,
                Material.GLASS_BOTTLE,
                Material.GLOWSTONE_DUST,
                Material.GOAT_HORN,
                Material.GUNPOWDER,
                Material.HEART_OF_THE_SEA,
                Material.HONEYCOMB,
                Material.INK_SAC,
                Material.LARGE_AMETHYST_BUD,
                Material.LEAD,
                Material.LEATHER,
                Material.MAP,
                Material.MEDIUM_AMETHYST_BUD,
                Material.NAME_TAG,
                Material.NAUTILUS_SHELL,
                Material.NETHER_BRICK,
                Material.NETHER_STAR,
                Material.OMINOUS_BOTTLE,
                Material.OMINOUS_TRIAL_KEY,
                Material.PAPER,
                Material.PHANTOM_MEMBRANE,
                Material.POPPED_CHORUS_FRUIT,
                Material.PRISMARINE_CRYSTALS,
                Material.PRISMARINE_SHARD,
                Material.RABBIT_HIDE,
                Material.RECOVERY_COMPASS,
                Material.RESIN_BRICK,
                Material.RESIN_CLUMP,
                Material.SHEARS,
                Material.SHULKER_SHELL,
                Material.SLIME_BALL,
                Material.SMALL_AMETHYST_BUD,
                Material.SNOWBALL,
                Material.SPYGLASS,
                Material.STICK,
                Material.STRING,
                Material.TOTEM_OF_UNDYING,
                Material.TRIAL_KEY,
                Material.TURTLE_SCUTE,
                Material.WHEAT,
                Material.WIND_CHARGE,
                Material.WRITABLE_BOOK,
            };
        }
        @Override protected List<Tag<Material>> getTags() {
            return List.of(MaterialTags.BUCKETS,
                           MaterialTags.FISH_BUCKETS);
        }
    },
    BREWING(() -> new ItemStack(Material.BREWING_STAND)) {
        @Override protected Material[] getMaterials() {
            return new Material[] {
                Material.NETHER_WART,
                Material.SUGAR,
                Material.REDSTONE,
                Material.GLOWSTONE_DUST,
                Material.BLAZE_POWDER,
                Material.DRAGON_BREATH,
                Material.SPIDER_EYE,
                Material.FERMENTED_SPIDER_EYE,
                Material.GHAST_TEAR,
                Material.GOLDEN_CARROT,
                Material.MAGMA_CREAM,
                Material.RABBIT_FOOT,
                Material.SLIME_BALL,
                Material.GLISTERING_MELON_SLICE,
                Material.GUNPOWDER,
                Material.PUFFERFISH,
                Material.PHANTOM_MEMBRANE,
                Material.TURTLE_HELMET,
            };
        }
        @Override protected MytemsCategory[] getMytemsCategories() {
            return new MytemsCategory[] {
                MytemsCategory.POTIONS,
            };
        }
        @Override protected CreativeCategory getCreativeCategory() {
            return CreativeCategory.BREWING;
        }
    },
    POTIONS(() -> {
            ItemStack icon = new ItemStack(Material.POTION);
            icon.editMeta(meta -> {
                    ((PotionMeta) meta).setBasePotionType(PotionType.FIRE_RESISTANCE);
                });
            return icon;
        }) {
        @Override protected List<StorageType> getStorageTypes() {
            return List.of(StorageType.POTION,
                           StorageType.SPLASH_POTION,
                           StorageType.LINGERING_POTION,
                           StorageType.TIPPED_ARROW);
        }
    },
    ENCHANTED_BOOKS(() -> {
            ItemStack icon = new ItemStack(Material.ENCHANTED_BOOK);
            icon.editMeta(meta -> {
                    ((EnchantmentStorageMeta) meta).addStoredEnchant(Enchantment.UNBREAKING, 1, false);
                });
            return icon;
        }) {
        @Override protected List<StorageType> getStorageTypes() {
            return List.of(StorageType.ENCHANTED_BOOK);
        }
    },
    STORAGE(() -> new ItemStack(Material.CHEST)) {
        @Override protected List<Tag<Material>> getTags() {
            return List.of(Tag.SHULKER_BOXES,
                           Tag.ITEMS_BUNDLES);
        }
        @Override protected Material[] getMaterials() {
            return new Material[] {
                Material.BARREL,
                Material.CHEST,
                Material.DISPENSER,
                Material.DROPPER,
                Material.ENDER_CHEST,
                Material.HOPPER,
                Material.TRAPPED_CHEST,
            };
        }
        @Override protected Mytems[] getMytems() {
            return new Mytems[] {
                Mytems.BOSS_CHEST,
            };
        }
    },
    ARMOR(() -> new ItemStack(Material.IRON_HELMET)) {
        @Override protected Material[] getMaterials() {
            return new Material[] {
                Material.CHAINMAIL_BOOTS,
                Material.CHAINMAIL_CHESTPLATE,
                Material.CHAINMAIL_HELMET,
                Material.CHAINMAIL_LEGGINGS,
                Material.DIAMOND_BOOTS,
                Material.DIAMOND_CHESTPLATE,
                Material.DIAMOND_HELMET,
                Material.DIAMOND_HORSE_ARMOR,
                Material.DIAMOND_LEGGINGS,
                Material.GOLDEN_BOOTS,
                Material.GOLDEN_CHESTPLATE,
                Material.GOLDEN_HELMET,
                Material.GOLDEN_HORSE_ARMOR,
                Material.GOLDEN_LEGGINGS,
                Material.IRON_BOOTS,
                Material.IRON_CHESTPLATE,
                Material.IRON_HELMET,
                Material.IRON_HORSE_ARMOR,
                Material.IRON_LEGGINGS,
                Material.LEATHER_BOOTS,
                Material.LEATHER_CHESTPLATE,
                Material.LEATHER_HELMET,
                Material.LEATHER_HORSE_ARMOR,
                Material.LEATHER_LEGGINGS,
                Material.NETHERITE_BOOTS,
                Material.NETHERITE_CHESTPLATE,
                Material.NETHERITE_HELMET,
                Material.NETHERITE_LEGGINGS,
                Material.TURTLE_HELMET,
                Material.WOLF_ARMOR,
            };
        }
        @Override protected List<Tag<Material>> getTags() {
            return List.of(Tag.ITEMS_HARNESSES);
        }
        @Override protected MytemsCategory[] getMytemsCategories() {
            return new MytemsCategory[] {
                MytemsCategory.SNEAKERS,
            };
        }
    },
    TOOLS(() -> new ItemStack(Material.IRON_PICKAXE)) {
        @Override protected List<Tag<Material>> getTags() {
            return List.of(MaterialTags.AXES,
                           MaterialTags.HOES,
                           MaterialTags.PICKAXES,
                           MaterialTags.SHOVELS);
        }
        @Override protected Material[] getMaterials() {
            return new Material[] {
                Material.DIAMOND_AXE,
                Material.DIAMOND_HOE,
                Material.DIAMOND_PICKAXE,
                Material.DIAMOND_SHOVEL,
                Material.GOLDEN_AXE,
                Material.GOLDEN_HOE,
                Material.GOLDEN_PICKAXE,
                Material.GOLDEN_SHOVEL,
                Material.IRON_AXE,
                Material.IRON_HOE,
                Material.IRON_PICKAXE,
                Material.IRON_SHOVEL,
                Material.NETHERITE_AXE,
                Material.NETHERITE_HOE,
                Material.NETHERITE_PICKAXE,
                Material.NETHERITE_SHOVEL,
                Material.STONE_AXE,
                Material.STONE_HOE,
                Material.STONE_PICKAXE,
                Material.STONE_SHOVEL,
                Material.WOODEN_AXE,
                Material.WOODEN_HOE,
                Material.WOODEN_PICKAXE,
                Material.WOODEN_SHOVEL,
                Material.BRUSH,
            };
        }
        @Override protected MytemsCategory[] getMytemsCategories() {
            return new MytemsCategory[] {
                MytemsCategory.UTILITY,
                MytemsCategory.MOB_CATCHERS,
                MytemsCategory.DIE,
                MytemsCategory.PAINTBRUSH,
                MytemsCategory.FRIENDS,
                MytemsCategory.HOOKSHOT,
            };
        }
        @Override protected MytemsTag[] getMytemsTags() {
            return new MytemsTag[] {
                MytemsTag.UTILITY,
            };
        }
    },
    COMBAT(() -> new ItemStack(Material.GOLDEN_SWORD)) {
        @Override protected List<Tag<Material>> getTags() {
            return List.of(MaterialTags.SWORDS,
                           MaterialTags.BOWS,
                           MaterialTags.ARROWS);
        }
        @Override protected Material[] getMaterials() {
            return new Material[] {
                Material.BOW,
                Material.CROSSBOW,
                Material.DIAMOND_SWORD,
                Material.GOLDEN_SWORD,
                Material.IRON_SWORD,
                Material.MACE,
                Material.NETHERITE_SWORD,
                Material.SHIELD,
                Material.STONE_SWORD,
                Material.TRIDENT,
                Material.WOODEN_SWORD,
            };
        }
        @Override protected MytemsTag[] getMytemsTags() {
            return new MytemsTag[] {
                MytemsTag.COMBAT,
            };
        }
    },
    INSTRUMENTS(Mytems.ANGELIC_HARP::createIcon) {
        @Override protected MytemsTag[] getMytemsTags() {
            return new MytemsTag[] {
                MytemsTag.MUSIC_ALL,
            };
        }
    },
    DISCS(() -> new ItemStack(Material.MUSIC_DISC_LAVA_CHICKEN)) {
        @Override protected List<Tag<Material>> getTags() {
            return List.of(MaterialTags.MUSIC_DISCS);
        }
    },
    GARDENING(Mytems.GOLDEN_SCYTHE::createIcon) {
        @Override protected MytemsCategory[] getMytemsCategories() {
            return new MytemsCategory[] {
                MytemsCategory.GARDENING,
                MytemsCategory.SEEDS,
            };
        }
    },
    COLLECTIBLES(Mytems.VOTE_CANDY::createIcon) {
        @Override protected MytemsCategory[] getMytemsCategories() {
            return new MytemsCategory[] {
                MytemsCategory.COLLECTIBLES,
                MytemsCategory.VOTE,
                MytemsCategory.TROPHY,
                MytemsCategory.TETRIS,
                MytemsCategory.CHESS,
            };
        }
        @Override protected MytemsTag[] getMytemsTags() {
            return new MytemsTag[] {
                MytemsTag.COLLECTIBLES,
            };
        }
    },
    CURRENCY(Mytems.RUBY::createIcon) {
        @Override protected MytemsCategory[] getMytemsCategories() {
            return new MytemsCategory[] {
                MytemsCategory.CURRENCY,
                MytemsCategory.ARMOR_PART,
                MytemsCategory.COIN,
            };
        }
    },
    ITEM_SETS(Mytems.DR_ACULA_STAFF::createIcon) {
        @Override protected MytemsTag[] getMytemsTags() {
            return new MytemsTag[] {
                MytemsTag.ITEM_SETS,
            };
        }
    },
    HOLIDAYS(Mytems.EASTER_EGG::createIcon) {
        @Override protected MytemsTag[] getMytemsTags() {
            return new MytemsTag[] {
                MytemsTag.HOLIDAYS,
            };
        }
        @Override protected MytemsCategory[] getMytemsCategories() {
            return new MytemsCategory[] {
                MytemsCategory.MAYPOLE,
            };
        }
    },
    COSTUMES(Mytems.KOBOLD_HEAD::createIcon) {
        @Override protected MytemsTag[] getMytemsTags() {
            return new MytemsTag[] {
                MytemsTag.COSTUME,
            };
        }
    },
    COMPONENTS(() -> new ItemStack(Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE)) {
        @Override protected Material[] getMaterials() {
            return new Material[] {
                // Banner Patterns
                Material.CREEPER_BANNER_PATTERN,
                Material.GLOBE_BANNER_PATTERN,
                Material.FLOWER_BANNER_PATTERN,
                Material.SKULL_BANNER_PATTERN,
                Material.PIGLIN_BANNER_PATTERN,
                Material.MOJANG_BANNER_PATTERN,
                Material.GUSTER_BANNER_PATTERN,
                Material.FLOW_BANNER_PATTERN,
                Material.BORDURE_INDENTED_BANNER_PATTERN,
                Material.FIELD_MASONED_BANNER_PATTERN,
                // Pottery Sherds
                Material.ANGLER_POTTERY_SHERD,
                Material.ARCHER_POTTERY_SHERD,
                Material.ARMS_UP_POTTERY_SHERD,
                Material.BLADE_POTTERY_SHERD,
                Material.BREWER_POTTERY_SHERD,
                Material.BURN_POTTERY_SHERD,
                Material.DANGER_POTTERY_SHERD,
                Material.EXPLORER_POTTERY_SHERD,
                Material.FLOW_POTTERY_SHERD,
                Material.FRIEND_POTTERY_SHERD,
                Material.GUSTER_POTTERY_SHERD,
                Material.HEARTBREAK_POTTERY_SHERD,
                Material.HEART_POTTERY_SHERD,
                Material.HOWL_POTTERY_SHERD,
                Material.MINER_POTTERY_SHERD,
                Material.MOURNER_POTTERY_SHERD,
                Material.PLENTY_POTTERY_SHERD,
                Material.PRIZE_POTTERY_SHERD,
                Material.SCRAPE_POTTERY_SHERD,
                Material.SHEAF_POTTERY_SHERD,
                Material.SHELTER_POTTERY_SHERD,
                Material.SKULL_POTTERY_SHERD,
                Material.SNORT_POTTERY_SHERD,
                // Smithing Templates
                Material.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE,
                Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE,
                Material.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE,
                Material.EYE_ARMOR_TRIM_SMITHING_TEMPLATE,
                Material.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE,
                Material.HOST_ARMOR_TRIM_SMITHING_TEMPLATE,
                Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE,
                Material.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE,
                Material.RIB_ARMOR_TRIM_SMITHING_TEMPLATE,
                Material.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE,
                Material.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE,
                Material.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE,
                Material.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE,
                Material.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE,
                Material.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE,
                Material.VEX_ARMOR_TRIM_SMITHING_TEMPLATE,
                Material.WARD_ARMOR_TRIM_SMITHING_TEMPLATE,
                Material.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE,
                Material.WILD_ARMOR_TRIM_SMITHING_TEMPLATE,
            };
        }
    },
    MISC(() -> new ItemStack(Material.EXPERIENCE_BOTTLE)) {
        @Override protected boolean isSpecial() {
            return true;
        }
    };

    private final Supplier<ItemStack> iconSupplier;
    @Getter private final List<StorableItem> storables = new ArrayList<>();
    @Getter private final String name;
    @Getter private final Component title;

    StorableCategory(final Supplier<ItemStack> iconSupplier) {
        this.iconSupplier = iconSupplier;
        this.name = Text.toCamelCase(this, " ");
        this.title = text(name, (isSpecial() ? YELLOW : WHITE));
    }

    /**
     * This must be called after the StorableItemIndex has been
     * populated.
     */
    public static void initialize(MassStoragePlugin plugin) {
        final Map<StorableItem, Boolean> unusedSet = new IdentityHashMap<>();
        for (StorableItem it : plugin.getIndex().all()) {
            unusedSet.put(it, true);
        }
        for (StorableCategory it : values()) {
            if (it == MISC || it == ALL) continue;
            final Map<StorableItem, Boolean> storableSet = new IdentityHashMap<>();
            for (Material material : it.getMaterials()) {
                StorableItem storable = plugin.getIndex().bukkitIndex.get(material);
                if (storable != null) storableSet.put(storable, true);
            }
            for (Tag<Material> tag : it.getTags()) {
                for (Material material : tag.getValues()) {
                    StorableItem storable = plugin.getIndex().bukkitIndex.get(material);
                    if (storable != null) storableSet.put(storable, true);
                }
            }
            for (Mytems mytems : it.getMytems()) {
                StorableItem storable = plugin.getIndex().mytemsIndex.get(mytems);
                if (storable != null) storableSet.put(storable, true);
            }
            for (MytemsTag tag : it.getMytemsTags()) {
                for (Mytems mytems : tag.getValues()) {
                    StorableItem storable = plugin.getIndex().mytemsIndex.get(mytems);
                    if (storable != null) storableSet.put(storable, true);
                }
            }
            for (MytemsCategory cat : it.getMytemsCategories()) {
                for (Mytems mytems : MytemsTag.of(cat).getValues()) {
                    StorableItem storable = plugin.getIndex().mytemsIndex.get(mytems);
                    if (storable != null) storableSet.put(storable, true);
                }
            }
            CreativeCategory creativeCategory = it.getCreativeCategory();
            if (creativeCategory != null) {
                for (Material material : Material.values()) {
                    if (material.isItem() && material.getCreativeCategory() == creativeCategory) {
                        StorableItem storable = plugin.getIndex().bukkitIndex.get(material);
                        if (storable != null) storableSet.put(storable, true);
                    }
                }
            }
            for (StorageType storageType : it.getStorageTypes()) {
                for (StorableItem storable : plugin.getIndex().of(storageType)) {
                    storableSet.put(storable, true);
                }
            }
            it.storables.addAll(storableSet.keySet());
            unusedSet.keySet().removeAll(it.storables);
        }
        MISC.storables.clear();
        MISC.storables.addAll(unusedSet.keySet());
        ALL.storables.addAll(plugin.getIndex().all());
    }

    protected static void clear() {
        for (StorableCategory it : values()) {
            it.storables.clear();
        }
    }

    protected Material[] getMaterials() {
        return new Material[0];
    }

    protected List<Tag<Material>> getTags() {
        return List.of();
    }

    protected Mytems[] getMytems() {
        return new Mytems[0];
    }

    protected MytemsCategory[] getMytemsCategories() {
        return new MytemsCategory[0];
    }

    protected MytemsTag[] getMytemsTags() {
        return new MytemsTag[0];
    }

    protected CreativeCategory getCreativeCategory() {
        return null;
    }

    protected List<StorageType> getStorageTypes() {
        return List.of();
    }

    protected boolean isSpecial() {
        return false;
    }

    @Override
    public ItemStack getIcon() {
        return iconSupplier.get();
    }

    private static NamespacedKey keyOf(String name) {
        return new NamespacedKey(MassStoragePlugin.getInstance(), name);
    }
}
