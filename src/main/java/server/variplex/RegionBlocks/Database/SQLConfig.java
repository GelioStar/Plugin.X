package server.variplex.RegionBlocks.Database;

import org.bukkit.World;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SQLConfig implements Database {
    private static final String privateBlocksList = "privateBlocksList";
    private static String url;

    public SQLConfig(File file) throws SQLException {
        url = "jdbc:sqlite:" + file.getAbsolutePath();
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(String.format(
                    "CREATE TABLE IF NOT EXISTS %s (" +
                            "playerUUID TEXT, " +
                            "privateID TEXT, " +
                            "world TEXT, " +
                            "blockX INTEGER, " +
                            "blockY INTEGER, " +
                            "blockZ INTEGER, " +
                            "uuid_1 TEXT, " +
                            "uuid_2 TEXT, " +
                            "uuid_3 TEXT, " +
                            "time BIGINT, " +
                            "UNIQUE (world, blockX, blockY, blockZ));"
                    , privateBlocksList));
        }
    }

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(url);
    }

    @Override
    public void savePrivate(Private privateBlock) {
        try (Connection connection = connect()) {
            save(connection, privateBlock);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void save(Connection connection, Private privateBlock) {
        String code = "INSERT INTO %s (playerUUID, privateID, world, blockX, blockY, blockZ, uuid_1, uuid_2, uuid_3, time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement s = connection.prepareStatement(String.format(code, privateBlocksList))) {
            s.setString(1, privateBlock.getPlayerUUID());
            s.setString(2, privateBlock.getPrivateID());
            s.setString(3, privateBlock.getWorld());
            s.setInt(4, privateBlock.getBlockX());
            s.setInt(5, privateBlock.getBlockY());
            s.setInt(6, privateBlock.getBlockZ());
            s.setString(7, privateBlock.getUUID_1());
            s.setString(8, privateBlock.getUUID_2());
            s.setString(9, privateBlock.getUUID_3());
            s.setLong(10, privateBlock.getTime());
            s.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Private> savePrivates(Private privateBlock) {
        return null;
    }

    @Override
    public List<Private> getPrivates() {
        String query = String.format("SELECT * FROM `%s`;", privateBlocksList);
        try(Connection c = connect(); PreparedStatement st = c.prepareStatement(query)) {
            List<Private> list = new ArrayList<>();
            try (ResultSet set = st.executeQuery()) {
                while (set.next())
                    list.add(getPrivate(set));
            }
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Private removePrivate(World world, int blockX, int blockY, int blockZ) {
        try (Connection connection = connect()) {
            Private privateBlock = null;
            try (PreparedStatement statement = connection.prepareStatement(String.format("SELECT * FROM %s WHERE world = ? AND blockX = ? AND blockY = ? AND blockZ = ?;", privateBlocksList))) {
                statement.setString(1, world.getName());
                statement.setInt(2, blockX);
                statement.setInt(3, blockY);
                statement.setInt(4, blockZ);
                try (ResultSet set = statement.executeQuery()) {
                    if (set.next()) {
                        privateBlock = getPrivate(set);
                    }
                }
            }
            if (privateBlock != null) {
                try (PreparedStatement statement = connection.prepareStatement(String.format("DELETE FROM %s WHERE world = ? AND blockX = ? AND blockY = ? AND blockZ = ?;", privateBlocksList))) {
                    statement.setString(1, world.getName());
                    statement.setInt(2, blockX);
                    statement.setInt(3, blockY);
                    statement.setInt(4, blockZ);
                    statement.executeUpdate();
                }
            }
            return privateBlock;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Private getPrivate(ResultSet set) throws SQLException {
        String world = set.getString("world");
        String playerUUID = set.getString("playerUUID");
        String privateID = set.getString("privateID");
        int blockX = set.getInt("blockX");
        int blockY = set.getInt("blockY");
        int blockZ = set.getInt("blockZ");
        String uuid_1 = set.getString("uuid_1");
        String uuid_2 = set.getString("uuid_2");
        String uuid_3 = set.getString("uuid_3");
        long time = set.getLong("time");
        return new Private(playerUUID, privateID, world, blockX, blockY, blockZ, uuid_1, uuid_2, uuid_3, time);
    }

    @Override
    public void closeConnection() {
    }
}
