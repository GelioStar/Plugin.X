package server.variplex.Auction.SQL;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import server.variplex.Auction.Tools.ID_Gen;
import server.variplex.Auction.Tools.ItemFactory;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

import static server.variplex.Auction.Tools.ItemFactory.itemToString;

public class ShowPiece {
    private static final EnumSet<Material> REDSTONE_CATEGORY = EnumSet.of( Material.REDSTONE, Material.REDSTONE_TORCH, Material.REDSTONE_BLOCK, Material.REPEATER, Material.COMPARATOR, Material.PISTON, Material.STICKY_PISTON, Material.SLIME_BLOCK, Material.HONEY_BLOCK, Material.OBSERVER, Material.HOPPER, Material.DISPENSER, Material.DROPPER, Material.LECTERN, Material.TARGET, Material.LEVER, Material.DAYLIGHT_DETECTOR, Material.TRIPWIRE_HOOK, Material.TRAPPED_CHEST, Material.TNT, Material.REDSTONE_LAMP, Material.NOTE_BLOCK, Material.STONE_BUTTON, Material.POLISHED_BLACKSTONE_BUTTON, Material.OAK_BUTTON, Material.SPRUCE_BUTTON, Material.BIRCH_BUTTON, Material.JUNGLE_BUTTON, Material.ACACIA_BUTTON, Material.DARK_OAK_BUTTON, Material.CRIMSON_BUTTON, Material.WARPED_BUTTON, Material.STONE_PRESSURE_PLATE, Material.POLISHED_BLACKSTONE_PRESSURE_PLATE, Material.LIGHT_WEIGHTED_PRESSURE_PLATE, Material.HEAVY_WEIGHTED_PRESSURE_PLATE, Material.OAK_PRESSURE_PLATE, Material.SPRUCE_PRESSURE_PLATE, Material.BIRCH_PRESSURE_PLATE, Material.JUNGLE_PRESSURE_PLATE, Material.ACACIA_PRESSURE_PLATE, Material.DARK_OAK_PRESSURE_PLATE, Material.CRIMSON_PRESSURE_PLATE, Material.WARPED_PRESSURE_PLATE, Material.IRON_DOOR, Material.OAK_DOOR, Material.SPRUCE_DOOR, Material.BIRCH_DOOR, Material.JUNGLE_DOOR, Material.ACACIA_DOOR, Material.DARK_OAK_DOOR, Material.CRIMSON_DOOR, Material.WARPED_DOOR, Material.IRON_TRAPDOOR, Material.OAK_TRAPDOOR, Material.SPRUCE_TRAPDOOR, Material.BIRCH_TRAPDOOR, Material.JUNGLE_TRAPDOOR, Material.ACACIA_TRAPDOOR, Material.DARK_OAK_TRAPDOOR, Material.CRIMSON_TRAPDOOR, Material.WARPED_TRAPDOOR, Material.OAK_FENCE_GATE, Material.SPRUCE_FENCE_GATE, Material.BIRCH_FENCE_GATE, Material.JUNGLE_FENCE_GATE, Material.ACACIA_FENCE_GATE, Material.DARK_OAK_FENCE_GATE, Material.CRIMSON_FENCE_GATE, Material.WARPED_FENCE_GATE );
    private static final EnumSet<Material> TRANSPORTATION_CATEGORY = EnumSet.of( Material.POWERED_RAIL, Material.DETECTOR_RAIL, Material.RAIL, Material.SADDLE, Material.MINECART, Material.CHEST_MINECART, Material.FURNACE_MINECART, Material.TNT_MINECART, Material.HOPPER_MINECART, Material.CARROT_ON_A_STICK, Material.WARPED_FUNGUS_ON_A_STICK, Material.ELYTRA, Material.OAK_BOAT, Material.SPRUCE_BOAT, Material.BIRCH_BOAT, Material.JUNGLE_BOAT, Material.ACACIA_BOAT, Material.DARK_OAK_BOAT );
    private static final EnumSet<Material> DECORATION_BLOCKS_CATEGORY = EnumSet.of( Material.OAK_SAPLING, Material.SPRUCE_SAPLING, Material.BIRCH_SAPLING, Material.JUNGLE_SAPLING, Material.ACACIA_SAPLING, Material.DARK_OAK_SAPLING, Material.OAK_LEAVES, Material.SPRUCE_LEAVES, Material.BIRCH_LEAVES, Material.JUNGLE_LEAVES, Material.ACACIA_LEAVES, Material.DARK_OAK_LEAVES, Material.COBWEB, Material.GRASS, Material.FERN, Material.DEAD_BUSH, Material.SEAGRASS, Material.SEA_PICKLE, Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID, Material.ALLIUM, Material.AZURE_BLUET, Material.RED_TULIP, Material.ORANGE_TULIP, Material.WHITE_TULIP, Material.PINK_TULIP, Material.OXEYE_DAISY, Material.CORNFLOWER, Material.LILY_OF_THE_VALLEY, Material.WITHER_ROSE, Material.BROWN_MUSHROOM, Material.RED_MUSHROOM, Material.CRIMSON_FUNGUS, Material.WARPED_FUNGUS, Material.CRIMSON_ROOTS, Material.WARPED_ROOTS, Material.NETHER_SPROUTS, Material.WEEPING_VINES, Material.TWISTING_VINES, Material.SUGAR_CANE, Material.KELP, Material.BAMBOO, Material.TORCH, Material.END_ROD, Material.CHORUS_FLOWER, Material.CHEST, Material.CRAFTING_TABLE, Material.FARMLAND, Material.FURNACE, Material.LADDER, Material.SNOW, Material.CACTUS, Material.JUKEBOX, Material.OAK_FENCE, Material.SPRUCE_FENCE, Material.BIRCH_FENCE, Material.JUNGLE_FENCE, Material.ACACIA_FENCE, Material.DARK_OAK_FENCE, Material.CRIMSON_FENCE, Material.WARPED_FENCE, Material.SOUL_TORCH, Material.BROWN_MUSHROOM_BLOCK, Material.RED_MUSHROOM_BLOCK, Material.MUSHROOM_STEM, Material.IRON_BARS, Material.CHAIN, Material.GLASS_PANE, Material.VINE, Material.LILY_PAD, Material.NETHER_BRICK_FENCE, Material.ENCHANTING_TABLE, Material.COBBLESTONE_WALL, Material.MOSSY_COBBLESTONE_WALL, Material.BRICK_WALL, Material.PRISMARINE_WALL, Material.RED_SANDSTONE_WALL, Material.MOSSY_STONE_BRICK_WALL, Material.GRANITE_WALL, Material.STONE_BRICK_WALL, Material.NETHER_BRICK_WALL, Material.ANDESITE_WALL, Material.RED_NETHER_BRICK_WALL, Material.SANDSTONE_WALL, Material.END_STONE_BRICK_WALL, Material.DIORITE_WALL, Material.BLACKSTONE_WALL, Material.POLISHED_BLACKSTONE_WALL, Material.POLISHED_BLACKSTONE_BRICK_WALL, Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL, Material.WHITE_CARPET, Material.ORANGE_CARPET, Material.MAGENTA_CARPET, Material.LIGHT_BLUE_CARPET, Material.YELLOW_CARPET, Material.LIME_CARPET, Material.PINK_CARPET, Material.GRAY_CARPET, Material.LIGHT_GRAY_CARPET, Material.CYAN_CARPET, Material.PURPLE_CARPET, Material.BLUE_CARPET, Material.BROWN_CARPET, Material.GREEN_CARPET, Material.RED_CARPET, Material.BLACK_CARPET, Material.DIRT_PATH, Material.SUNFLOWER, Material.LILAC, Material.ROSE_BUSH, Material.PEONY, Material.TALL_GRASS, Material.LARGE_FERN, Material.WHITE_STAINED_GLASS_PANE, Material.ORANGE_STAINED_GLASS_PANE, Material.MAGENTA_STAINED_GLASS_PANE, Material.LIGHT_BLUE_STAINED_GLASS_PANE, Material.YELLOW_STAINED_GLASS_PANE, Material.LIME_STAINED_GLASS_PANE, Material.PINK_STAINED_GLASS_PANE, Material.GRAY_STAINED_GLASS_PANE, Material.LIGHT_GRAY_STAINED_GLASS_PANE, Material.CYAN_STAINED_GLASS_PANE, Material.PURPLE_STAINED_GLASS_PANE, Material.BLUE_STAINED_GLASS_PANE, Material.BROWN_STAINED_GLASS_PANE, Material.GREEN_STAINED_GLASS_PANE, Material.RED_STAINED_GLASS_PANE, Material.BLACK_STAINED_GLASS_PANE, Material.SHULKER_BOX, Material.WHITE_SHULKER_BOX, Material.ORANGE_SHULKER_BOX, Material.MAGENTA_SHULKER_BOX, Material.LIGHT_BLUE_SHULKER_BOX, Material.YELLOW_SHULKER_BOX, Material.LIME_SHULKER_BOX, Material.PINK_SHULKER_BOX, Material.GRAY_SHULKER_BOX, Material.LIGHT_GRAY_SHULKER_BOX, Material.CYAN_SHULKER_BOX, Material.PURPLE_SHULKER_BOX, Material.BLUE_SHULKER_BOX, Material.BROWN_SHULKER_BOX, Material.GREEN_SHULKER_BOX, Material.RED_SHULKER_BOX, Material.BLACK_SHULKER_BOX, Material.WHITE_GLAZED_TERRACOTTA, Material.ORANGE_GLAZED_TERRACOTTA, Material.MAGENTA_GLAZED_TERRACOTTA, Material.LIGHT_BLUE_GLAZED_TERRACOTTA, Material.YELLOW_GLAZED_TERRACOTTA, Material.LIME_GLAZED_TERRACOTTA, Material.PINK_GLAZED_TERRACOTTA, Material.GRAY_GLAZED_TERRACOTTA, Material.LIGHT_GRAY_GLAZED_TERRACOTTA, Material.CYAN_GLAZED_TERRACOTTA, Material.PURPLE_GLAZED_TERRACOTTA, Material.BLUE_GLAZED_TERRACOTTA, Material.BROWN_GLAZED_TERRACOTTA, Material.GREEN_GLAZED_TERRACOTTA, Material.RED_GLAZED_TERRACOTTA, Material.BLACK_GLAZED_TERRACOTTA, Material.TUBE_CORAL, Material.BRAIN_CORAL, Material.BUBBLE_CORAL, Material.FIRE_CORAL, Material.HORN_CORAL, Material.DEAD_TUBE_CORAL, Material.DEAD_BRAIN_CORAL, Material.DEAD_BUBBLE_CORAL, Material.DEAD_FIRE_CORAL, Material.DEAD_HORN_CORAL, Material.TUBE_CORAL_FAN, Material.BRAIN_CORAL_FAN, Material.BUBBLE_CORAL_FAN, Material.FIRE_CORAL_FAN, Material.HORN_CORAL_FAN, Material.DEAD_TUBE_CORAL_FAN, Material.DEAD_BRAIN_CORAL_FAN, Material.DEAD_BUBBLE_CORAL_FAN, Material.DEAD_FIRE_CORAL_FAN, Material.DEAD_HORN_CORAL_FAN, Material.SCAFFOLDING, Material.PAINTING, Material.OAK_SIGN, Material.SPRUCE_SIGN, Material.BIRCH_SIGN, Material.JUNGLE_SIGN, Material.ACACIA_SIGN, Material.DARK_OAK_SIGN, Material.CRIMSON_SIGN, Material.WARPED_SIGN, Material.WHITE_BED, Material.ORANGE_BED, Material.MAGENTA_BED, Material.LIGHT_BLUE_BED, Material.YELLOW_BED, Material.LIME_BED, Material.PINK_BED, Material.GRAY_BED, Material.LIGHT_GRAY_BED, Material.CYAN_BED, Material.PURPLE_BED, Material.BLUE_BED, Material.BROWN_BED, Material.GREEN_BED, Material.RED_BED, Material.BLACK_BED, Material.ITEM_FRAME, Material.FLOWER_POT, Material.SKELETON_SKULL, Material.WITHER_SKELETON_SKULL, Material.ZOMBIE_HEAD, Material.CREEPER_HEAD, Material.DRAGON_HEAD, Material.ARMOR_STAND, Material.WHITE_BANNER, Material.ORANGE_BANNER, Material.MAGENTA_BANNER, Material.LIGHT_BLUE_BANNER, Material.YELLOW_BANNER, Material.LIME_BANNER, Material.PINK_BANNER, Material.GRAY_BANNER, Material.LIGHT_GRAY_BANNER, Material.CYAN_BANNER, Material.PURPLE_BANNER, Material.BLUE_BANNER, Material.BROWN_BANNER, Material.GREEN_BANNER, Material.RED_BANNER, Material.BLACK_BANNER, Material.LOOM, Material.COMPOSTER, Material.BARREL, Material.SMOKER, Material.BLAST_FURNACE, Material.CARTOGRAPHY_TABLE, Material.FLETCHING_TABLE, Material.GRINDSTONE, Material.SMITHING_TABLE, Material.BELL, Material.LANTERN, Material.SOUL_LANTERN, Material.CAMPFIRE, Material.SOUL_CAMPFIRE, Material.SHROOMLIGHT, Material.BEE_NEST, Material.BEEHIVE, Material.HONEYCOMB_BLOCK, Material.LODESTONE );
    private static final EnumSet<Material> COMBAT_CATEGORY = EnumSet.of( Material.TURTLE_HELMET, Material.BOW, Material.ARROW, Material.WOODEN_SWORD, Material.STONE_SWORD, Material.GOLDEN_SWORD, Material.IRON_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS, Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS, Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS, Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS, Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS, Material.SPECTRAL_ARROW, Material.TIPPED_ARROW, Material.SHIELD, Material.TOTEM_OF_UNDYING, Material.TRIDENT, Material.CROSSBOW );
    private static final EnumSet<Material> TOOLS_CATEGORY = EnumSet.of( Material.FLINT_AND_STEEL, Material.WOODEN_SHOVEL, Material.WOODEN_PICKAXE, Material.WOODEN_AXE, Material.WOODEN_HOE, Material.STONE_SHOVEL, Material.STONE_PICKAXE, Material.STONE_AXE, Material.STONE_HOE, Material.GOLDEN_SHOVEL, Material.GOLDEN_PICKAXE, Material.GOLDEN_AXE, Material.GOLDEN_HOE, Material.IRON_SHOVEL, Material.IRON_PICKAXE, Material.IRON_AXE, Material.IRON_HOE, Material.DIAMOND_SHOVEL, Material.DIAMOND_PICKAXE, Material.DIAMOND_AXE, Material.DIAMOND_HOE, Material.NETHERITE_SHOVEL, Material.NETHERITE_PICKAXE, Material.NETHERITE_AXE, Material.NETHERITE_HOE, Material.COMPASS, Material.FISHING_ROD, Material.CLOCK, Material.SHEARS, Material.LEAD, Material.NAME_TAG );
    private static final EnumSet<Material> MISCELLANEOUS_CATEGORY = EnumSet.of( Material.BEACON, Material.TURTLE_EGG, Material.CONDUIT, Material.SCUTE, Material.COAL, Material.CHARCOAL, Material.DIAMOND, Material.EMERALD, Material.LAPIS_LAZULI, Material.QUARTZ, Material.IRON_INGOT, Material.GOLD_INGOT, Material.NETHERITE_INGOT, Material.NETHERITE_SCRAP, Material.STICK, Material.BOWL, Material.STRING, Material.FEATHER, Material.GUNPOWDER, Material.WHEAT_SEEDS, Material.WHEAT, Material.FLINT, Material.BUCKET, Material.WATER_BUCKET, Material.LAVA_BUCKET, Material.SNOWBALL, Material.LEATHER, Material.MILK_BUCKET, Material.PUFFERFISH_BUCKET, Material.SALMON_BUCKET, Material.COD_BUCKET, Material.TROPICAL_FISH_BUCKET, Material.BRICK, Material.CLAY_BALL, Material.PAPER, Material.BOOK, Material.SLIME_BALL, Material.EGG, Material.GLOWSTONE_DUST, Material.INK_SAC, Material.COCOA_BEANS, Material.WHITE_DYE, Material.ORANGE_DYE, Material.MAGENTA_DYE, Material.LIGHT_BLUE_DYE, Material.YELLOW_DYE, Material.LIME_DYE, Material.PINK_DYE, Material.GRAY_DYE, Material.LIGHT_GRAY_DYE, Material.CYAN_DYE, Material.PURPLE_DYE, Material.BLUE_DYE, Material.BROWN_DYE, Material.GREEN_DYE, Material.RED_DYE, Material.BLACK_DYE, Material.BONE_MEAL, Material.SUGAR, Material.PUMPKIN_SEEDS, Material.MELON_SEEDS, Material.ENDER_PEARL, Material.BLAZE_ROD, Material.GOLD_NUGGET, Material.NETHER_WART, Material.ENDER_EYE, Material.BAT_SPAWN_EGG, Material.BEE_SPAWN_EGG, Material.BLAZE_SPAWN_EGG, Material.CAT_SPAWN_EGG, Material.CAVE_SPIDER_SPAWN_EGG, Material.CHICKEN_SPAWN_EGG, Material.COD_SPAWN_EGG, Material.COW_SPAWN_EGG, Material.CREEPER_SPAWN_EGG, Material.DOLPHIN_SPAWN_EGG, Material.DONKEY_SPAWN_EGG, Material.DROWNED_SPAWN_EGG, Material.ELDER_GUARDIAN_SPAWN_EGG, Material.ENDERMAN_SPAWN_EGG, Material.ENDERMITE_SPAWN_EGG, Material.EVOKER_SPAWN_EGG, Material.FOX_SPAWN_EGG, Material.GHAST_SPAWN_EGG, Material.GUARDIAN_SPAWN_EGG, Material.HOGLIN_SPAWN_EGG, Material.HORSE_SPAWN_EGG, Material.HUSK_SPAWN_EGG, Material.LLAMA_SPAWN_EGG, Material.MAGMA_CUBE_SPAWN_EGG, Material.MOOSHROOM_SPAWN_EGG, Material.MULE_SPAWN_EGG, Material.OCELOT_SPAWN_EGG, Material.PANDA_SPAWN_EGG, Material.PARROT_SPAWN_EGG, Material.PHANTOM_SPAWN_EGG, Material.PIG_SPAWN_EGG, Material.PIGLIN_SPAWN_EGG, Material.PIGLIN_BRUTE_SPAWN_EGG, Material.PILLAGER_SPAWN_EGG, Material.POLAR_BEAR_SPAWN_EGG, Material.PUFFERFISH_SPAWN_EGG, Material.RABBIT_SPAWN_EGG, Material.RAVAGER_SPAWN_EGG, Material.SALMON_SPAWN_EGG, Material.SHEEP_SPAWN_EGG, Material.SHULKER_SPAWN_EGG, Material.SILVERFISH_SPAWN_EGG, Material.SKELETON_SPAWN_EGG, Material.SKELETON_HORSE_SPAWN_EGG, Material.SLIME_SPAWN_EGG, Material.SPIDER_SPAWN_EGG, Material.SQUID_SPAWN_EGG, Material.STRAY_SPAWN_EGG, Material.STRIDER_SPAWN_EGG, Material.TRADER_LLAMA_SPAWN_EGG, Material.TROPICAL_FISH_SPAWN_EGG, Material.TURTLE_SPAWN_EGG, Material.VEX_SPAWN_EGG, Material.VILLAGER_SPAWN_EGG, Material.VINDICATOR_SPAWN_EGG, Material.WANDERING_TRADER_SPAWN_EGG, Material.WITCH_SPAWN_EGG, Material.WITHER_SKELETON_SPAWN_EGG, Material.WOLF_SPAWN_EGG, Material.ZOGLIN_SPAWN_EGG, Material.ZOMBIE_SPAWN_EGG, Material.ZOMBIE_HORSE_SPAWN_EGG, Material.ZOMBIE_VILLAGER_SPAWN_EGG, Material.ZOMBIFIED_PIGLIN_SPAWN_EGG, Material.FIRE_CHARGE, Material.WRITABLE_BOOK, Material.MAP, Material.NETHER_STAR, Material.FIREWORK_ROCKET, Material.FIREWORK_STAR, Material.NETHER_BRICK, Material.PRISMARINE_SHARD, Material.PRISMARINE_CRYSTALS, Material.RABBIT_HIDE, Material.LEATHER_HORSE_ARMOR, Material.IRON_HORSE_ARMOR, Material.GOLDEN_HORSE_ARMOR, Material.DIAMOND_HORSE_ARMOR, Material.CHORUS_FRUIT, Material.POPPED_CHORUS_FRUIT, Material.BEETROOT_SEEDS, Material.SHULKER_SHELL, Material.IRON_NUGGET, Material.MUSIC_DISC_13, Material.MUSIC_DISC_CAT, Material.MUSIC_DISC_BLOCKS, Material.MUSIC_DISC_CHIRP, Material.MUSIC_DISC_FAR, Material.MUSIC_DISC_MALL, Material.MUSIC_DISC_MELLOHI, Material.MUSIC_DISC_STAL, Material.MUSIC_DISC_STRAD, Material.MUSIC_DISC_WARD, Material.MUSIC_DISC_11, Material.MUSIC_DISC_WAIT, Material.MUSIC_DISC_PIGSTEP, Material.NAUTILUS_SHELL, Material.HEART_OF_THE_SEA, Material.FLOWER_BANNER_PATTERN, Material.CREEPER_BANNER_PATTERN, Material.SKULL_BANNER_PATTERN, Material.MOJANG_BANNER_PATTERN, Material.GLOBE_BANNER_PATTERN, Material.PIGLIN_BANNER_PATTERN, Material.HONEYCOMB );
    private static final EnumSet<Material> BREWING_CATEGORY = EnumSet.of( Material.GHAST_TEAR, Material.POTION, Material.GLASS_BOTTLE, Material.FERMENTED_SPIDER_EYE, Material.BLAZE_POWDER, Material.MAGMA_CREAM, Material.BREWING_STAND, Material.CAULDRON, Material.GLISTERING_MELON_SLICE, Material.GOLDEN_CARROT, Material.RABBIT_FOOT, Material.DRAGON_BREATH, Material.SPLASH_POTION, Material.LINGERING_POTION, Material.PHANTOM_MEMBRANE );

