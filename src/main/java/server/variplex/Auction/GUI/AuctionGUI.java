package server.variplex.Auction.GUI;

import all.api.UTILS.Menu;
import all.api.UTILS.Message;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.banner.Pattern;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.potion.PotionEffect;
import server.variplex.Auction.Auction;
import server.variplex.Auction.SQL.AuctionCore;
import server.variplex.Auction.SQL.SQLite;
import server.variplex.Auction.SQL.ShowPiece;
import server.variplex.Auction.Tools.ItemFactory;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

import static all.api.UTILS.Message.translateColorCodes;
import static server.variplex.Auction.Commands.Auction.formatFloat;
import static server.variplex.Auction.SQL.SQLite.getShowPieceByID;

public class AuctionGUI {
    private static final int ITEMS_PER_PAGE = 45;

    public static Inventory openAuctionTab(Player player, int category, int mode, String status, int currentPage, boolean isSellersItems, boolean isExpiredItems, @Nullable String isPlayersItemsUUID) {
        List<ItemStack> showpieces = (isPlayersItemsUUID != null) ? SQLite.getShowpiecesItemStackSQL(isPlayersItemsUUID, category, mode, status) : SQLite.getShowpiecesItemStackSQL(category, mode, status);
        List<ShowPiece> showpieceDataList = (isPlayersItemsUUID != null) ? SQLite.getShowpiecesItemStackSQLData(isPlayersItemsUUID, category, mode, status) : SQLite.getShowpiecesItemStackSQLData(category, mode, status, null);
        if (showpieces == null) {
            player.sendMessage("No data available for the selected category.");
            return null;
        }
        int totalPages = (int) Math.ceil((double) showpieces.size() / 45);
        if (currentPage < 1) currentPage = 1;
        if (currentPage > totalPages) currentPage = totalPages;
        Inventory inventory = (isPlayersItemsUUID != null) ? ((isExpiredItems) ? Menu.createDefaultMenu(player, 54, translateColorCodes("&#1C1C1CНераспроданные товары")) : Menu.createDefaultMenu(player, 54, translateColorCodes("&#1C1C1CАукцион " + Objects.requireNonNull(Bukkit.getPlayer(UUID.fromString(isPlayersItemsUUID))).getName()))) : (totalPages > 1) ? Menu.createDefaultMenu(player, 54, translateColorCodes("&#1C1C1CАукцион (" + currentPage + "/" + totalPages + ")")) : Menu.createDefaultMenu(player, 54, translateColorCodes("&#1C1C1CАукцион (1/1)"));
        if (showpieces.size() <= 0) {
            Menu.createItem(inventory, 48, Material.GRAY_DYE, 1, Message.translateColorCodes("&#16DF88&l◀ &#19DE7DП&#1BDE78р&#1CDE73е&#1EDD6Eд&#1FDD68ы&#21DD63д&#22DC5Eу&#24DC59щ&#25DC53а&#27DC4Eя &#2ADB43с&#2CDB3Eт&#2DDA39р&#2FDA34а&#30DA2Eн&#32D929и&#33D924ц&#35D91Fа"), new ArrayList<>());
            Menu.createItem(inventory, 50, Material.GRAY_DYE, 1, Message.translateColorCodes("&#3CD707С&#3AD70Eл&#38D815е&#36D81Bд&#34D922у&#32D929ю&#30DA30щ&#2EDA36а&#2CDA3Dя &#28DB4Bс&#26DC51т&#24DC58р&#22DD5Fа&#20DD66н&#1EDD6Cи&#1CDE73ц&#1ADE7Aа &#16DF87&l▶"), new ArrayList<>());
            SellersItemsOrAuctionList_Slot45(inventory, !isSellersItems);
            ExpiredItemsOrAuctionList_Slot46(inventory, !isExpiredItems);
            SQLCategorySwitcher(inventory, category);
            SQLModeSwitcher(inventory, mode);
            addPaginationControls(inventory, currentPage, totalPages);
            updaterButtonAndDescription(inventory);
            return inventory;
        }
        int startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int currentIndex = startIndex + i;
            if (currentIndex >= showpieces.size()) break;
            ItemStack originalItem = showpieces.get(currentIndex);
            assert showpieceDataList != null;
            ShowPiece data = showpieceDataList.get(currentIndex);
            if (!isExpiredItems && data.getTime() + Auction.getSettings().getConfig().getLong("intervalMillis.slot") < System.currentTimeMillis()) {
                new AuctionCore().addExpiredItem(data);
                continue;
            }
            ItemStack clonedItem = originalItem.clone();
            ItemMeta meta = clonedItem.getItemMeta();
            ArrayList<String> loreItem = new ArrayList<>();
            buildLore(loreItem, data, originalItem, isExpiredItems);
            assert meta != null;
            meta.setLore(loreItem);
            clonedItem.setItemMeta(meta);
            inventory.setItem(i, clonedItem);
        }
        SellersItemsOrAuctionList_Slot45(inventory, !isSellersItems);
        ExpiredItemsOrAuctionList_Slot46(inventory, !isExpiredItems);
        SQLCategorySwitcher(inventory, category);
        SQLModeSwitcher(inventory, mode);
        addPaginationControls(inventory, currentPage, totalPages);
        updaterButtonAndDescription(inventory);
        return inventory;
    }

    private static final Map<String, String> CATEGORY_DISPLAY_NAMES = new HashMap<>();
    static {
        CATEGORY_DISPLAY_NAMES.put("buildings_block", "Строительство");
        CATEGORY_DISPLAY_NAMES.put("decoration_block", "Декорирование");
        CATEGORY_DISPLAY_NAMES.put("combat", "Оружие и доспехи");
        CATEGORY_DISPLAY_NAMES.put("tools", "Инструменты");
        CATEGORY_DISPLAY_NAMES.put("transportation", "Транспорт");
        CATEGORY_DISPLAY_NAMES.put("food", "Еда");
        CATEGORY_DISPLAY_NAMES.put("redstone", "Механизмы");
        CATEGORY_DISPLAY_NAMES.put("brewing", "Зельеварение");
        CATEGORY_DISPLAY_NAMES.put("enchanting", "Зачарование");
        CATEGORY_DISPLAY_NAMES.put("miscellaneous", "Разное");
    }
    private static String getCategoryDisplayName(String categoryKey) {
        return CATEGORY_DISPLAY_NAMES.getOrDefault( categoryKey, "Неизвестная категория" );
    }

    private static void buildLore(List<String> loreItem, ShowPiece data, ItemStack itemStack, boolean isExpiredItem) {
        Configuration config = Auction.getSettings().getConfig();
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
        List<String> buildLore = config.getStringList("ShowPiece.buildLore");
        for (String line : buildLore) {
            String replacedLine = line
                    .replace("{id}", data.getItemID())
                    .replace("{category}", getCategoryDisplayName(data.getCategory()))
                    .replace("{seller}", Objects.requireNonNull(Bukkit.getPlayer(UUID.fromString(data.getSeller()))).getName());
            loreItem.add(Message.translateColorCodes(replacedLine));
        }
        try {
            if (itemStack.getItemMeta() != null && itemStack.getItemMeta().getLore() != null)
                loreItem.addAll(itemStack.getItemMeta().getLore());
        } catch (NullPointerException ignored) {}
        ConfigurationSection priceSection = config.getConfigurationSection("ShowPiece.buildLoreInfo.price");
        if (data.getAmount() > 1) {
            assert priceSection != null;
            String totalPriceLine = Objects.requireNonNull(priceSection.getString("multi.total"))
                    .replace("{total_price}", formatter.format((int) data.getPrice()));
            loreItem.add(Message.translateColorCodes(totalPriceLine));
            String unitPriceKey = isExpiredItem ? "unit_expired" : "unit_active";
            String unitPriceLine = Objects.requireNonNull(priceSection.getString("multi." + unitPriceKey))
                    .replace("{unit_price}", String.valueOf(data.getUnit()));
            loreItem.add(Message.translateColorCodes(unitPriceLine));
        } else {
            assert priceSection != null;
            String singleLine = Objects.requireNonNull(priceSection.getString("single"))
                    .replace("{total_price}", formatter.format((int) data.getPrice()));
            loreItem.add(Message.translateColorCodes(singleLine));
        }
        if (!isExpiredItem) {
            String expireLine = Objects.requireNonNull(config.getString("ShowPiece.buildLoreInfo.expire"))
                    .replace("{expire_time}", toFormattedTime(data.getTime() + config.getLong("intervalMillis.slot")));
            loreItem.add(Message.translateColorCodes(expireLine));
        }
        if (config.getBoolean("ShowPiece.buildLoreInfo.empty_line_condition") && !Objects.requireNonNull(ItemFactory.stringToItem(data.getItem()).getItemMeta()).hasAttributeModifiers())
            loreItem.add(Message.translateColorCodes(" "));
    }

    public static void SQLCategorySwitcher(Inventory inventory, int activeCategory) {
        Configuration config = Auction.getSettings().getConfig();
        String configPath = "Items.SQLCategorySwitcher.";
        int slot = config.getInt(configPath + "slot", 52);
        Material material = Material.matchMaterial(config.getString(configPath + "material", "CHEST_MINECART"));
        if (material == null) material = Material.CHEST_MINECART;
        int amount = config.getInt(configPath + "amount", 1);
        String name = translateColorCodes(config.getString(configPath + "name", " "));
        List<String> lore = config.getStringList(configPath + "lore.header")
                .stream()
                .map(Message::translateColorCodes).collect(Collectors.toList());
        List<String> categories = config.getStringList(configPath + "lore.categories");
        String activeMarker = translateColorCodes(config.getString(configPath + "lore.markers.active", "&#00ACFF⏺"));
        String inactiveMarker = translateColorCodes(config.getString(configPath + "lore.markers.inactive", "&#393939○"));
        String activeColor = translateColorCodes(config.getString(configPath + "lore.colors.active", "&#2998CD"));
        String inactiveColor = translateColorCodes(config.getString(configPath + "lore.colors.inactive", "&7"));

        for (int i = 0; i < categories.size(); i++) {
            boolean isActive = (i == activeCategory);
            String marker = isActive ? activeMarker : inactiveMarker;
            String color = isActive ? activeColor : inactiveColor;
            lore.add(translateColorCodes(String.format(" %s %s%s  ", marker, color, categories.get(i))));
        }
        lore.addAll(
                config.getStringList(configPath + "lore.footer")
                        .stream()
                        .map(Message::translateColorCodes).toList()
        );
        Menu.createItem(inventory, slot, material, amount, name, lore);
    }
    private static void SQLModeSwitcher(Inventory inventory, int activeMode) {
        Configuration config = Auction.getSettings().getConfig();
        String configPath = "Items.SQLModeSwitcher.";
        int slot = config.getInt(configPath + "slot", 51);
        Material material = Material.matchMaterial(config.getString(configPath + "material", "HOPPER_MINECART"));
        if (material == null) material = Material.HOPPER_MINECART;
        int amount = config.getInt(configPath + "amount", 1);
        String name = translateColorCodes(config.getString(configPath + "name", " "));
        List<String> lore = config.getStringList(configPath + "lore.header")
                .stream()
                .map(Message::translateColorCodes).collect(Collectors.toList());

        List<String> modes = config.getStringList(configPath + "lore.modes");
        String activeMarker = translateColorCodes(config.getString(configPath + "lore.markers.active", "&#00ACFF⏺"));
        String inactiveMarker = translateColorCodes(config.getString(configPath + "lore.markers.inactive", "&#393939○"));
        String activeColor = translateColorCodes(config.getString(configPath + "lore.colors.active", "&#2998CD"));
        String inactiveColor = translateColorCodes(config.getString(configPath + "lore.colors.inactive", "&7"));

        for (int i = 0; i < modes.size(); i++) {
            boolean isActive = (i == activeMode);
            String marker = isActive ? activeMarker : inactiveMarker;
            String color = isActive ? activeColor : inactiveColor;
            lore.add(translateColorCodes(String.format(" %s %s%s  ", marker, color, modes.get(i))));
        }
        lore.addAll(
                config.getStringList(configPath + "lore.footer")
                        .stream()
                        .map(Message::translateColorCodes).toList()
        );
        Menu.createItem(inventory, slot, material, amount, name, lore);
    }

    private static void SellersItemsOrAuctionList_Slot45(Inventory inventory, boolean isAuctionList) {
        Configuration config = Auction.getSettings().getConfig();
        String configPath = "Items.SellersAuctionItem." + (isAuctionList ? "AuctionItem" : "SellersItem");
        String materialName = config.getString(configPath + ".material", "HONEYCOMB");
        Material material = Material.matchMaterial(materialName);
        if (material == null)
            Auction.getInstance().getLogger().warning("Неверный материал в конфиге: " + materialName);
        int amount = config.getInt(configPath + ".amount", 1);
        String name = translateColorCodes(config.getString(configPath + ".name", " "));
        List<String> lore = config.getStringList(configPath + ".lore")
                .stream()
                .map(Message::translateColorCodes)
                .collect(Collectors.toList());
        int slot = config.getInt("Items.SellersAuctionItem.slot");
        assert material != null;
        Menu.createItem(inventory, slot, material, amount, name, lore);
    }

    private static void ExpiredItemsOrAuctionList_Slot46(Inventory inventory, boolean isAuctionList) { //по дефолту - ExpiredItemsList
        List<String> lore = new ArrayList<>();
        if(isAuctionList) {
            List<String> auctionItemLore = Auction.getSettings().getConfig().getStringList("Items.AuctionExpiredItem.AuctionItem.lore");
            for(String S : auctionItemLore)
                lore.add(translateColorCodes(S));
        } else {
            List<String> expiredItemLore = Auction.getSettings().getConfig().getStringList("Items.AuctionExpiredItem.ExpiredItem.lore");
            for(String S : expiredItemLore)
                lore.add(translateColorCodes(S));
        }
        int slot = Auction.getSettings().getConfig().getInt("Items.AuctionExpiredItem.slot");
        Material material = Objects.requireNonNull((isAuctionList)
                ? Material.matchMaterial(Objects.requireNonNull(Auction.getSettings().getConfig().getString("Items.AuctionExpiredItem.AuctionItem.material")))
                : Material.matchMaterial(Objects.requireNonNull(Auction.getSettings().getConfig().getString("Items.AuctionExpiredItem.ExpiredItem.material"))));
        int amount = (isAuctionList)
                ? Auction.getSettings().getConfig().getInt("Items.AuctionExpiredItem.AuctionItem.amount")
                : Auction.getSettings().getConfig().getInt("Items.AuctionExpiredItem.ExpiredItem.amount");
        String name = translateColorCodes((isAuctionList)
                ? translateColorCodes(Objects.requireNonNull(Auction.getSettings().getConfig().getString("Items.AuctionExpiredItem.AuctionItem.name")))
                : translateColorCodes(Objects.requireNonNull(Auction.getSettings().getConfig().getString("Items.AuctionExpiredItem.ExpiredItem.name"))));
        Menu.createItem(inventory, slot, material, amount, name, lore);
    }

    public static String toFormattedTime(long endTimeMillis) {
        long timeLeftMillis = endTimeMillis - System.currentTimeMillis();
        long totalSeconds = timeLeftMillis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format(Message.translateColorCodes("&#18acff%02d&7:&#18acff%02d&7:&#18acff%02d&7"), hours, minutes, seconds);
    }
    private static void addPaginationControls(Inventory inventory, int currentPage, int totalPages) {
        final String prevText = "&#16DF88&l◀ &#19DE7DП&#1BDE78р&#1CDE73е&#1EDD6Eд&#1FDD68ы&#21DD63д&#22DC5Eу&#24DC59щ&#25DC53а&#27DC4Eя &#2ADB43с&#2CDB3Eт&#2DDA39р&#2FDA34а&#30DA2Eн&#32D929и&#33D924ц&#35D91Fа";
        final String nextText = "&#3CD707С&#3AD70Eл&#38D815е&#36D81Bд&#34D922у&#32D929ю&#30DA30щ&#2EDA36а&#2CDA3Dя &#28DB4Bс&#26DC51т&#24DC58р&#22DD5Fа&#20DD66н&#1EDD6Cи&#1CDE73ц&#1ADE7Aа &#16DF87&l▶";
        boolean isFirstPage = currentPage <= 1 || totalPages == 1;
        boolean isLastPage = currentPage >= totalPages || totalPages == 1;
        Menu.createItem(inventory, 48, isFirstPage ? Material.GRAY_DYE : Material.LIME_DYE, 1, Message.translateColorCodes(prevText), new ArrayList<>());
        Menu.createItem(inventory, 50, isLastPage ? Material.GRAY_DYE : Material.LIME_DYE, 1, Message.translateColorCodes(nextText), new ArrayList<>());
    }

    private static void updaterButtonAndDescription(Inventory inventory) {
        createConfigItem(inventory, "Items.UpdaterButton");
        createConfigItem(inventory, "Items.DescriptionItem");
        createConfigItemWithPlaceholders(inventory);
    }
    private static void createConfigItem(Inventory inventory, String configPath) {
        Configuration config = Auction.getSettings().getConfig();
        Material material = Material.matchMaterial(Objects.requireNonNull(config.getString(configPath + ".material")));
        int slot = config.getInt(configPath + ".slot");
        int amount = config.getInt(configPath + ".amount", 1);
        String name = translateColorCodes(config.getString(configPath + ".name", " "));
        List<String> lore = config.getStringList(configPath + ".lore")
                .stream()
                .map(Message::translateColorCodes)
                .collect(Collectors.toList());
        assert material != null;
        Menu.createItem(inventory, slot, material, amount, name, lore);
    }
    private static void createConfigItemWithPlaceholders(Inventory inventory) {
        Configuration config = Auction.getSettings().getConfig();
        Material material = Material.matchMaterial(config.getString("Items.CommandsItem" + ".material", "BLAZE_POWDER"));
        int slot = config.getInt("Items.CommandsItem" + ".slot", 53);
        int amount = config.getInt("Items.CommandsItem" + ".amount", 1);
        String name = translateColorCodes(config.getString("Items.CommandsItem" + ".name", " "));
        int minCoins = config.getInt("coinsValue.minValue");
        int maxCoins = config.getInt("coinsValue.maxValue");
        String formattedMaxCoins = NumberFormat.getNumberInstance(Locale.US).format(maxCoins);
        List<String> lore = new ArrayList<>();
        for (String line : config.getStringList("Items.CommandsItem" + ".lore")) {
            String replacedLine = line.replace("{min_coins}", String.valueOf(minCoins))
                    .replace("{max_coins}", formattedMaxCoins);
            lore.add(translateColorCodes(replacedLine));
        }
        assert material != null;
        Menu.createItem(inventory, slot, material, amount, name, lore);
    }

    public static Inventory acceptSell(Player player, ItemStack itemStack, List<String> loreItem) {
        Inventory inventory = Menu.createDefaultMenu(player, 27, Message.translateColorCodes("&#1C1C1CПодтверждение покупки"));
//        Menu.createItem(inventory, new int[]{0, 1, 2, 9, 11, 18, 19, 20}, Material.LIME_STAINED_GLASS_PANE, 1, Message.translateColorCodes("&#34EA00П&#2FE707о&#2AE50Dд&#25E214т&#20DF1Bв&#1CDD22е&#17DA28р&#12D72Fд&#0DD436и&#08D23Cт&#03CF43ь"), new ArrayList<>());
//        Menu.createItem(inventory, 10, Material.GREEN_STAINED_GLASS_PANE, 1, Message.translateColorCodes("&#34EA00П&#2FE707о&#2AE50Dд&#25E214т&#20DF1Bв&#1CDD22е&#17DA28р&#12D72Fд&#0DD436и&#08D23Cт&#03CF43ь"), new ArrayList<>());
//        Menu.createItem(inventory, new int[]{3, 4, 5, 12, 14, 21, 22, 23}, Material.CYAN_STAINED_GLASS_PANE, 1, Message.translateColorCodes("&#005CEAП&#0062EAо&#0068EAд&#006EEAт&#0074EAв&#007AEAе&#0080EAр&#0086EAд&#008CEAе&#0092EAн&#0099EAи&#009FEAе &#00ABEAп&#00B1EAо&#00B7EAк&#00BDEAу&#00C3EAп&#00C9EAк&#00CFEAи"), new ArrayList<>());
//        Menu.createItem(inventory, new int[]{6, 7, 8, 15, 17, 24, 25, 26}, Material.RED_STAINED_GLASS_PANE, 1, Message.translateColorCodes("&#DD0000О&#DE0400т&#E00800к&#E10D00а&#E21100з&#E41500а&#E51900т&#E61E00ь&#E82200с&#E92600я"), new ArrayList<>());
//        Menu.createItem(inventory, 16, Material.BROWN_STAINED_GLASS_PANE, 1, Message.translateColorCodes("&#DD0000О&#DE0400т&#E00800к&#E10D00а&#E21100з&#E41500а&#E51900т&#E61E00ь&#E82200с&#E92600я"), new ArrayList<>());


//        Menu.createItem(inventory, new int[]{0, 1, 2, 9, 10, 11, 18, 19, 20}, Material.LIME_STAINED_GLASS_PANE, 1, Message.translateColorCodes("&#34EA00П&#2FE707о&#2AE50Dд&#25E214т&#20DF1Bв&#1CDD22е&#17DA28р&#12D72Fд&#0DD436и&#08D23Cт&#03CF43ь"), new ArrayList<>());
        Menu.createItem(inventory, 10, Material.LIME_STAINED_GLASS_PANE, 1, Message.translateColorCodes("&#34EA00П&#2FE707о&#2AE50Dд&#25E214т&#20DF1Bв&#1CDD22е&#17DA28р&#12D72Fд&#0DD436и&#08D23Cт&#03CF43ь"), new ArrayList<>());
        Menu.createItem(inventory, new int[]{3, 4, 5, 12, 14, 21, 22, 23}, Material.CYAN_STAINED_GLASS_PANE, 1, Message.translateColorCodes("&#005CEAП&#0062EAо&#0068EAд&#006EEAт&#0074EAв&#007AEAе&#0080EAр&#0086EAд&#008CEAе&#0092EAн&#0099EAи&#009FEAе &#00ABEAп&#00B1EAо&#00B7EAк&#00BDEAу&#00C3EAп&#00C9EAк&#00CFEAи"), new ArrayList<>());
//        Menu.createItem(inventory, new int[]{6, 7, 8, 15, 17, 24, 25, 26}, Material.RED_STAINED_GLASS_PANE, 1, Message.translateColorCodes("&#DD0000О&#DE0400т&#E00800к&#E10D00а&#E21100з&#E41500а&#E51900т&#E61E00ь&#E82200с&#E92600я"), new ArrayList<>());
        Menu.createItem(inventory, 16, Material.RED_STAINED_GLASS_PANE, 1, Message.translateColorCodes("&#DD0000О&#DE0400т&#E00800к&#E10D00а&#E21100з&#E41500а&#E51900т&#E61E00ь&#E82200с&#E92600я"), new ArrayList<>());
        createItemFromSQL(inventory, itemStack, loreItem, 13);
        return inventory;
    }

    public static Inventory quantityGoods(Player player, ItemStack itemStack, List<String> loreItem, Map<Player, Integer> acceptSell, int amount) {
        Inventory inventory = Menu.createMenu(InventoryType.HOPPER, player, Message.translateColorCodes("&#1C1C1CКоличество товара"));
        AuctionGUI.acceptSellValue(Objects.requireNonNull(getShowPieceByID(acceptSell.get(player))), inventory, itemStack, loreItem, amount);
        Menu.createItem(inventory, 4, Material.LIME_STAINED_GLASS_PANE, 1, Message.translateColorCodes("&#34EA00К&#32EA01о&#30E902л&#2EE903и&#2BE804ч&#29E805е&#27E806с&#25E707т&#23E708в&#21E609о&#1EE60A: &#1AE50C+&#18E50D8 &#14E40Fе&#11E310д&#0FE311. &#0BE213т&#09E214о&#07E115в&#04E116а&#02E017р&#00E018а"), new ArrayList<>());
        Menu.createItem(inventory, 3, Material.GREEN_STAINED_GLASS_PANE, 1, Message.translateColorCodes("&#02B000К&#02B105о&#02B30Aл&#02B40Eи&#02B513ч&#02B718е&#02B81Dс&#01B922т&#01BB26в&#01BC2Bо&#01BD30: &#01C03A+&#01C13E1 &#01C448е&#01C54Dд&#01C751. &#00C95Bт&#00CB60о&#00CC65в&#00CD69а&#00CF6Eр&#00D073а"), new ArrayList<>());
        createItemFromSQL(inventory, itemStack, loreItem, 2);
        Menu.createItem(inventory, 1, Material.BROWN_STAINED_GLASS_PANE, 1, Message.translateColorCodes("&#D41900К&#D41900о&#D41B00л&#D41D00и&#D31F00ч&#D32000е&#D32200с&#D32400т&#D22600в&#D22800о&#D22A00: &#D22E00-&#D12F001 &#D13300е&#D13500д&#D13700. &#D03B00т&#D03D00о&#D03E00в&#CF4000а&#CF4200р&#CF4400а"), new ArrayList<>());
        Menu.createItem(inventory, 0, Material.RED_STAINED_GLASS_PANE, 1, Message.translateColorCodes("&#EE0000К&#EE0000о&#EE0200л&#EE0300и&#ED0500ч&#ED0700е&#ED0800с&#ED0A00т&#EC0C00в&#EC0D00о&#EC0F00: &#EC1200-&#EB14008 &#EB1700е&#EB1900д&#EB1A00. &#EA1E00т&#EA1F00о&#EA2100в&#E92300а&#E92400р&#E92600а"), new ArrayList<>());
        return inventory;
    }

    public static void acceptSellValue(ShowPiece data, Inventory inventory, ItemStack itemStack, List<String> loreItem, int amount) {
        List<String> loreConfig = Auction.getSettings().getConfig().getStringList("Items.AcceptSellValue.ShowPieceLore");
        for (String line : loreConfig) {
            String replacedLine = line
                    .replace("{category}", getCategoryDisplayName(data.getCategory()))
                    .replace("{seller}", Objects.requireNonNull(Bukkit.getPlayer(UUID.fromString(data.getSeller()))).getName())
                    .replace("{total_price}", formatFloat(data.getUnit() * amount))
                    .replace("{unit_price}", formatFloat(data.getUnit()))
                    .replace("{expire_time}", toFormattedTime(data.getTime() + Auction.getSettings().getConfig().getLong("intervalMillis.slot")));
            loreItem.add(Message.translateColorCodes(replacedLine));
        }
        if (!Objects.requireNonNull(ItemFactory.stringToItem(data.getItem()).getItemMeta()).hasAttributeModifiers())
            loreItem.add(Message.translateColorCodes(" "));
        createItemFromSQL(inventory, itemStack, loreItem, 2);
    }
    public static void createItemFromSQL(Inventory inventory, ItemStack itemStack, List<String> loreItem, int slot) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;
        int durability = 0;
        if (meta instanceof Damageable)
            durability = ((Damageable) meta).getDamage();
        Color potionColor = null;
        List<PotionEffect> potionEffects = new ArrayList<>();
        if (meta instanceof PotionMeta potionMeta) {
            potionEffects = new ArrayList<>(potionMeta.getCustomEffects());
            potionColor = potionMeta.getColor();
        }
        List<ItemStack> shulkerContents = new ArrayList<>();
        if (itemStack.getType().name().contains("SHULKER_BOX") && meta instanceof BlockStateMeta blockMeta)
            if (blockMeta.getBlockState() instanceof ShulkerBox shulker)
                for (ItemStack item : shulker.getInventory().getContents())
                    shulkerContents.add(item != null ? item.clone() : null);
        List<FireworkEffect> fireworkEffects = new ArrayList<>();
        if (meta instanceof FireworkMeta fireworkMeta)
            fireworkEffects = new ArrayList<>(fireworkMeta.getEffects());
        List<Pattern> patterns = new ArrayList<>();
        if (meta instanceof BannerMeta bannerMeta)
            patterns = new ArrayList<>(bannerMeta.getPatterns());
        String texture = null;
        if (meta instanceof SkullMeta skullMeta) {
            GameProfile profile = getProfile(skullMeta);
            if (profile != null)
                texture = profile.getProperties().get("textures").stream()
                        .findFirst()
                        .map(Property::getValue)
                        .orElse(null);
        }
        ItemFlag[] modifiers = meta.getItemFlags().toArray(new ItemFlag[0]);
        Menu.createItemGPT( inventory, slot, itemStack.getType(), itemStack.getAmount(), meta.hasDisplayName() ? meta.getDisplayName() : null, loreItem, modifiers, new HashMap<>(itemStack.getEnchantments()), potionEffects, potionColor, shulkerContents, fireworkEffects, patterns, texture, durability );
    }

    private static GameProfile getProfile(SkullMeta skullMeta) {
        try {
            Field profileField = skullMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            return (GameProfile) profileField.get(skullMeta);
        } catch (Exception e) {
            return null;
        }
    }
}
