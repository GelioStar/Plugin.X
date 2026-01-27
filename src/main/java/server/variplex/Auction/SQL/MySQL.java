package server.variplex.Auction.SQL;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.inventory.ItemStack;
import server.variplex.Auction.Tools.ItemFactory;

import javax.annotation.Nullable;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MySQL implements Database {

    public static final String Lots = "Lots", ItemIDs = "ItemIDs", SellersData = "SellersData", LotsData = "LotsData", BuyersData = "BuyersData";

    private static HikariDataSource src;

    public MySQL(String host, int port, String database, String user, String password) throws SQLException {
        HikariConfig config = new HikariConfig();
        // Добавляем параметры для совместимости
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?allowMultiQueries=true");
        config.setUsername(user);
        config.setPassword(password);
        config.addDataSourceProperty("cachePrepStmts", true);
        config.addDataSourceProperty("prepStmtCacheSize", 250);
        config.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);
        config.addDataSourceProperty("useServerPrepStmts", true);

        src = new HikariDataSource(config);
        try (Connection cn = connect(); Statement st = cn.createStatement()) {
            st.executeUpdate(String.format(
                    "CREATE TABLE IF NOT EXISTS `%s` (" +
                            "`id` INT PRIMARY KEY AUTO_INCREMENT, " +
                            "`item` TEXT NOT NULL, " +
                            "`category` TEXT NOT NULL );" +
                            "" +
                            "CREATE TABLE IF NOT EXISTS `%s` (" +
                            "`id` INT NOT NULL, " +
                            "`item_id` TEXT NOT NULL UNIQUE, " +
                            "FOREIGN KEY (`id`) REFERENCES `%s`(`id`) ON UPDATE CASCADE ON DELETE CASCADE);" +
                            "" +
                            "CREATE TABLE IF NOT EXISTS `%s` (" +
                            "`id` INT NOT NULL, " +
                            "`seller` TEXT NOT NULL, " +
                            "FOREIGN KEY (`id`) REFERENCES `%s`(`id`) ON UPDATE CASCADE ON DELETE CASCADE);" +
                            "" +
                            "CREATE TABLE IF NOT EXISTS `%s` (" +
                            "`id` INT NOT NULL, " +
                            "`amount` INT NOT NULL CHECK (`amount` BETWEEN 0 AND 64), " +
                            "`price` DECIMAL(10, 2) NOT NULL, " +
                            "`unit` DECIMAL(10, 2), " +
                            "`time` BIGINT NOT NULL, " +
                            "`status` TEXT CHECK (`status` IN ('-', '0', '+')), " +
                            "FOREIGN KEY (`id`) REFERENCES `%s`(`id`) ON UPDATE CASCADE ON DELETE CASCADE);" +
                            "" +
                            "CREATE TABLE IF NOT EXISTS `%s` (" +
                            "`id` INT NOT NULL, " +
                            "`buyer` TEXT, " +
                            "`amount` INT CHECK (`amount` BETWEEN 0 AND 64), " +
                            "FOREIGN KEY (`id`) REFERENCES `%s`(`id`) ON UPDATE CASCADE ON DELETE CASCADE);",
                    Lots,
                    ItemIDs, Lots,
                    SellersData, Lots,
                    LotsData, Lots,
                    BuyersData, Lots));
        }
    }
    private static Connection connect() throws SQLException { return src.getConnection(); }

    public static List<ItemStack> getShowpiecesItemStackSQL(String seller, int category, int mode, String status) {
        String[] categories = { "LIKE '%%'", "= 'building'", "= 'decoration'", "= 'combat'", "= 'tools'", "= 'transportation'", "= 'food'", "= 'redstone'", "= 'brewing'", "= 'enchanting'", "= 'miscellaneous'" };
        String[] modes = { "ORDER BY ld.time DESC", "ORDER BY ld.time", "ORDER BY ld.price", "ORDER BY ld.price DESC", "ORDER BY ld.unit", "ORDER BY ld.unit DESC" };
        String sqlTemplate =
                "SELECT " +
                        "l.id, i.item_id, s.seller, l.item, l.category, " +
                        "ld.amount, ld.price, ld.unit, ld.time, ld.status, " +
                        "b.buyer, b.amount AS amountValue " +
                        "FROM `%s` AS l " +
                        "LEFT JOIN `%s` AS i ON l.id = i.id " +
                        "LEFT JOIN `%s` AS s ON l.id = s.id " +
                        "LEFT JOIN `%s` AS ld ON l.id = ld.id " +
                        "LEFT JOIN `%s` AS b ON l.id = b.id " +
                        "WHERE s.seller = ? " +
                        "AND l.category %s " +
                        "AND ld.status = ? " +
                        " %s";  // ORDER BY clause

        String sql = String.format(
                sqlTemplate,
                Lots, ItemIDs, SellersData, LotsData, BuyersData,
                categories[category],
                modes[mode]
        );

        try (Connection c = connect();
             PreparedStatement st = c.prepareStatement(sql)) {

            st.setString(1, seller);
            st.setString(2, status);

            List<ItemStack> result = new ArrayList<>();
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    ShowPiece piece = getShowPiece(rs);
                    if (piece.getItem() != null) {
                        result.add(ItemFactory.stringToItem(piece.getItem()));
                    }
                }
            }
            return result;

        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public static List<ShowPiece> getShowpiecesItemStackSQLData(String seller, int category, int mode, String status) {
        String[] categories = {
                "l.category LIKE '%%'",
                "l.category = 'building'",
                "l.category = 'decoration'",
                "l.category = 'combat'",
                "l.category = 'tools'",
                "l.category = 'transportation'",
                "l.category = 'food'",
                "l.category = 'redstone'",
                "l.category = 'brewing'",
                "l.category = 'enchanting'",
                "l.category = 'miscellaneous'"
        };

        String[] modes = { "ld.time DESC", "ld.time", "ld.price", "ld.price DESC", "ld.unit", "ld.unit DESC" };
        String sql = "SELECT " +
                "l.id, i.item_id, s.seller, l.item, l.category, ld.amount, " +
                "ld.price, ld.unit, ld.time, ld.status, b.buyer, b.amount AS amountValue " +
                "FROM `" + Lots + "` AS l " +
                "LEFT JOIN `" + ItemIDs + "` AS i ON l.id = i.id " +
                "LEFT JOIN `" + SellersData + "` AS s ON l.id = s.id " +
                "LEFT JOIN `" + LotsData + "` AS ld ON l.id = ld.id " +
                "LEFT JOIN `" + BuyersData + "` AS b ON l.id = b.id " +
                "WHERE " + categories[category] + " AND ld.status = ? AND s.seller = ? " +
                "ORDER BY " + modes[mode];

        try (Connection c = connect();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setString(1, status);
            st.setString(2, seller);
            List<ShowPiece> list = new ArrayList<>();
            try (ResultSet set = st.executeQuery()) {
                while (set.next())
                    list.add(getShowPiece(set));
            }
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public static List<ItemStack> getShowpiecesItemStackSQL(int category, int mode, String status) {
        String[] categories = {"l.category LIKE '%%'", "l.category = 'building'", "l.category = 'decoration'",
                "l.category = 'combat'", "l.category = 'tools'", "l.category = 'transportation'",
                "l.category = 'food'", "l.category = 'redstone'", "l.category = 'brewing'",
                "l.category = 'enchanting'", "l.category = 'miscellaneous'"};
        String[] modes = {"ld.time DESC", "ld.time", "ld.price", "ld.price DESC", "ld.unit", "ld.unit DESC"};

        String baseSQL = "SELECT " +
                "l.id, i.item_id, s.seller, l.item, l.category, ld.amount, " +
                "ld.price, ld.unit, ld.time, ld.status, b.buyer, b.amount AS amountValue " +
                "FROM `" + Lots + "` AS l " +
                "LEFT JOIN `" + ItemIDs + "` AS i ON l.id = i.id " +
                "LEFT JOIN `" + SellersData + "` AS s ON l.id = s.id " +
                "LEFT JOIN `" + LotsData + "` AS ld ON l.id = ld.id " +
                "LEFT JOIN `" + BuyersData + "` AS b ON l.id = b.id " +
                "WHERE " + categories[category] + " AND ld.status = ? " +
                "ORDER BY " + modes[mode];

        try (Connection c = connect();
             PreparedStatement st = c.prepareStatement(baseSQL)) {
            st.setString(1, status);
            List<ItemStack> list = new ArrayList<>();
            try (ResultSet set = st.executeQuery()) {
                while (set.next())
                    list.add(ItemFactory.stringToItem(getShowPiece(set).getItem()));
            }
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public static List<ShowPiece> getShowpiecesItemStackSQLData(int category, int mode, String status, @Nullable String sellerUUID) {
        String[] categories = { "l.category LIKE '%%'", "l.category = 'building'", "l.category = 'decoration'",
                "l.category = 'combat'", "l.category = 'tools'", "l.category = 'transportation'",
                "l.category = 'food'", "l.category = 'redstone'", "l.category = 'brewing'",
                "l.category = 'enchanting'", "l.category = 'miscellaneous'" };

        String[] modes = {"ld.time DESC", "ld.time", "ld.price", "ld.price DESC", "ld.unit", "ld.unit DESC"};

        String sql = "SELECT l.id, i.item_id, s.seller, l.item, l.category, ld.amount, " +
                "ld.price, ld.unit, ld.time, ld.status, b.buyer, b.amount AS amountValue " +
                "FROM `" + Lots + "` AS l " +
                "LEFT JOIN `" + ItemIDs + "` AS i ON l.id = i.id " +
                "LEFT JOIN `" + SellersData + "` AS s ON l.id = s.id " +
                "LEFT JOIN `" + LotsData + "` AS ld ON l.id = ld.id " +
                "LEFT JOIN `" + BuyersData + "` AS b ON l.id = b.id " +
                "WHERE " + categories[category] + " AND ld.status = ? ";

        if (sellerUUID != null)
            sql += "AND s.seller = ? ";
        sql += "ORDER BY " + modes[mode];
        try (Connection c = connect();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setString(1, status);
            if (sellerUUID != null)
                st.setString(2, sellerUUID);
            List<ShowPiece> list = new ArrayList<>();
            try (ResultSet set = st.executeQuery()) {
                while (set.next())
                    list.add(getShowPiece(set));
            }
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public static int getSlots(String seller) {
        String query = "SELECT COUNT(*) AS repeat_count FROM `" + SellersData + "` WHERE seller = ?";
        try (Connection c = connect(); PreparedStatement st = c.prepareStatement(query)) {
            st.setString(1, seller);
            try (ResultSet set = st.executeQuery()) {
                return set.next() ? set.getInt("repeat_count") : 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private void save(Connection connection, ShowPiece showPiece) {
        try {
            connection.setAutoCommit(false);
            if (showPiece.getId() <= 0) {
                int lotId = insertIntoTableLots(connection, showPiece);
                showPiece.setId(lotId);
                insertIntoTable(connection, ItemIDs, lotId, showPiece.getItemID());
                insertIntoTable(connection, SellersData, lotId, showPiece.getSeller());
                insertIntoLotsData(connection, lotId, showPiece);
                insertIntoBuyersData(connection, lotId);
            } else {
                updateBuyersData(connection, showPiece);
                updateLotsData(connection, showPiece);
            }
            connection.commit();
        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException(e);
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    private int insertIntoTableLots(Connection connection, ShowPiece showPiece) throws SQLException {
        String sql = "INSERT INTO `" + Lots + "` (item, category) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, showPiece.getItem());
            stmt.setString(2, showPiece.getCategory());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
            throw new SQLException("Failed to get Lot ID");
        }
    }

    private void insertIntoTable(Connection connection, String table, int lotId, String value) throws SQLException {
        String column = table.equals(ItemIDs) ? "item_id" : "seller";
        String sql = "INSERT INTO `" + table + "` (id, `" + column + "`) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, lotId);
            stmt.setString(2, value);
            stmt.executeUpdate();
        }
    }

    private void insertIntoLotsData(Connection connection, int lotId, ShowPiece showPiece) throws SQLException {
        String sql = "INSERT INTO `" + LotsData + "` (id, amount, price, unit, time, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, lotId);
            stmt.setInt(2, showPiece.getAmount());
            stmt.setDouble(3, showPiece.getPrice());
            stmt.setDouble(4, showPiece.getPrice() / showPiece.getAmount());
            stmt.setLong(5, showPiece.getTime());
            stmt.setString(6, showPiece.getStatus());
            stmt.executeUpdate();
        }
    }

    private void insertIntoBuyersData(Connection connection, int lotId) throws SQLException {
        String sql = "INSERT INTO `" + BuyersData + "` (id, buyer, amount) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, lotId);
            stmt.setNull(2, Types.VARCHAR);
            stmt.setInt(3, 0);
            stmt.executeUpdate();
        }
    }

    private void updateBuyersData(Connection connection, ShowPiece showPiece) throws SQLException {
        String sql = "UPDATE `" + BuyersData + "` SET buyer = ?, amount = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, showPiece.getBuyer());
            stmt.setInt(2, showPiece.getAmountValue());
            stmt.setInt(3, showPiece.getId());
            stmt.executeUpdate();
        }
    }

    private void updateLotsData(Connection connection, ShowPiece showPiece) throws SQLException {
        String sql = "UPDATE `" + LotsData + "` SET price = ?, amount = ?, status = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDouble(1, showPiece.getPrice());
            stmt.setInt(2, showPiece.getAmount());
            stmt.setString(3, showPiece.getStatus());
            stmt.setInt(4, showPiece.getId());
            stmt.executeUpdate();
        }
    }

    public static ShowPiece getShowPiece(ResultSet set) throws SQLException {
        int id = set.getInt("id");
        String item = set.getString("item");
        String itemID = set.getString("item_id");
        String seller = set.getString("seller");
        String category = set.getString("category");
        int amount = set.getInt("amount");
        float price = set.getFloat("price");
        float unit = set.getFloat("unit");
        long time = set.getLong("time");
        String status = set.getString("status");
        String buyer = set.getString("buyer");
        int amountValue = set.getInt("amountValue");
        ShowPiece showPiece = new ShowPiece(itemID, seller, item, category, amount, price, unit, time, status, buyer, amountValue);
        showPiece.setId(id);
        return showPiece;
    }

    @Override
    public void saveShowpiece(ShowPiece showPiece) {
        try (Connection connection = connect()) {
            save(connection, showPiece);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ShowPiece removeShowpiece(String seller, int id, String status) {
        try (Connection c = connect()) {
            ShowPiece showPiece = null;
            final String selectQuery = "SELECT " +
                    "l.id, i.item_id, s.seller, l.item, l.category, " +
                    "ld.amount, ld.price, ld.unit, ld.time, ld.status, " +
                    "b.buyer, b.amount AS amountValue " +
                    "FROM `" + Lots + "` l " +
                    "LEFT JOIN `" + ItemIDs + "` i ON l.id = i.id " +
                    "LEFT JOIN `" + SellersData + "` s ON l.id = s.id " +
                    "LEFT JOIN `" + LotsData + "` ld ON l.id = ld.id " +
                    "LEFT JOIN `" + BuyersData + "` b ON l.id = b.id " +
                    "WHERE l.id = ? AND s.seller = ? AND ld.status = ?";
            try (PreparedStatement s = c.prepareStatement(selectQuery)) {
                s.setInt(1, id);
                s.setString(2, seller);
                s.setString(3, status);

                try (ResultSet set = s.executeQuery()) {
                    if (set.next()) showPiece = getShowPiece(set);
                }
            }
            if (showPiece != null) {
                try {
                    c.setAutoCommit(false);
                    executeDelete(c, ItemIDs, id);
                    executeDelete(c, SellersData, id);
                    executeDelete(c, LotsData, id);
                    executeDelete(c, BuyersData, id);
                    executeDelete(c, Lots, id);
                    c.commit();
                } catch (SQLException e) {
                    try { c.rollback(); } catch (SQLException ignored) {}
                    throw new RuntimeException(e);
                } finally {
                    try { c.setAutoCommit(true); } catch (SQLException ignored) {}
                }
            }
            return showPiece;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void executeDelete(Connection c, String table, int id) throws SQLException {
        final String query = "DELETE FROM `" + table + "` WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public void closeConnection() { src.close(); }

    public static int getShowpieceItemIDByIndex(int category, int mode, int index, boolean isExpiredItems, @Nullable String sellerUUID) {
        List<ShowPiece> showpieceDataList = getShowpiecesItemStackSQLData( category, mode, isExpiredItems ? "-" : "0", sellerUUID );
        if (index >= 0 && index < showpieceDataList.size())
            return showpieceDataList.get(index).getId();
        return -1;
    }
}