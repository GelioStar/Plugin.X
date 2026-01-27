package server.variplex.Auction.Commands;

import all.api.UTILS.Command;
import all.api.UTILS.Menu;
import all.api.UTILS.Message;
import com.google.common.collect.Lists;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import server.variplex.Auction.GUI.AuctionGUI;
import server.variplex.Auction.SQL.AuctionCore;
import server.variplex.Auction.SQL.SQLite;
import server.variplex.Auction.SQL.ShowPiece;
import server.variplex.Auction.Tools.BookManager;
import server.variplex.Auction.Tools.ItemFactory;
import server.variplex.Auction.Tools.JSON_Parse;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static server.variplex.Auction.GUI.AuctionGUI.createItemFromSQL;
import static server.variplex.Auction.SQL.SQLite.*;

public class Auction extends Command implements Listener {
    private static final Map<String, Integer> permissionSlotsMap = new HashMap<>();
    private final Map<Player, Integer> acceptSell = new HashMap<>();
    private static final Map<UUID, Map.Entry<Integer, Integer>> FilterCategoryPlayerAuction = new ConcurrentHashMap<>();
    private static final Map<UUID, Map.Entry<Boolean, Boolean>> SellersExpiredPlayerAuction = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> PagePlayerAuction = new HashMap<>();
    public Auction() {
        super("ah", server.variplex.Auction.Auction.getInstance().getClass());
        permissionSlotsMap.put("ah.slots.150", 150);
        permissionSlotsMap.put("ah.slots.30", 30);
        permissionSlotsMap.put("ah.slots.25", 25);
        permissionSlotsMap.put("ah.slots.22", 22);
        permissionSlotsMap.put("ah.slots.20", 20);
        permissionSlotsMap.put("ah.slots.18", 18);
        permissionSlotsMap.put("ah.slots.15", 15);
        permissionSlotsMap.put("ah.slots.12", 12);
        permissionSlotsMap.put("ah.slots.10", 10);
        permissionSlotsMap.put("ah.slots.9", 9);
        permissionSlotsMap.put("ah.slots.7", 7);
        permissionSlotsMap.put("ah.slots.5", 5);
    }

    private static final int FLAG_i = 1; // 00000001 - от ignore (игнорировать атрибуты) — продать все предметы с указанным ID без учёта зачарований, эффектов и т. д.
    private static final int FLAG_u = 1 << 1; // 00000010 - от unit (штучно) — продать предмет, задав цену за один экземпляр, а не за весь лот целиком
    private static final int FLAG_m = 1 << 2; // 00000100 - от match (соответствие) — продать все предметы с указанным ID с учётом зачарований, эффектов и т. д.

    public record CommandContext(Player player, ItemStack item, float price, int flags) {
        public float calculatePrice(ItemStack item, float basePrice, int flags) {
            if ((flags & FLAG_u) != 0) return basePrice * item.getAmount();
            return basePrice;
        }
    }

    private static final Map<Integer, Consumer<Auction.CommandContext>> flagHandlers = new LinkedHashMap<>() {{
        put(FLAG_i | FLAG_u | FLAG_m, (consumer) -> (consumer.player).sendMessage(Message.translateColorCodes(Objects.requireNonNull(server.variplex.Auction.Auction.getSettings().getConfig().getString("command_messages.errorSlotsFull")))));
        put(FLAG_i | FLAG_m, (consumer) -> (consumer.player).sendMessage(Message.translateColorCodes(Objects.requireNonNull(server.variplex.Auction.Auction.getSettings().getConfig().getString("command_messages.errorSlotsFull")))));
        put(FLAG_u | FLAG_m, (consumer) -> {
            Player player = consumer.player;
            ItemStack baseItem = consumer.item;
            List<ItemStack> collectItems = collectItems(player, baseItem, consumer.flags);
            int maxSlots = getMaxSlots(player);
            int usedSlots = SQLite.getSlots(String.valueOf(player.getUniqueId()));
            int availableSlots = maxSlots - usedSlots;
            for (ItemStack IS : collectItems) {
                if (availableSlots <= 0) {
                    errorMessage(player, String.valueOf(maxSlots));
                    break;
                }
                float price = consumer.calculatePrice(baseItem, consumer.price, consumer.flags);
                price = Math.min(price, server.variplex.Auction.Auction.getSettings().getConfig().getInt("coinsValue.maxValue"));
                ItemStack copy = IS.clone(); // сохранить оригинальные NBT
                server.variplex.Auction.Auction.getAuctionCore().sellShowPiece(String.valueOf(player.getUniqueId()), copy, price);
                player.getInventory().remove(IS);
                availableSlots--;
                successShowpieceSlot(player, baseItem, baseItem.getAmount(), consumer.calculatePrice(baseItem, consumer.price, consumer.flags));
            }
        });
        put(FLAG_i | FLAG_u, (consumer) -> {
            Player player = consumer.player;
            ItemStack baseItem = consumer.item;
            List<ItemStack> collectItems = collectItems(player, baseItem, consumer.flags);
            int maxSlots = getMaxSlots(player);
            int usedSlots = SQLite.getSlots(String.valueOf(player.getUniqueId()));
            int availableSlots = maxSlots - usedSlots;
            for (ItemStack IS : collectItems) {
                if (availableSlots <= 0) {
                    errorMessage(player, String.valueOf(maxSlots));
                    break;
                }
                float price = consumer.calculatePrice(baseItem, consumer.price, consumer.flags);
                price = Math.min(price, server.variplex.Auction.Auction.getSettings().getConfig().getInt("coinsValue.maxValue"));
                ItemStack copy = IS.clone(); // сохранить оригинальные NBT
                server.variplex.Auction.Auction.getAuctionCore().sellShowPiece(String.valueOf(player.getUniqueId()), copy, price);
                player.getInventory().remove(IS);
                availableSlots--;
                successShowpieceSlot(player, baseItem, baseItem.getAmount(), consumer.calculatePrice(baseItem, consumer.price, consumer.flags));
            }
        });
        put(FLAG_i, (consumer) -> {
            Player player = consumer.player;
            ItemStack baseItem = consumer.item;
            List<ItemStack> collectItems = collectItems(player, baseItem, consumer.flags);
            int maxSlots = getMaxSlots(player);
            int usedSlots = SQLite.getSlots(String.valueOf(player.getUniqueId()));
            int availableSlots = maxSlots - usedSlots;
            for (ItemStack IS : collectItems) {
                if (availableSlots <= 0) {
                    errorMessage(player, String.valueOf(maxSlots));
                    break;
                }
                float price = consumer.calculatePrice(baseItem, consumer.price, consumer.flags);
                price = Math.min(price, server.variplex.Auction.Auction.getSettings().getConfig().getInt("coinsValue.maxValue"));
                ItemStack copy = IS.clone(); // сохранить оригинальные NBT
                server.variplex.Auction.Auction.getAuctionCore().sellShowPiece(String.valueOf(player.getUniqueId()), copy, price);
                player.getInventory().remove(IS);
                availableSlots--;
                successShowpieceSlot(player, baseItem, baseItem.getAmount(), consumer.calculatePrice(baseItem, consumer.price, consumer.flags));
            }
        });
        put(FLAG_u, (consumer) -> {
            Player player = consumer.player;
            ItemStack baseItem = player.getInventory().getItemInMainHand();
            int maxSlots = getMaxSlots(player);
            int usedSlots = SQLite.getSlots(String.valueOf(player.getUniqueId()));
            int availableSlots = maxSlots - usedSlots;
            if (availableSlots <= 0) {
                errorMessage(player, String.valueOf(maxSlots));
                return;
            }
            float price = consumer.calculatePrice(baseItem, consumer.price, consumer.flags);
            price = Math.min(price, server.variplex.Auction.Auction.getSettings().getConfig().getInt("coinsValue.maxValue"));
            ItemStack copy = baseItem.clone();
            server.variplex.Auction.Auction.getAuctionCore().sellShowPiece(String.valueOf(player.getUniqueId()), copy, price);
            baseItem.setType(Material.AIR);
            successShowpieceSlot(player, baseItem, baseItem.getAmount(), consumer.calculatePrice(baseItem, consumer.price, consumer.flags));
        });
        put(FLAG_m, (consumer) -> {
            Player player = consumer.player;
            ItemStack baseItem = consumer.item;
            List<ItemStack> collectItems = collectItems(player, baseItem, consumer.flags);
            int maxSlots = getMaxSlots(player);
            int usedSlots = SQLite.getSlots(String.valueOf(player.getUniqueId()));
            int availableSlots = maxSlots - usedSlots;
            for (ItemStack IS : collectItems) {
                if (availableSlots <= 0) {
                    errorMessage(player, String.valueOf(maxSlots));
                    break;
                }
                float price = consumer.calculatePrice(baseItem, consumer.price, consumer.flags);
                price = Math.min(price, server.variplex.Auction.Auction.getSettings().getConfig().getInt("coinsValue.maxValue"));
                ItemStack copy = IS.clone(); // сохранить оригинальные NBT
                server.variplex.Auction.Auction.getAuctionCore().sellShowPiece(String.valueOf(player.getUniqueId()), copy, price);
                player.getInventory().remove(IS);
                availableSlots--;
                successShowpieceSlot(player, baseItem, baseItem.getAmount(), consumer.calculatePrice(baseItem, consumer.price, consumer.flags));
            }
        });
    }};