    // Карта категорий и их предикатов
    public static final Map<String, Predicate<Material>> CATEGORIES = new LinkedHashMap<>();
    static {
        // Специальные случаи
        CATEGORIES.put("food", Material::isEdible);
        CATEGORIES.put("enchanting", m -> m == Material.ENCHANTED_BOOK);
        CATEGORIES.put("brewing", BREWING_CATEGORY::contains);

        // Категории по наборам
        CATEGORIES.put("redstone", REDSTONE_CATEGORY::contains);
        CATEGORIES.put("transportation", TRANSPORTATION_CATEGORY::contains);
        CATEGORIES.put("decoration", DECORATION_BLOCKS_CATEGORY::contains);
        CATEGORIES.put("combat", COMBAT_CATEGORY::contains);
        CATEGORIES.put("tools", TOOLS_CATEGORY::contains);

        // Исправленная проверка для строительных блоков
        CATEGORIES.put("building", m -> m.isSolid()
                && !REDSTONE_CATEGORY.contains(m)
                && !DECORATION_BLOCKS_CATEGORY.contains(m)
                && !MISCELLANEOUS_CATEGORY.contains(m)
        );

        // Общая категория
        CATEGORIES.put("miscellaneous", MISCELLANEOUS_CATEGORY::contains);
    }

