package server.variplex.Auction.Tools;

import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.AbstractMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Holder {
    private static Player holder = null;
    private final int category, mode, currentPage;
    private final String status;
    private final boolean isSellersItems, isExpiredItems;
    private final String isPlayersItems;

    private static final Map<UUID, Map.Entry<Integer, Integer>> FilterCategoryPlayerAuction = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> PagePlayerAuction = new ConcurrentHashMap<>();

    public Holder(Player holder, int category, int mode, int currentPage, String status, boolean isSellersItems, boolean isExpiredItems, @Nullable String isPlayersItems) {
        Holder.holder = holder;
        this.category = category;
        this.mode = mode;
        this.currentPage = currentPage;
        this.status = status;
        this.isSellersItems = isSellersItems;
        this.isExpiredItems = isExpiredItems;
        this.isPlayersItems = isPlayersItems;

        FilterCategoryPlayerAuction.put(holder.getUniqueId(), new AbstractMap.SimpleEntry<>(0, 0));
        PagePlayerAuction.put(holder.getUniqueId(), 1);
    }

    public Player getHolder() { return holder; }
    public int getCategory() { return category; }
    public int getMode() { return mode; }
    public int getCurrentPage() { return currentPage; }
    public String getStatus() { return status; }
    public boolean isSellersItems() { return isSellersItems; }
    public boolean isExpiredItems() { return isExpiredItems; }
    public String getIsPlayersItems() { return isPlayersItems; }

    public static void PlusOneFilter(int max) {
        Map.Entry<Integer, Integer> FilterCategory = FilterCategoryPlayerAuction.get(holder.getUniqueId());
        if (FilterCategory.getKey() - 1 < 0) FilterCategoryPlayerAuction.replace(holder.getUniqueId(), new AbstractMap.SimpleEntry<>(0, FilterCategory.getValue()));
        else FilterCategoryPlayerAuction.replace(holder.getUniqueId(), new AbstractMap.SimpleEntry<>(FilterCategory.getKey() + 1, FilterCategory.getValue()));
    }
    public static void MinusOneFilter(int max) {
        Map.Entry<Integer, Integer> FilterCategory = FilterCategoryPlayerAuction.get(holder.getUniqueId());
        if (FilterCategory.getKey() - 1 < 0) FilterCategoryPlayerAuction.replace(holder.getUniqueId(), new AbstractMap.SimpleEntry<>(max, FilterCategory.getValue()));
        else FilterCategoryPlayerAuction.replace(holder.getUniqueId(), new AbstractMap.SimpleEntry<>(FilterCategory.getKey() - 1, FilterCategory.getValue()));
    }
    public static void PlusOneCategory(int max) {
        Map.Entry<Integer, Integer> FilterCategory = FilterCategoryPlayerAuction.get(holder.getUniqueId());
        if (FilterCategory.getValue() + 1 > max) FilterCategoryPlayerAuction.replace(holder.getUniqueId(), new AbstractMap.SimpleEntry<>(FilterCategory.getKey(), 0));
        else FilterCategoryPlayerAuction.replace(holder.getUniqueId(), new AbstractMap.SimpleEntry<>(FilterCategory.getKey(), FilterCategory.getValue() + 1));
    }
    public static void MinusOneCategory(int max) {
        Map.Entry<Integer, Integer> FilterCategory = FilterCategoryPlayerAuction.get(holder.getUniqueId());
        if (FilterCategory.getValue() - 1 < 0) FilterCategoryPlayerAuction.replace(holder.getUniqueId(), new AbstractMap.SimpleEntry<>(FilterCategory.getKey(), max));
        else FilterCategoryPlayerAuction.replace(holder.getUniqueId(), new AbstractMap.SimpleEntry<>(FilterCategory.getKey(), FilterCategory.getValue() - 1));
    }
    public static void PlusOneCurrentPage(int totalPage) { PagePlayerAuction.replace(holder.getUniqueId(), Math.min(PagePlayerAuction.get(holder.getUniqueId()) + 1, totalPage)); }
    public static void MinusOneCurrentPage() { PagePlayerAuction.replace(holder.getUniqueId(), Math.max(PagePlayerAuction.get(holder.getUniqueId()) - 1, 1)); }
}
