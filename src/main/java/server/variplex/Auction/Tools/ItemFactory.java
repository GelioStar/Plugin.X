package server.variplex.Auction.Tools;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.*;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.banner.Pattern;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class ItemFactory {

    // Кэш для рефлексивных полей
    private static final Map<String, Field> PROFILE_FIELDS = new HashMap<>();

    public static class ItemData {
        // Основные свойства
        public Material material;
        public int amount = 1;
        public int durability = 0;

        // Общие метаданные
        public String displayName;
        public List<String> lore;
        public int customModelData;
        public Set<ItemFlag> itemFlags = new HashSet<>();
        public Map<Enchantment, Integer> enchantments = new HashMap<>();

        // Специфические данные
        public PotionData potionData;
        public List<PotionEffect> potionEffects = new ArrayList<>();
        public Color potionColor;
        public List<ItemStack> shulkerContents = new ArrayList<>();
        public List<FireworkEffect> fireworkEffects = new ArrayList<>();
        public List<Pattern> bannerPatterns = new ArrayList<>();
        public String skullTexture;
        public Map<String, String> persistentData = new HashMap<>();

        public static ItemData fromItemStack(ItemStack item) {
            if (item == null || item.getType() == Material.AIR) return null;

            ItemData data = new ItemData();
            data.material = item.getType();
            data.amount = item.getAmount();

            if (!item.hasItemMeta()) return data;

            ItemMeta meta = item.getItemMeta();

            // Общие метаданные
            assert meta != null;
            data.displayName = meta.getDisplayName();
            data.lore = meta.getLore();
            data.customModelData = meta.getCustomModelData();
            data.itemFlags = meta.getItemFlags();
            data.enchantments = meta.getEnchants();

            // Durability
            if (meta instanceof Damageable) {
                data.durability = ((Damageable) meta).getDamage();
            }

            // Persistent Data
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.getKeys().forEach(key -> {
                String value = pdc.get(key, PersistentDataType.STRING);
                if (value != null) {
                    data.persistentData.put(key.toString(), value);
                }
            });

            // Обработка специфических типов
            if (meta instanceof PotionMeta) {
                handlePotionMeta((PotionMeta) meta, data);
            }
            if (item.getType().name().contains("SHULKER_BOX") && meta instanceof BlockStateMeta) {
                handleShulkerMeta((BlockStateMeta) meta, data);
            }
            if (meta instanceof FireworkMeta) {
                handleFireworkMeta((FireworkMeta) meta, data);
            }
            if (meta instanceof BannerMeta) {
                handleBannerMeta((BannerMeta) meta, data);
            }
            if (meta instanceof SkullMeta) {
                handleSkullMeta((SkullMeta) meta, data);
            }

            return data;
        }
    }

    public static ItemStack createItem(ItemData data) {
        if (data == null) return new ItemStack(Material.AIR);

        ItemStack item = new ItemStack(data.material, data.amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        applyCommonMeta(meta, data);
        applySpecificMeta(meta, data);

        item.setItemMeta(meta);
        return item;
    }

    private static void applyCommonMeta(ItemMeta meta, ItemData data) {
        if (data.displayName != null) meta.setDisplayName(data.displayName);
        if (data.lore != null) meta.setLore(data.lore);
        if (data.customModelData != 0) meta.setCustomModelData(data.customModelData);

        data.itemFlags.forEach(meta::addItemFlags);
        data.enchantments.forEach((ench, lvl) -> meta.addEnchant(ench, lvl, true));

        // Durability
        if (meta instanceof Damageable) {
            ((Damageable) meta).setDamage(data.durability);
        }

        // Persistent Data
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        data.persistentData.forEach((key, value) ->
                pdc.set(new NamespacedKey("C8Au", key), PersistentDataType.STRING, value)
        );
    }

    private static void applySpecificMeta(ItemMeta meta, ItemData data) {
        if (meta instanceof PotionMeta) {
            applyPotionMeta((PotionMeta) meta, data);
        }
        if (meta instanceof BlockStateMeta) {
            applyShulkerMeta((BlockStateMeta) meta, data);
        }
        if (meta instanceof FireworkMeta) {
            applyFireworkMeta((FireworkMeta) meta, data);
        }
        if (meta instanceof BannerMeta) {
            applyBannerMeta((BannerMeta) meta, data);
        }
        if (meta instanceof SkullMeta) {
            applySkullMeta((SkullMeta) meta, data);
        }
    }

    // region Handlers
    private static void handlePotionMeta(PotionMeta meta, ItemData data) {
        data.potionData = meta.getBasePotionData();
        data.potionEffects = meta.getCustomEffects();
        data.potionColor = meta.getColor();
    }

    private static void applyPotionMeta(PotionMeta meta, ItemData data) {
        meta.setBasePotionData(data.potionData);
        data.potionEffects.forEach(effect -> meta.addCustomEffect(effect, true));
        meta.setColor(data.potionColor);
    }

    private static void handleShulkerMeta(BlockStateMeta meta, ItemData data) {
        ShulkerBox box = (ShulkerBox) meta.getBlockState();
        data.shulkerContents = Arrays.stream(box.getInventory().getContents())
                .map(item -> item != null ? item.clone() : null)
                .collect(Collectors.toList());
    }

    private static void applyShulkerMeta(BlockStateMeta meta, ItemData data) {
        ShulkerBox box = (ShulkerBox) meta.getBlockState();
        Inventory inv = box.getInventory();
        inv.clear();

        for (int i = 0; i < data.shulkerContents.size(); i++) {
            ItemStack item = data.shulkerContents.get(i);
            if (item != null) {
                inv.setItem(i, item.clone());
            }
        }
        meta.setBlockState(box);
    }

    private static void handleFireworkMeta(FireworkMeta meta, ItemData data) {
        data.fireworkEffects = new ArrayList<>(meta.getEffects());
    }

    private static void applyFireworkMeta(FireworkMeta meta, ItemData data) {
        meta.addEffects(data.fireworkEffects);
    }

    private static void handleBannerMeta(BannerMeta meta, ItemData data) {
        data.bannerPatterns = new ArrayList<>(meta.getPatterns());
    }

    private static void applyBannerMeta(BannerMeta meta, ItemData data) {
        meta.setPatterns(data.bannerPatterns);
    }

    private static void handleSkullMeta(SkullMeta meta, ItemData data) {
        try {
            Field profileField = getProfileField();
            GameProfile profile = (GameProfile) profileField.get(meta);
            if (profile != null) {
                data.skullTexture = profile.getProperties().get("textures")
                        .stream()
                        .findFirst()
                        .map(Property::getValue)
                        .orElse(null);
            }
        } catch (Exception e) {
            // Логирование ошибки
        }
    }

    private static void applySkullMeta(SkullMeta meta, ItemData data) {
        if (data.skullTexture == null) return;

        try {
            GameProfile profile = new GameProfile(UUID.randomUUID(), null);
            profile.getProperties().put("textures", new Property("textures", data.skullTexture));

            Field profileField = getProfileField();
            profileField.set(meta, profile);
        } catch (Exception e) {
            // Логирование ошибки
        }
    }

    private static Field getProfileField() {
        String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        return PROFILE_FIELDS.computeIfAbsent(version, v -> {
            try {
                Field f = SkullMeta.class.getDeclaredField("profile");
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException e) {
                throw new RuntimeException("Error getting profile field", e);
            }
        });
    }

    public static String itemToString(ItemStack item) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeObject(item);
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Ошибка конвертации предмета в Base64.", e);
        }
    }

    public static ItemStack stringToItem(String base64) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(base64));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
            return (ItemStack) dataInput.readObject();
        } catch (Exception e) {
            throw new IllegalStateException("Ошибка конвертации из Base64 в предмет.", e);
        }
    }

    public static class PotionUtil {
        public static String getEffectName(PotionEffect effect) {
            String name = effect.getType().getName();
            String duration = getDurationString(effect.getDuration());
            String amplifier = getAmplifierString(effect.getAmplifier());
            return ChatColor.GRAY + name + " " + amplifier + ChatColor.GRAY + " (" + duration + ")";
        }

        private static String getDurationString(int ticks) {
            int seconds = ticks / 20;
            return seconds + " сек";
        }

        private static String getAmplifierString(int amplifier) {
            if (amplifier == 0) return "I";
            return RomanNumber.toRoman(amplifier + 1);
        }
    }

    public static class RomanNumber {
        private static final String[] ROMAN_NUMBERS = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

        public static String toRoman(int number) {
            if (number <= 0 || number > 10) return String.valueOf(number);
            return ROMAN_NUMBERS[number];
        }
    }



}