    // Метод для сбора предметов по флагам
    private static List<ItemStack> collectItems(Player player, ItemStack baseItem, int flags) {
        List<ItemStack> items = new ArrayList<>();
        boolean ignoreMeta = (flags & FLAG_i) != 0;
        boolean exactMatch = (flags & FLAG_m) != 0;

        for (ItemStack item : player.getInventory()) {
            if (item == null || item.getType() == Material.AIR) continue;
            if (isItemMatch(baseItem, item, ignoreMeta, exactMatch))
                items.add(item.clone());
        }
        return items;
    }

    // Проверка соответствия предметов
    private static boolean isItemMatch(ItemStack a, ItemStack b, boolean ignoreMeta, boolean exactMatch) {
        if (a.getType() != b.getType()) return false;
        if (ignoreMeta) return true;

        return exactMatch ?
                a.isSimilar(b) : // Полное совпадение метаданных
                (a.getType() == b.getType()); // Сравнение без NBT
    }
//    private static boolean compareBasicAttributes(ItemStack a, ItemStack b) {
//        if (a == null || b == null) return false;
//        if (a.getType() != b.getType()) return false;
//        boolean aHasMeta = a.hasItemMeta();
//        boolean bHasMeta = b.hasItemMeta();
//        if (aHasMeta != bHasMeta) return false;
//        if (!aHasMeta) return true;
//        ItemMeta metaA = a.getItemMeta();
//        ItemMeta metaB = b.getItemMeta();
//        if (metaA == null || metaB == null) return false;
//        if (metaA.hasDisplayName() != metaB.hasDisplayName()) return false;
//        if (metaA.hasDisplayName() && !metaA.getDisplayName().equals(metaB.getDisplayName())) return false;
//        if (metaA.hasLore() != metaB.hasLore()) return false;
//        if (metaA.hasLore() && !Objects.equals(metaA.getLore(), metaB.getLore())) return false;
//        return metaA.getEnchants().equals(metaB.getEnchants());
//    }

    // Получение максимальных слотов из пермишенов
    private static int getMaxSlots(Player player) {
        for (Map.Entry<String, Integer> entry : permissionSlotsMap.entrySet())
            if (player.hasPermission(entry.getKey()))
                return entry.getValue();
        return -1; // Дефолтное значение
    }

    @Override
    public void execute(CommandSender sender, String s, String[] args) {
        if (!sender.hasPermission("C8Au.commands.ah")) {
            sender.sendMessage(Message.translateColorCodes(Objects.requireNonNull(server.variplex.Auction.Auction.getSettings().getConfig().getString("command_messages.errorMessage"))));
            return;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(Message.translateColorCodes(Objects.requireNonNull(server.variplex.Auction.Auction.getSettings().getConfig().getString("command_messages.errorConsoleMessage"))));
            return;
        }
        if(args.length == 0) {
            ((Player) sender).openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) sender, 0, 0, "0", 1, false, false, null)));
            FilterCategoryPlayerAuction.put(Objects.requireNonNull(((Player) sender).getPlayer()).getUniqueId(), new AbstractMap.SimpleEntry<>(0, 0));
            SellersExpiredPlayerAuction.put(Objects.requireNonNull(((Player) sender).getPlayer()).getUniqueId(), new AbstractMap.SimpleEntry<>(false, false));
            PagePlayerAuction.put(Objects.requireNonNull(((Player) sender).getPlayer()).getUniqueId(), 1);
            int usedSlots = SQLite.getSlots(String.valueOf(((Player) sender).getUniqueId()));