    // Поля класса
    private int id;
    private final String seller;
    public final String item;
    private final String category;
    public final String itemID;
    private final float price, unit;
    private final int amount;
    private final long time;
    public final String status;
    public final String buyer;
    public final int amountValue;


    public ShowPiece(String itemID, String seller, String item, String category, int amount, float price, float unit, long time, String status, String buyer, int amountValue) {
        this.itemID = itemID;
        this.seller = seller;
        this.item = item;
        this.category = category;
        this.price = price;
        this.unit = unit;
        this.amount = amount;
        this.time = time;
        this.status = status;
        this.buyer = buyer;
        this.amountValue = amountValue;
    }

    public static ShowPiece create(String seller, ItemStack item, float price) {
        String itemBase64 = itemToString(item);
        int amount = item.getAmount();
        float unit = price / amount;
        long time = System.currentTimeMillis();

        return CATEGORIES.entrySet().stream()
                .filter(e -> e.getValue().test(item.getType()))
                .findFirst()
                .map(e -> new ShowPiece(ID_Gen.generateID_M2(), seller, itemBase64, e.getKey(), amount, price, unit, time, "0", "NULL", 0) )
                .orElse(new ShowPiece(ID_Gen.generateID_M2(), seller, itemBase64, "null", amount, price, unit, time, "0", "NULL", 0) );
    }

    public double getTotalSum() { return amount * price; }
    public boolean isExpired(long unix, int period) { return (System.currentTimeMillis() > getTime() + period); }

    public ItemStack toItemStack() {
        return ItemFactory.createItem(ItemFactory.ItemData.fromItemStack(ItemFactory.stringToItem(getItem())));
    }

    public String getItemID() { return itemID; }
    public String getStatus() { return status; }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getSeller() { return seller; }
    public String getItem() { return item; }
    public String getCategory() { return category; }
    public float getPrice() { return price; }
    public float getUnit() { return unit; }
    public int getAmount() { return amount; }
    public long getTime() { return time; }

    public String getBuyer() { return buyer; }
    public int getAmountValue() { return amountValue; }
}