//            sender.sendMessage(usedSlots + " : " + getMaxSlots((Player) sender));
        }
        if(args.length > 0) {

            if (Objects.equals(args[0], "sell")) {
                if (!(sender.hasPermission("C8Au.commands.ah.sell")) || args.length == 1) {
                    sender.sendMessage(Message.translateColorCodes(Objects.requireNonNull(server.variplex.Auction.Auction.getSettings().getConfig().getString("command_messages.errorMessage"))));
                    return;
                }
                if (args[1] == null) {
                    sender.sendMessage(Message.translateColorCodes(Objects.requireNonNull(server.variplex.Auction.Auction.getSettings().getConfig().getString("command_messages.errorCoinMessage"))));
                    return;
                }
                if (((Player) sender).getInventory().getItemInMainHand().getItemMeta() == null) {
                    sender.sendMessage(Message.translateColorCodes(Objects.requireNonNull(server.variplex.Auction.Auction.getSettings().getConfig().getString("command_messages.errorItemMessage"))));
                    return;
                }
                float price;
                try {
                    price = Integer.parseInt(args[1]);
                    if (price < server.variplex.Auction.Auction.getSettings().getConfig().getInt("coinsValue.minValue") || price > server.variplex.Auction.Auction.getSettings().getConfig().getInt("coinsValue.maxValue")) {
                        sender.sendMessage(Message.translateColorCodes(Objects.requireNonNull(server.variplex.Auction.Auction.getSettings().getConfig().getString("command_messages.errorCoinMessage"))));
                        return;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(Message.translateColorCodes(Objects.requireNonNull(server.variplex.Auction.Auction.getSettings().getConfig().getString("command_messages.errorCoinMessage"))));
                    return;
                }
                int flagsMask = 0;
                for (int i = 2; i < args.length; i++)
                    if (args[i].startsWith("-"))
                        for (char c : args[i].substring(1).toCharArray())
                            switch (c) {
                                case 'i' -> flagsMask |= FLAG_i;
                                case 'u' -> flagsMask |= FLAG_u;
                                case 'm' -> flagsMask |= FLAG_m;
                                // ... остальные флаги
                                default -> {
                                    String message = Objects.requireNonNull(server.variplex.Auction.Auction.getSettings().getConfig().getString("command_messages.errorUnknownFlagMessage"));
                                    message = message.replace("{flag}", String.valueOf(c));
                                    sender.sendMessage(Message.translateColorCodes(message));
                                    return;
                                }
                            }

                if (flagsMask > 0) {
                    CommandContext commandContext = new CommandContext(((Player) sender).getPlayer(), ((Player) sender).getInventory().getItemInMainHand(), price, flagsMask);
                    flagHandlers.get(flagsMask).accept(commandContext);
                } else {
                    for (Map.Entry<String, Integer> entry : permissionSlotsMap.entrySet()) {
                        String permission = entry.getKey();
                        int slotLimit = entry.getValue();
                        if (sender.hasPermission(permission)) {
                            int playerSlots = SQLite.getSlots(Objects.requireNonNull(((Player) sender).getPlayer()).getName());
                            if (playerSlots < slotLimit) {
                                server.variplex.Auction.Auction.getAuctionCore().sellShowPiece(String.valueOf(Objects.requireNonNull(((Player) sender).getPlayer()).getUniqueId()), ((Player) sender).getInventory().getItemInMainHand(), price);
                                successShowpieceSlot(sender, Objects.requireNonNull(((Player) sender).getInventory().getItemInMainHand()), ((Player) sender).getInventory().getItemInMainHand().getAmount(), price);
                                ((Player) sender).getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                            } else {
                                errorMessage(sender, String.valueOf(slotLimit));
                                return;
                            }
                            return;
                        }
                    }
                }
            } else if (Objects.equals(args[0], "player")) {
                if (!(sender.hasPermission("C8Au.commands.ah.player"))) {
                    sender.sendMessage(Message.translateColorCodes(Objects.requireNonNull(server.variplex.Auction.Auction.getSettings().getConfig().getString("command_messages.errorMessage"))));
                    return;
                }
                if (args[1] == null) {
                    sender.sendMessage(Message.translateColorCodes(Objects.requireNonNull(server.variplex.Auction.Auction.getSettings().getConfig().getString("command_messages.errorPlayer"))));
                    return;
                }
                String seller = args[1];
                if(seller.equals(sender.getName())) {
                    ((Player) sender).openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) sender, 0, 0, "0", 1, true, false, String.valueOf(Objects.requireNonNull(Bukkit.getPlayer(seller)).getUniqueId()))));
                    SellersExpiredPlayerAuction.put(Objects.requireNonNull(((Player) sender).getPlayer()).getUniqueId(), new AbstractMap.SimpleEntry<>(true, false));
                } else {
                    ((Player) sender).openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) sender, 0, 0, "0", 1, false, false, String.valueOf(Objects.requireNonNull(Bukkit.getPlayer(seller)).getUniqueId()))));
                    SellersExpiredPlayerAuction.put(Objects.requireNonNull(((Player) sender).getPlayer()).getUniqueId(), new AbstractMap.SimpleEntry<>(false, false));
                }
                FilterCategoryPlayerAuction.put(Objects.requireNonNull(((Player) sender).getPlayer()).getUniqueId(), new AbstractMap.SimpleEntry<>(0, 0));
                PagePlayerAuction.put(((Player) sender).getUniqueId(), 1);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String auctionHouseTitle = Message.translateColorCodes("&#1C1C1CАукцион (");
        String acceptSellTitle = Message.translateColorCodes("&#1C1C1CПодтверждение покупки");
        String countShowpieceTitle = Message.translateColorCodes("&#1C1C1CКоличество товара");
        String expiredAuctionHouseItems = Message.translateColorCodes("&#1C1C1CНераспроданные товары");
        String customAuctionHouseItems = Message.translateColorCodes("&#1C1C1CАукцион " + event.getWhoClicked().getName());
        String playerAuctionHouseItems = Message.translateColorCodes("&#1C1C1CАукцион ");
        String patternAuctionHouseTitle = Pattern.quote(auctionHouseTitle) + "\\d+/\\d+\\)"; // Экранируем спецсимволы \s*\S+
        String patternPlayerAuctionHouseItems = Pattern.quote(playerAuctionHouseItems) + "\\s*\\S+"; // Экранируем спецсимволы \s*\S+
        String patternAcceptSellTitle = Pattern.quote(acceptSellTitle); // Экранируем спецсимволы
        String patternCountShowpieceTitle = Pattern.quote(countShowpieceTitle); // Экранируем спецсимволы

        if (event.getView().getTitle().matches(patternAuctionHouseTitle)) {
            event.setCancelled(true);
            String[] parts = event.getView().getTitle().split("[()/]");
            int currentPage = Integer.parseInt(parts[1]), totalPage = Integer.parseInt(parts[2]);
            Map.Entry<Integer, Integer> FilterCategory = FilterCategoryPlayerAuction.get(event.getWhoClicked().getUniqueId());
            Map.Entry<Boolean, Boolean> SellersExpired = SellersExpiredPlayerAuction.get(event.getWhoClicked().getUniqueId());
            if (event.getSlot() >= 0 && event.getSlot() < 45) {
                int index = getItemIndexFromPageAndSlot(currentPage, event.getSlot());
                int itemId = SQLite.getShowpieceItemIDByIndex(FilterCategory.getValue(), FilterCategory.getKey(), index, false, null);

                List<ShowPiece> showpieces = SQLite.getShowpiecesItemStackSQLData(event.getWhoClicked().getName(), 0, 0, "0");
                if (event.getCurrentItem() == null || Objects.requireNonNull(event.getCurrentItem()).getType().equals(Material.AIR)) return;
                double coins = Integer.parseInt(PlaceholderAPI.setPlaceholders((Player) event.getWhoClicked(), "%vault_eco_balance%"));
                assert showpieces != null;
                if (Objects.requireNonNull(getShowPieceByID(itemId)).getUnit() <= coins &&
                        Objects.requireNonNull(getShowPieceByID(itemId)).getAmount() > 1 &&
                        !(event.getWhoClicked().getName().equals(Objects.requireNonNull(Bukkit.getPlayer(UUID.fromString(Objects.requireNonNull(getShowPieceByID(itemId)).getSeller()))).getName()))) { // Нужен отклицательный знак
                    ItemStack newItemStack = ItemFactory.stringToItem(Objects.requireNonNull(getShowPieceByID(itemId)).getItem());
                    newItemStack.setAmount(1);
                    acceptSell.put((Player) event.getWhoClicked(), itemId);
                    event.getWhoClicked().openInventory(AuctionGUI.quantityGoods(((Player) event.getWhoClicked()).getPlayer(), newItemStack, new ArrayList<>(), acceptSell, 1));
                } else if (Objects.requireNonNull(getShowPieceByID(itemId)).getUnit() > coins &&
                        Objects.requireNonNull(getShowPieceByID(itemId)).getAmount() > 1 &&
                        !(event.getWhoClicked().getName().equals(Objects.requireNonNull(getShowPieceByID(itemId)).getSeller()))) {
                    ItemStack itemStack = Menu.getItem(event.getInventory(), event.getSlot());
                    Menu.setItem(event.getInventory(), event.getSlot(), Material.BARRIER, 1, Message.translateColorCodes("&#FFFF00Недостаточно средств, мудила!"), new ArrayList<>(), new ItemFlag[0], new HashMap<>());
                    Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> createItemFromSQL(event.getInventory(), itemStack, Objects.requireNonNull(itemStack.getItemMeta()).getLore(), event.getSlot()), 60);
                }
                if (Objects.requireNonNull(getShowPieceByID(itemId)).getPrice() <= coins &&
                        Objects.requireNonNull(getShowPieceByID(itemId)).getAmount() == 1 &&
                        !(event.getWhoClicked().getName().equals(Objects.requireNonNull(Bukkit.getPlayer(UUID.fromString(Objects.requireNonNull(getShowPieceByID(itemId)).getSeller()))).getName()))) {
                    event.getWhoClicked().openInventory(AuctionGUI.acceptSell(((Player) event.getWhoClicked()).getPlayer(), event.getCurrentItem(), Objects.requireNonNull(event.getCurrentItem().getItemMeta()).getLore()));
                    acceptSell.put((Player) event.getWhoClicked(), itemId);
                } else if (Objects.requireNonNull(getShowPieceByID(itemId)).getPrice() > coins && Objects.requireNonNull(getShowPieceByID(itemId)).getAmount() == 1) {
                    ItemStack itemStack = Menu.getItem(event.getInventory(), event.getSlot());
                    Menu.setItem(event.getInventory(), event.getSlot(), Material.BARRIER, 1, Message.translateColorCodes("&#FFFF00Недостаточно средств, мудила!"), new ArrayList<>(), new ItemFlag[0], new HashMap<>());
                    Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> createItemFromSQL(event.getInventory(), itemStack, Objects.requireNonNull(itemStack.getItemMeta()).getLore(), event.getSlot()), 60);
                }
            } else if (event.getSlot() == 45) {
                SellersExpiredPlayerAuction.put(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(!SellersExpired.getKey(), false));
                Map.Entry<Boolean, Boolean> finalSellersExpired = SellersExpiredPlayerAuction.get(event.getWhoClicked().getUniqueId());
                Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), FilterCategory.getValue(), FilterCategory.getKey(), "0", 1, finalSellersExpired.getKey(), false, finalSellersExpired.getKey() ? String.valueOf(event.getWhoClicked().getUniqueId()) : null))), 1L);
            } else if (event.getSlot() == 46) {
                // Я инвертрую значение у Expired, чтобы можно было открывать соответствуюшую вкладку
                SellersExpiredPlayerAuction.put(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(false, !SellersExpired.getValue()));
                // Я применяю значение и обновляю карту
                Map.Entry<Boolean, Boolean> finalSellersExpired1 = SellersExpiredPlayerAuction.get(event.getWhoClicked().getUniqueId());
                // Открываю инвентарь
                Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(
                        AuctionGUI.openAuctionTab(
                                (Player) event.getWhoClicked(),                 // Кому открывать инвентарь
                                FilterCategory.getValue(),                      //
                                FilterCategory.getKey(),
                                finalSellersExpired1.getValue() ? "-" : "0",
                                1,
                                false,
                                finalSellersExpired1.getValue(),
                                finalSellersExpired1.getValue() ? String.valueOf(event.getWhoClicked().getUniqueId()) : null)))
                        , 1L);
            } else if (event.getSlot() == 47) {
                Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), FilterCategory.getValue(), FilterCategory.getKey(), "0", (SellersExpired.getKey() || SellersExpired.getValue()) ? 1 : PagePlayerAuction.get(event.getWhoClicked().getUniqueId()), false, false, null))), 1L);
            } else if (event.getSlot() == 48) {
                if (PagePlayerAuction.get(event.getWhoClicked().getUniqueId()) - 1 < 1) return;
                PagePlayerAuction.put(event.getWhoClicked().getUniqueId(), PagePlayerAuction.get(event.getWhoClicked().getUniqueId()) - 1);
                Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), FilterCategory.getValue(), FilterCategory.getKey(), "0", PagePlayerAuction.get(event.getWhoClicked().getUniqueId()), false, false, null))), 1L);
            } else if (event.getSlot() == 50) {
                if (PagePlayerAuction.get(event.getWhoClicked().getUniqueId()) + 1 > totalPage) return;
                PagePlayerAuction.put(event.getWhoClicked().getUniqueId(), PagePlayerAuction.get(event.getWhoClicked().getUniqueId()) + 1);
                Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), FilterCategory.getValue(), FilterCategory.getKey(), "0", PagePlayerAuction.get(event.getWhoClicked().getUniqueId()), false, false, null))), 1L);
            } else if (event.getSlot() == 51) {
                if(event.getClick().equals(ClickType.LEFT)) {
                    if (FilterCategory.getKey() + 1 > 5) {
                        FilterCategoryPlayerAuction.replace(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(0, FilterCategory.getValue()));
                        Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), FilterCategory.getValue(), 0, "0", PagePlayerAuction.get(event.getWhoClicked().getUniqueId()), false, false, null))), 1L);
                    }
                    else {
                        FilterCategoryPlayerAuction.replace(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(FilterCategory.getKey() + 1, FilterCategory.getValue()));
                        Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), FilterCategory.getValue(), FilterCategory.getKey() + 1, "0", PagePlayerAuction.get(event.getWhoClicked().getUniqueId()), false, false, null))), 1L);
                    }
                } else if(event.getClick().equals(ClickType.RIGHT)) {
                    if (FilterCategory.getKey() - 1 < 0) {
                        FilterCategoryPlayerAuction.replace(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(5, FilterCategory.getValue()));
                        Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), FilterCategory.getValue(), 5, "0", PagePlayerAuction.get(event.getWhoClicked().getUniqueId()), false, false, null))), 1L);
                    }
                    else {
                        FilterCategoryPlayerAuction.replace(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(FilterCategory.getKey() - 1, FilterCategory.getValue()));
                        Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), FilterCategory.getValue(), FilterCategory.getKey() - 1, "0", PagePlayerAuction.get(event.getWhoClicked().getUniqueId()), false, false, null))), 1L);
                    }
                }
            } else if (event.getSlot() == 52) {
                if(event.getClick().equals(ClickType.LEFT)) {
                    if (FilterCategory.getValue() + 1 > 10) {
                        FilterCategoryPlayerAuction.replace(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(FilterCategory.getKey(), 0));
                        Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), 0, FilterCategory.getKey(), "0", PagePlayerAuction.get(event.getWhoClicked().getUniqueId()), false, false, null))), 1L);
                    }
                    else {
                        FilterCategoryPlayerAuction.replace(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(FilterCategory.getKey(), FilterCategory.getValue() + 1));
                        Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), FilterCategory.getValue() + 1, FilterCategory.getKey(), "0", PagePlayerAuction.get(event.getWhoClicked().getUniqueId()), false, false, null))), 1L);
                    }

                } else if(event.getClick().equals(ClickType.RIGHT)) {
                    if (FilterCategory.getValue() - 1 < 0) {
                        FilterCategoryPlayerAuction.replace(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(FilterCategory.getKey(), 10));
                        Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), 10, FilterCategory.getKey(), "0", PagePlayerAuction.get(event.getWhoClicked().getUniqueId()), false, false, null))), 1L);
                    }
                    else {
                        FilterCategoryPlayerAuction.replace(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(FilterCategory.getKey(), FilterCategory.getValue() - 1));
                        Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), FilterCategory.getValue() - 1, FilterCategory.getKey(), "0", PagePlayerAuction.get(event.getWhoClicked().getUniqueId()), false, false, null))), 1L);
                    }
                }
            } else if (event.getSlot() == 53) {
                event.getWhoClicked().closeInventory();
                ((Player) event.getWhoClicked()).openBook(BookManager.createWikiBook());
            }
        } else if (event.getView().getTitle().matches(patternAcceptSellTitle)) {
            event.setCancelled(true);
            int[] trueSlots = {0, 1, 2, 9, 10, 11, 18, 19, 20};
            if (Arrays.stream(trueSlots).anyMatch(slot -> slot == event.getSlot())) {
                Player player = (Player) event.getWhoClicked();
                double coins = Double.parseDouble(PlaceholderAPI.setPlaceholders(player, "%vault_eco_balance%"));
                Integer slotId = acceptSell.get(player);

                // Проверка на null и получение объекта лота
                ShowPiece showPiece = getShowPieceByID(slotId);
                if (showPiece == null) return;

                ItemStack clickedItem = event.getInventory().getItem(13);
                if (clickedItem == null) return;

                int itemAmount = clickedItem.getAmount();
                long timeThreshold = showPiece.getTime() + server.variplex.Auction.Auction.getSettings().getConfig().getLong("intervalMillis.slot");

                // Основное условие покупки
                if (showPiece.getUnit() * itemAmount <= coins
                        && showPiece.getAmount() > 1
                        && showPiece.getAmount() - itemAmount > 0
                        && !player.getName().equals(Objects.requireNonNull(Bukkit.getPlayer(UUID.fromString(showPiece.getSeller()))).getName())
                        && timeThreshold >= System.currentTimeMillis()) {

                    Connection conn = null;
                    try {
                        conn = SQLite.connect();
                        conn.setAutoCommit(false); // Начало транзакции

                        String message = Message.translateColorCodes(Objects.requireNonNull(server.variplex.Auction.Auction.getSettings().getConfig().getString("command_messages.successBuySlot")));

                        if(Objects.requireNonNull(Bukkit.getPlayer(UUID.fromString(showPiece.getSeller()))).isOnline()) {
                            message = message.replace("{buyerName}", player.getName());
                            ItemMeta meta = ItemFactory.stringToItem(showPiece.getItem()).getItemMeta();
                            assert meta != null;
                            if (!meta.getDisplayName().equals(""))
                                message = message.replace("{itemName}", meta.getDisplayName());
                            else
                                message = message.replace("{itemName}", JSON_Parse.materialTranslations.get(ItemFactory.stringToItem(showPiece.getItem()).getType()));

                            message = message.replace("{amount}", String.valueOf(clickedItem.getAmount()))
                                                            .replace("{price}", String.valueOf(itemAmount * showPiece.getUnit()));
                            Objects.requireNonNull(Bukkit.getPlayer(UUID.fromString(showPiece.getSeller()))).sendMessage(Message.translateColorCodes(message));

                        } else
                        // Первый запрос: Обновление покупателя
                            try (PreparedStatement updateBuyer = conn.prepareStatement(
                                    String.format("UPDATE %s SET buyer = ?, amount = ? WHERE id = ?;", BuyersData))) {
                                updateBuyer.setString(1, player.getName());
                                updateBuyer.setInt(2, showPiece.getAmountValue() + itemAmount);
                                updateBuyer.setInt(3, slotId);
                                updateBuyer.executeUpdate();
                            }


                        // Второй запрос: Обновление предмета
                        ItemStack updatedItem = ItemFactory.stringToItem(showPiece.getItem());
                        updatedItem.setAmount(updatedItem.getAmount() - itemAmount);

                        try (PreparedStatement updateItem = conn.prepareStatement(
                                String.format("UPDATE %s SET item = ? WHERE id = ?", Lots))) {
                            updateItem.setString(1, ItemFactory.itemToString(updatedItem));
                            updateItem.setInt(2, slotId);
                            updateItem.executeUpdate();
                        }

                        // Третий запрос: Обновление данных лота
                        try (PreparedStatement updateLotData = conn.prepareStatement(
                                String.format("UPDATE %s SET amount=?, price=? WHERE id=?", LotsData))) {
                            updateLotData.setInt(1, showPiece.getAmount() - itemAmount);
                            updateLotData.setFloat(2, showPiece.getPrice() - (itemAmount * showPiece.getUnit()));
                            updateLotData.setInt(3, slotId);
                            updateLotData.executeUpdate();
                        }

                        conn.commit(); // Фиксация транзакции

                        // Экономические операции
                        int totalCost = (int) (showPiece.getUnit() * itemAmount);
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                String.format("eco take %s %d", player.getName(), totalCost));
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                String.format("eco give %s %d", Objects.requireNonNull(Bukkit.getPlayer(UUID.fromString(showPiece.getSeller()))).getName(), totalCost));

                        // Выдача предмета
                        ItemStack purchasedItem = ItemFactory.stringToItem(showPiece.getItem());
                        purchasedItem.setAmount(itemAmount);
                        player.getInventory().addItem(purchasedItem);

                        // Обновление интерфейса
                        player.openInventory(getFilterCategoryAuctionInventory(player));
                        acceptSell.remove(player);

                    } catch (SQLException e) {
                        if (conn != null) {
                            try { conn.rollback(); }
                            catch (SQLException ex) { ex.printStackTrace(); }
                        }
                        e.printStackTrace();
                    } finally {
                        if (conn != null) {
                            try {
                                conn.setAutoCommit(true);
                                conn.close();
                            } catch (SQLException e) { System.out.println("Блок TRY-CATCH"); /* Игнорируем */ }
                        }
                    }
                }
                // Обработка полной покупки лота
                else if (showPiece.getPrice() <= coins
                        && showPiece.getAmount() == itemAmount
                        && !player.getName().equals(Objects.requireNonNull(Bukkit.getPlayer(UUID.fromString(showPiece.getSeller()))).getName())
                        && timeThreshold >= System.currentTimeMillis()) {
                    acceptSell.put((Player) event.getWhoClicked(), slotId);

                    new AuctionCore().removeShowPrice(showPiece.getSeller(), slotId, "0", showPieceLambda -> {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), String.format("eco take %s %d", player.getName(), (int) showPieceLambda.getPrice()));
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), String.format("eco give %s %d", Objects.requireNonNull(Bukkit.getPlayer(UUID.fromString(showPieceLambda.getSeller()))).getName(), (int) showPieceLambda.getPrice()));

                        String message = Message.translateColorCodes(Objects.requireNonNull(server.variplex.Auction.Auction.getSettings().getConfig().getString("command_messages.successBuySlot")));
                        if(Objects.requireNonNull(Bukkit.getPlayer(UUID.fromString(showPieceLambda.getSeller()))).isOnline()) {
                            message = message.replace("{buyerName}", player.getName());
                            ItemMeta meta = ItemFactory.stringToItem(showPieceLambda.getItem()).getItemMeta();
                            assert meta != null;
                            if (!meta.getDisplayName().equals(""))
                                message = message.replace("{itemName}", meta.getDisplayName());
                            else
                                message = message.replace("{itemName}", JSON_Parse.materialTranslations.get(ItemFactory.stringToItem(showPieceLambda.getItem()).getType()));

                            message = message.replace("{amount}", String.valueOf(clickedItem.getAmount()))
                                    .replace("{price}", String.valueOf(itemAmount * showPieceLambda.getUnit()));
                            Objects.requireNonNull(Bukkit.getPlayer(UUID.fromString(showPieceLambda.getSeller()))).sendMessage(Message.translateColorCodes(message));
                        }
                        successSlot(player, Objects.requireNonNull(Bukkit.getPlayer(showPieceLambda.getSeller())).getName(), ItemFactory.stringToItem(showPieceLambda.getItem()), showPieceLambda.getAmount(), itemAmount * showPieceLambda.getUnit());
                        player.getInventory().addItem(ItemFactory.stringToItem(showPieceLambda.getItem()));
                        player.openInventory(getFilterCategoryAuctionInventory(player));
                        acceptSell.remove(player);
                    });
                }
            }
            int[] falseSlots = {6, 7, 8, 15, 16, 17, 24, 25, 26};
            if (Arrays.stream(falseSlots).anyMatch(slot -> slot == event.getSlot()))
                Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(getFilterCategoryAuctionInventory((Player) event.getWhoClicked())), 1L);
        } else if (event.getView().getTitle().matches(customAuctionHouseItems)) {
            event.setCancelled(true);
            Map.Entry<Integer, Integer> FilterCategory = FilterCategoryPlayerAuction.get(event.getWhoClicked().getUniqueId());
            Map.Entry<Boolean, Boolean> SellersExpired = SellersExpiredPlayerAuction.get(event.getWhoClicked().getUniqueId());
            if (event.getSlot() >= 0 && event.getSlot() < 45) {
                int index = getItemIndexFromPageAndSlot(1, event.getSlot());
                int itemId = SQLite.getShowpieceItemIDByIndex(FilterCategory.getValue(), FilterCategory.getKey(), index, false, String.valueOf(event.getWhoClicked().getUniqueId()));
                List<ShowPiece> showpieces = SQLite.getShowpiecesItemStackSQLData(event.getWhoClicked().getName(), 0, 0, "0");
                if (event.getCurrentItem() == null || Objects.requireNonNull(event.getCurrentItem()).getType().equals(Material.AIR)) return;
                assert showpieces != null;
                new AuctionCore().removeShowPrice(String.valueOf(event.getWhoClicked().getUniqueId()), itemId, "0", showPiece -> {
                    try {
                        event.getWhoClicked().getInventory().addItem(ItemFactory.stringToItem(Objects.requireNonNull(showPiece.getItem())));
                    } catch (NullPointerException ignored) { System.out.println("[AH] Небольшая проблема при взятии предмета с Аукцион игрока-продавца!"); }
                    Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab(((Player) event.getWhoClicked()), FilterCategory.getValue(), FilterCategory.getKey(), "0", PagePlayerAuction.get(event.getWhoClicked().getUniqueId()), true, false, String.valueOf(event.getWhoClicked().getUniqueId())))), 1L);
                });
            } else if (event.getSlot() == 45) {
                SellersExpiredPlayerAuction.put(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(!SellersExpired.getKey(), false));
                Map.Entry<Boolean, Boolean> finalSellersExpired = SellersExpiredPlayerAuction.get(event.getWhoClicked().getUniqueId());
                Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), FilterCategory.getValue(), FilterCategory.getKey(), "0", 1, finalSellersExpired.getKey(), finalSellersExpired.getValue(), finalSellersExpired.getKey() ? String.valueOf(event.getWhoClicked().getUniqueId()) : null))), 1L);
            } else if (event.getSlot() == 46) {
                SellersExpiredPlayerAuction.put(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(false, !SellersExpired.getValue()));
                Map.Entry<Boolean, Boolean> finalSellersExpired1 = SellersExpiredPlayerAuction.get(event.getWhoClicked().getUniqueId());
                Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), FilterCategory.getValue(), FilterCategory.getKey(), finalSellersExpired1.getValue() ? "-" : "0", 1, finalSellersExpired1.getKey(), finalSellersExpired1.getValue(), String.valueOf(event.getWhoClicked().getUniqueId())))), 1L);
            } else if (event.getSlot() == 47) {
                Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), FilterCategory.getValue(), FilterCategory.getKey(), "0", 1, true, false, String.valueOf(event.getWhoClicked().getUniqueId())))), 1L);
            } else if (event.getSlot() == 51) {
                if(event.getClick().equals(ClickType.LEFT)) {
                    if (FilterCategory.getKey() + 1 > 5) {
                        FilterCategoryPlayerAuction.replace(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(0, FilterCategory.getValue()));
                        Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), FilterCategory.getValue(), 0, "0", PagePlayerAuction.get(event.getWhoClicked().getUniqueId()), false, false, String.valueOf(event.getWhoClicked().getUniqueId())))), 1L);
                    }
                    else {
                        FilterCategoryPlayerAuction.replace(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(FilterCategory.getKey() + 1, FilterCategory.getValue()));
                        Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), FilterCategory.getValue(), FilterCategory.getKey() + 1, "0", PagePlayerAuction.get(event.getWhoClicked().getUniqueId()), false, false, String.valueOf(event.getWhoClicked().getUniqueId())))), 1L);
                    }
                } else if(event.getClick().equals(ClickType.RIGHT)) {
                    if (FilterCategory.getKey() - 1 < 0) {
                        FilterCategoryPlayerAuction.replace(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(5, FilterCategory.getValue()));
                        Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), FilterCategory.getValue(), 5, "0", PagePlayerAuction.get(event.getWhoClicked().getUniqueId()), false, false, String.valueOf(event.getWhoClicked().getUniqueId())))), 1L);
                    }
                    else {
                        FilterCategoryPlayerAuction.replace(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(FilterCategory.getKey() - 1, FilterCategory.getValue()));
                        Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), FilterCategory.getValue(), FilterCategory.getKey() - 1, "0", PagePlayerAuction.get(event.getWhoClicked().getUniqueId()), false, false, String.valueOf(event.getWhoClicked().getUniqueId())))), 1L);
                    }
                }
            } else if (event.getSlot() == 52) {
                if(event.getClick().equals(ClickType.LEFT)) {
                    if (FilterCategory.getValue() + 1 > 10) {
                        FilterCategoryPlayerAuction.replace(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(FilterCategory.getKey(), 0));
                        Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), 0, FilterCategory.getKey(), "0", PagePlayerAuction.get(event.getWhoClicked().getUniqueId()), false, false, String.valueOf(event.getWhoClicked().getUniqueId())))), 1L);
                    }
                    else {
                        FilterCategoryPlayerAuction.replace(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(FilterCategory.getKey(), FilterCategory.getValue() + 1));
                        Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), FilterCategory.getValue() + 1, FilterCategory.getKey(), "0", PagePlayerAuction.get(event.getWhoClicked().getUniqueId()), false, false, String.valueOf(event.getWhoClicked().getUniqueId())))), 1L);
                    }

                } else if(event.getClick().equals(ClickType.RIGHT)) {
                    if (FilterCategory.getValue() - 1 < 0) {
                        FilterCategoryPlayerAuction.replace(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(FilterCategory.getKey(), 10));
                        Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), 10, FilterCategory.getKey(), "0", PagePlayerAuction.get(event.getWhoClicked().getUniqueId()), false, false, String.valueOf(event.getWhoClicked().getUniqueId())))), 1L);
                    }
                    else {
                        FilterCategoryPlayerAuction.replace(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(FilterCategory.getKey(), FilterCategory.getValue() - 1));
                        Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), FilterCategory.getValue() - 1, FilterCategory.getKey(), "0", PagePlayerAuction.get(event.getWhoClicked().getUniqueId()), false, false, String.valueOf(event.getWhoClicked().getUniqueId())))), 1L);
                    }

                }
            } else if (event.getSlot() == 53) {
                event.getWhoClicked().closeInventory();
                ((Player) event.getWhoClicked()).openBook(BookManager.createWikiBook());
            }
        } else if (event.getView().getTitle().matches(patternPlayerAuctionHouseItems)) {
            event.setCancelled(true);
            String[] parts = event.getView().getTitle().split(" ");
            String sellerName = parts[1];
            Map.Entry<Integer, Integer> FilterCategory = FilterCategoryPlayerAuction.get(event.getWhoClicked().getUniqueId());
            Map.Entry<Boolean, Boolean> SellersExpired = SellersExpiredPlayerAuction.get(event.getWhoClicked().getUniqueId());
            if (event.getSlot() >= 0 && event.getSlot() < 45) {
                int index = getItemIndexFromPageAndSlot(1, event.getSlot());
                int itemId = SQLite.getShowpieceItemIDByIndex(FilterCategory.getValue(), FilterCategory.getKey(), index, false, String.valueOf(Objects.requireNonNull(Bukkit.getPlayer(sellerName)).getUniqueId()));

                List<ShowPiece> showpieces = SQLite.getShowpiecesItemStackSQLData(sellerName, 0, 0, "0");
                if (event.getCurrentItem() == null || Objects.requireNonNull(event.getCurrentItem()).getType().equals(Material.AIR)) return;
                double coins = Integer.parseInt(PlaceholderAPI.setPlaceholders((Player) event.getWhoClicked(), "%vault_eco_balance%"));
                assert showpieces != null;
                if (Objects.requireNonNull(getShowPieceByID(itemId)).getUnit() <= coins &&
                        Objects.requireNonNull(getShowPieceByID(itemId)).getAmount() > 1 &&
                        !(event.getWhoClicked().getName().equals(Objects.requireNonNull(Bukkit.getPlayer(UUID.fromString(Objects.requireNonNull(getShowPieceByID(itemId)).getSeller()))).getName()))) { // Нужен отклицательный знак
                    ItemStack newItemStack = ItemFactory.stringToItem(Objects.requireNonNull(getShowPieceByID(itemId)).getItem());
                    newItemStack.setAmount(1);
                    acceptSell.put((Player) event.getWhoClicked(), itemId);
                    event.getWhoClicked().openInventory(AuctionGUI.quantityGoods(((Player) event.getWhoClicked()).getPlayer(), newItemStack, new ArrayList<>(), acceptSell, 1));
                } else if (Objects.requireNonNull(getShowPieceByID(itemId)).getUnit() > coins &&
                        Objects.requireNonNull(getShowPieceByID(itemId)).getAmount() > 1 &&
                        !(event.getWhoClicked().getName().equals(Objects.requireNonNull(getShowPieceByID(itemId)).getSeller()))) {
                    ItemStack itemStack = Menu.getItem(event.getInventory(), event.getSlot());
                    Menu.setItem(event.getInventory(), event.getSlot(), Material.BARRIER, 1, Message.translateColorCodes("&#FFFF00Недостаточно средств, мудила!"), new ArrayList<>(), new ItemFlag[0], new HashMap<>());
                    Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> createItemFromSQL(event.getInventory(), itemStack, Objects.requireNonNull(itemStack.getItemMeta()).getLore(), event.getSlot()), 60);
                }
                if (Objects.requireNonNull(getShowPieceByID(itemId)).getPrice() <= coins &&
                        Objects.requireNonNull(getShowPieceByID(itemId)).getAmount() == 1 &&
                        !(event.getWhoClicked().getName().equals(Objects.requireNonNull(Bukkit.getPlayer(UUID.fromString(Objects.requireNonNull(getShowPieceByID(itemId)).getSeller()))).getName()))) {
                    event.getWhoClicked().openInventory(AuctionGUI.acceptSell(((Player) event.getWhoClicked()).getPlayer(), event.getCurrentItem(), Objects.requireNonNull(event.getCurrentItem().getItemMeta()).getLore()));
                    acceptSell.put((Player) event.getWhoClicked(), itemId);
                } else if (Objects.requireNonNull(getShowPieceByID(itemId)).getPrice() > coins && Objects.requireNonNull(getShowPieceByID(itemId)).getAmount() == 1) {
                    ItemStack itemStack = Menu.getItem(event.getInventory(), event.getSlot());
                    Menu.setItem(event.getInventory(), event.getSlot(), Material.BARRIER, 1, Message.translateColorCodes("&#FFFF00Недостаточно средств, мудила!"), new ArrayList<>(), new ItemFlag[0], new HashMap<>());
                    Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> createItemFromSQL(event.getInventory(), itemStack, Objects.requireNonNull(itemStack.getItemMeta()).getLore(), event.getSlot()), 60);
                }
            } else if (event.getSlot() == 45) {
                SellersExpiredPlayerAuction.put(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(!SellersExpired.getKey(), false));
                Map.Entry<Boolean, Boolean> finalSellersExpired = SellersExpiredPlayerAuction.get(event.getWhoClicked().getUniqueId());
                Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), FilterCategory.getValue(), FilterCategory.getKey(), "0", 1, finalSellersExpired.getKey(), finalSellersExpired.getValue(), finalSellersExpired.getKey() ? String.valueOf(event.getWhoClicked().getUniqueId()) : String.valueOf(Objects.requireNonNull(Bukkit.getPlayer(sellerName)).getUniqueId())))), 1L);
            } else if (event.getSlot() == 46) {
                SellersExpiredPlayerAuction.put(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(false, !SellersExpired.getValue()));
                Map.Entry<Boolean, Boolean> finalSellersExpired1 = SellersExpiredPlayerAuction.get(event.getWhoClicked().getUniqueId());
                Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), FilterCategory.getValue(), FilterCategory.getKey(), finalSellersExpired1.getValue() ? "-" : "0", 1, finalSellersExpired1.getKey(), finalSellersExpired1.getValue(), finalSellersExpired1.getValue() ? String.valueOf(event.getWhoClicked().getUniqueId()) : String.valueOf(Objects.requireNonNull(Bukkit.getPlayer(sellerName)).getUniqueId())))), 1L);
            } else if (event.getSlot() == 47) {
                Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), FilterCategory.getValue(), FilterCategory.getKey(), "0", 1, false, false, String.valueOf(Objects.requireNonNull(Bukkit.getPlayer(sellerName)).getUniqueId())))), 1L);
            } else if (event.getSlot() == 51) {
                if(event.getClick().equals(ClickType.LEFT)) {
                    if (FilterCategory.getKey() + 1 > 5) {
                        FilterCategoryPlayerAuction.replace(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(0, FilterCategory.getValue()));
                        Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), FilterCategory.getValue(), 0, "0", PagePlayerAuction.get(event.getWhoClicked().getUniqueId()), false, false, String.valueOf(Objects.requireNonNull(Bukkit.getPlayer(sellerName)).getUniqueId())))), 1L);
                    }
                    else {
                        FilterCategoryPlayerAuction.replace(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(FilterCategory.getKey() + 1, FilterCategory.getValue()));
                        Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), FilterCategory.getValue(), FilterCategory.getKey() + 1, "0", PagePlayerAuction.get(event.getWhoClicked().getUniqueId()), false, false, String.valueOf(Objects.requireNonNull(Bukkit.getPlayer(sellerName)).getUniqueId())))), 1L);
                    }
                } else if(event.getClick().equals(ClickType.RIGHT)) {
                    if (FilterCategory.getKey() - 1 < 0) {
                        FilterCategoryPlayerAuction.replace(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(5, FilterCategory.getValue()));
                        Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), FilterCategory.getValue(), 5, "0", PagePlayerAuction.get(event.getWhoClicked().getUniqueId()), false, false, String.valueOf(Objects.requireNonNull(Bukkit.getPlayer(sellerName)).getUniqueId())))), 1L);
                    }
                    else {
                        FilterCategoryPlayerAuction.replace(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(FilterCategory.getKey() - 1, FilterCategory.getValue()));
                        Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), FilterCategory.getValue(), FilterCategory.getKey() - 1, "0", PagePlayerAuction.get(event.getWhoClicked().getUniqueId()), false, false, String.valueOf(Objects.requireNonNull(Bukkit.getPlayer(sellerName)).getUniqueId())))), 1L);
                    }
                }
            } else if (event.getSlot() == 52) {
                if(event.getClick().equals(ClickType.LEFT)) {
                    if (FilterCategory.getValue() + 1 > 10) {
                        FilterCategoryPlayerAuction.replace(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(FilterCategory.getKey(), 0));
                        Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), 0, FilterCategory.getKey(), "0", PagePlayerAuction.get(event.getWhoClicked().getUniqueId()), false, false, String.valueOf(Objects.requireNonNull(Bukkit.getPlayer(sellerName)).getUniqueId())))), 1L);
                    }
                    else {
                        FilterCategoryPlayerAuction.replace(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(FilterCategory.getKey(), FilterCategory.getValue() + 1));
                        Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), FilterCategory.getValue() + 1, FilterCategory.getKey(), "0", PagePlayerAuction.get(event.getWhoClicked().getUniqueId()), false, false, String.valueOf(Objects.requireNonNull(Bukkit.getPlayer(sellerName)).getUniqueId())))), 1L);
                    }

                } else if(event.getClick().equals(ClickType.RIGHT)) {
                    if (FilterCategory.getValue() - 1 < 0) {
                        FilterCategoryPlayerAuction.replace(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(FilterCategory.getKey(), 10));
                        Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), 10, FilterCategory.getKey(), "0", PagePlayerAuction.get(event.getWhoClicked().getUniqueId()), false, false, String.valueOf(Objects.requireNonNull(Bukkit.getPlayer(sellerName)).getUniqueId())))), 1L);
                    }
                    else {
                        FilterCategoryPlayerAuction.replace(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(FilterCategory.getKey(), FilterCategory.getValue() - 1));
                        Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), FilterCategory.getValue() - 1, FilterCategory.getKey(), "0", PagePlayerAuction.get(event.getWhoClicked().getUniqueId()), false, false, String.valueOf(Objects.requireNonNull(Bukkit.getPlayer(sellerName)).getUniqueId())))), 1L);
                    }

                }
            }
        } else if (event.getView().getTitle().matches(patternCountShowpieceTitle) && event.getInventory().getType().equals(InventoryType.HOPPER)) {
            event.setCancelled(true);
            ItemStack itemStack = event.getInventory().getItem(2);
            assert itemStack != null;
            if (event.getSlot() == 0) {
                itemStack.setAmount(Math.max(itemStack.getAmount() - 8, 1));

                AuctionGUI.acceptSellValue(Objects.requireNonNull(getShowPieceByID(acceptSell.get((Player) event.getWhoClicked()))), event.getInventory(), itemStack, new ArrayList<>(), itemStack.getAmount());
            } else if (event.getSlot() == 1) {
                itemStack.setAmount(Math.max(itemStack.getAmount() - 1, 1));

                AuctionGUI.acceptSellValue(Objects.requireNonNull(getShowPieceByID(acceptSell.get((Player) event.getWhoClicked()))), event.getInventory(), itemStack, new ArrayList<>(), itemStack.getAmount());
            } else if (event.getSlot() == 3) {
                itemStack.setAmount(Math.min(itemStack.getAmount() + 1, Objects.requireNonNull(getShowPieceByID(acceptSell.get((Player) event.getWhoClicked()))).getAmount()));

                AuctionGUI.acceptSellValue(Objects.requireNonNull(getShowPieceByID(acceptSell.get((Player) event.getWhoClicked()))), event.getInventory(), itemStack, new ArrayList<>(), itemStack.getAmount());
            } else if (event.getSlot() == 4) {
                itemStack.setAmount(Math.min(itemStack.getAmount() + 8, Objects.requireNonNull(getShowPieceByID(acceptSell.get((Player) event.getWhoClicked()))).getAmount()));

                AuctionGUI.acceptSellValue(Objects.requireNonNull(getShowPieceByID(acceptSell.get((Player) event.getWhoClicked()))), event.getInventory(), itemStack, new ArrayList<>(), itemStack.getAmount());
            }
            if (event.getSlot() == 2) {
                Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(AuctionGUI.acceptSell(((Player) event.getWhoClicked()).getPlayer(), event.getInventory().getItem(2), Objects.requireNonNull(Objects.requireNonNull(event.getCurrentItem()).getItemMeta()).getLore())), 1L);
            }
        } else if (event.getView().getTitle().matches(expiredAuctionHouseItems)) {
            event.setCancelled(true);
            Map.Entry<Integer, Integer> FilterCategory = FilterCategoryPlayerAuction.get(event.getWhoClicked().getUniqueId());
            Map.Entry<Boolean, Boolean> SellersExpired = SellersExpiredPlayerAuction.get(event.getWhoClicked().getUniqueId());
            if (event.getSlot() >= 0 && event.getSlot() < 45) {
                int index = getItemIndexFromPageAndSlot(1, event.getSlot());
                int itemId = SQLite.getShowpieceItemIDByIndex(FilterCategory.getValue(), FilterCategory.getKey(), index, true, String.valueOf(event.getWhoClicked().getUniqueId()));
                List<ShowPiece> showpieces = SQLite.getShowpiecesItemStackSQLData(event.getWhoClicked().getName(), 0, 0, "-");
                if (event.getCurrentItem() == null || Objects.requireNonNull(event.getCurrentItem()).getType().equals(Material.AIR)) return;
                assert showpieces != null;
                new AuctionCore().removeShowPrice(String.valueOf(event.getWhoClicked().getUniqueId()), itemId, "-", showPiece -> {
                    try {
                        event.getWhoClicked().getInventory().addItem(ItemFactory.stringToItem(Objects.requireNonNull(showPiece.getItem())));
                    } catch (NullPointerException ignored) { System.out.println("[AH] Небольшая проблема при взятии предмета с Нераспроданные товары!"); }
                    Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab(((Player) event.getWhoClicked()), FilterCategory.getValue(), FilterCategory.getKey(), "-", PagePlayerAuction.get(event.getWhoClicked().getUniqueId()), false, true, String.valueOf(event.getWhoClicked().getUniqueId())))), 1L);
                });
            } else if (event.getSlot() == 45) {
                SellersExpiredPlayerAuction.put(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(!SellersExpired.getKey(), false));
                Map.Entry<Boolean, Boolean> finalSellersExpired = SellersExpiredPlayerAuction.get(event.getWhoClicked().getUniqueId());
                Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), FilterCategory.getValue(), FilterCategory.getKey(), SellersExpired.getValue() ? "-" : "0", 1, finalSellersExpired.getKey(), finalSellersExpired.getValue(), finalSellersExpired.getKey() ? String.valueOf(event.getWhoClicked().getUniqueId()) : null))), 1L);
            } else if (event.getSlot() == 46) {
                SellersExpiredPlayerAuction.put(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(false, !SellersExpired.getValue()));
                Map.Entry<Boolean, Boolean> finalSellersExpired1 = SellersExpiredPlayerAuction.get(event.getWhoClicked().getUniqueId());
                Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), FilterCategory.getValue(), FilterCategory.getKey(), finalSellersExpired1.getValue() ? "-" : "0", 1, finalSellersExpired1.getKey(), finalSellersExpired1.getValue(), finalSellersExpired1.getValue() ? String.valueOf(event.getWhoClicked().getUniqueId()) : null))), 1L);
            } else if (event.getSlot() == 51) {
                if(event.getClick().equals(ClickType.LEFT)) {
                    if (FilterCategory.getKey() + 1 > 5) {
                        FilterCategoryPlayerAuction.replace(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(0, FilterCategory.getValue()));
                        Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), FilterCategory.getValue(), 0, "-", PagePlayerAuction.get(event.getWhoClicked().getUniqueId()), false, true, String.valueOf(event.getWhoClicked().getUniqueId())))), 1L);
                    }
                    else {
                        FilterCategoryPlayerAuction.replace(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(FilterCategory.getKey() + 1, FilterCategory.getValue()));
                        Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), FilterCategory.getValue(), FilterCategory.getKey() + 1, "-", PagePlayerAuction.get(event.getWhoClicked().getUniqueId()), false, true, String.valueOf(event.getWhoClicked().getUniqueId())))), 1L);
                    }
                } else if(event.getClick().equals(ClickType.RIGHT)) {
                    if (FilterCategory.getKey() - 1 < 0) {
                        FilterCategoryPlayerAuction.replace(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(5, FilterCategory.getValue()));
                        Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), FilterCategory.getValue(), 5, "-", PagePlayerAuction.get(event.getWhoClicked().getUniqueId()), false, true, String.valueOf(event.getWhoClicked().getUniqueId())))), 1L);
                    }
                    else {
                        FilterCategoryPlayerAuction.replace(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(FilterCategory.getKey() - 1, FilterCategory.getValue()));
                        Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), FilterCategory.getValue(), FilterCategory.getKey() - 1, "-", PagePlayerAuction.get(event.getWhoClicked().getUniqueId()), false, true, String.valueOf(event.getWhoClicked().getUniqueId())))), 1L);
                    }
                }
            } else if (event.getSlot() == 52) {
                if(event.getClick().equals(ClickType.LEFT)) {
                    if (FilterCategory.getValue() + 1 > 10) {
                        FilterCategoryPlayerAuction.replace(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(FilterCategory.getKey(), 0));
                        Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), 0, FilterCategory.getKey(), "-", PagePlayerAuction.get(event.getWhoClicked().getUniqueId()), false, true, String.valueOf(event.getWhoClicked().getUniqueId())))), 1L);
                    }
                    else {
                        FilterCategoryPlayerAuction.replace(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(FilterCategory.getKey(), FilterCategory.getValue() + 1));
                        Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), FilterCategory.getValue() + 1, FilterCategory.getKey(), "-", PagePlayerAuction.get(event.getWhoClicked().getUniqueId()), false, true, String.valueOf(event.getWhoClicked().getUniqueId())))), 1L);
                    }

                } else if(event.getClick().equals(ClickType.RIGHT)) {
                    if (FilterCategory.getValue() - 1 < 0) {
                        FilterCategoryPlayerAuction.replace(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(FilterCategory.getKey(), 10));
                        Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), 10, FilterCategory.getKey(), "-", PagePlayerAuction.get(event.getWhoClicked().getUniqueId()), false, true, String.valueOf(event.getWhoClicked().getUniqueId())))), 1L);
                    }
                    else {
                        FilterCategoryPlayerAuction.replace(event.getWhoClicked().getUniqueId(), new AbstractMap.SimpleEntry<>(FilterCategory.getKey(), FilterCategory.getValue() - 1));
                        Bukkit.getScheduler().runTaskLater(server.variplex.Auction.Auction.getInstance(), () -> event.getWhoClicked().openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab((Player) event.getWhoClicked(), FilterCategory.getValue() - 1, FilterCategory.getKey(), "-", PagePlayerAuction.get(event.getWhoClicked().getUniqueId()), false, true, String.valueOf(event.getWhoClicked().getUniqueId())))), 1L);
                    }

                }
            } else if (event.getSlot() == 53) {
                event.getWhoClicked().closeInventory();
                ((Player) event.getWhoClicked()).openBook(BookManager.createWikiBook());
            }
        } else event.setCancelled(false);
    }

    public static void errorMessage(CommandSender sender, String replace) {
        String message = Message.translateColorCodes(Objects.requireNonNull(server.variplex.Auction.Auction.getSettings().getConfig().getString("command_messages.errorSlotsFull")));
        message = message.replace("{slots}", replace);
        sender.sendMessage(message);
    }
    public static void successShowpieceSlot(CommandSender sender, ItemStack item, int amount, float price) {
        String message = Message.translateColorCodes(Objects.requireNonNull(server.variplex.Auction.Auction.getSettings().getConfig().getString("command_messages.successShowpieceSlot")));
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        if (!meta.getDisplayName().equals(""))
            message = message.replace("{itemName}", meta.getDisplayName());
        else
            message = message.replace("{itemName}", JSON_Parse.materialTranslations.get(item.getType()));
        message = message.replace("{amount}", String.valueOf(amount))
                .replace("{price}", formatFloat(price));
        sender.sendMessage(message);
    }
    public static void successSlot(Player sender, String seller, ItemStack item, int amount, float price) {
        String message = Message.translateColorCodes(Objects.requireNonNull(server.variplex.Auction.Auction.getSettings().getConfig().getString("command_messages.successShowpieceSlot")));
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        if (!meta.getDisplayName().equals(""))
            message = message.replace("{itemName}", meta.getDisplayName());
        else
            message = message.replace("{itemName}", JSON_Parse.materialTranslations.get(item.getType()));
        message = message.replace("{amount}", String.valueOf(amount))
                .replace("{seller}", seller)
                .replace("{price}", formatFloat(price));
        sender.sendMessage(message);
    }
    public static String formatFloat(float number) {
        if (Float.isNaN(number) || Float.isInfinite(number))
            return "NaN";
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
        numberFormat.setMinimumFractionDigits(0); // Минимум 2 знака (даже для целых чисел)
        numberFormat.setMaximumFractionDigits(2); // Максимум 2 знака после запятой
        return numberFormat.format(number);
    }
    private int getItemIndexFromPageAndSlot(int currentPage, int clickedSlot) {
        final int ITEMS_PER_PAGE = 45;
        return (currentPage - 1) * ITEMS_PER_PAGE + clickedSlot;
    }
    private static Inventory getFilterCategoryAuctionInventory(Player player) {
        Map.Entry<Integer, Integer> FilterCategory = FilterCategoryPlayerAuction.get(player.getUniqueId());
        return Objects.requireNonNull(AuctionGUI.openAuctionTab(player, FilterCategory.getValue(), FilterCategory.getKey(), "0", PagePlayerAuction.get(player.getUniqueId()), false, false, null));
    }

    @EventHandler
    public void onBookClose(InventoryCloseEvent event) {
        if (event.getInventory().getType() != InventoryType.LECTERN) return;
        ItemStack book = event.getInventory().getItem(0);
        if (book == null || book.getType() != Material.WRITTEN_BOOK) return;
        NamespacedKey key = new NamespacedKey(server.variplex.Auction.Auction.getInstance(), "book_id");
        if (!Objects.requireNonNull(book.getItemMeta()).getPersistentDataContainer().has(key, PersistentDataType.STRING)) return;
        String bookId = book.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        assert bookId != null;
        if (!bookId.equals(server.variplex.Auction.Auction.getSettings().getConfig().getString("Wiki.id"))) return;
        Player player = (Player) event.getPlayer();
        Map.Entry<Integer, Integer> FilterCategory = FilterCategoryPlayerAuction.get(player.getUniqueId());
        Map.Entry<Boolean, Boolean> SellersExpired = SellersExpiredPlayerAuction.get(player.getUniqueId());
        player.openInventory(Objects.requireNonNull(AuctionGUI.openAuctionTab(player, FilterCategory.getValue(), FilterCategory.getKey(), (SellersExpired.getValue()) ? "-" : "0", PagePlayerAuction.get(player.getUniqueId()), SellersExpired.getKey(), SellersExpired.getValue(), SellersExpired.getKey() ? player.getUniqueId().toString() : null)));
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1) return Lists.newArrayList("sell", "player");
        return Lists.newArrayList();
    }
}
