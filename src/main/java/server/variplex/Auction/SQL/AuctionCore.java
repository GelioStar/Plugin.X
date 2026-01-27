package server.variplex.Auction.SQL;

import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.inventory.ItemStack;
import server.variplex.Auction.Auction;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Consumer;

import static server.variplex.Auction.SQL.SQLite.LotsData;

public class AuctionCore {
    private final Database sqlDatabase;
    public AuctionCore() {
        try {
            Configuration configuration = Auction.getSettings().getConfig();
            if(configuration.getBoolean("MySQL.enabled")) {
                sqlDatabase = new MySQL(
                        configuration.getString("MySQL.host"),
                        configuration.getInt("MySQL.port"),
                        configuration.getString("MySQL.database"), configuration.getString("MySQL.user"), configuration.getString("MySQL.password") );
            } else {
                sqlDatabase = new SQLite(new File(Auction.getInstance().getDataFolder(), "auction.db"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("[ERROR] Ошибка в инициализации базы данных.", e);
        }
    }

    private void addShowPrice(String player, ItemStack item, float price) {
        ShowPiece showPiece = ShowPiece.create(player, item, price);
        async(() -> sqlDatabase.saveShowpiece(showPiece));
    }

    public void removeShowPrice(String seller, int id, String status, Consumer<ShowPiece> showpieceConsumer) {
        async(() -> {
            ShowPiece showPiece = sqlDatabase.removeShowpiece(seller, id, status);
            sync(() -> showpieceConsumer.accept(showPiece));
        });
    }

    public void sellShowPiece(String player, ItemStack item, float price) {
        addShowPrice(player, item, price);
    }

    public void addExpiredItem(ShowPiece showPiece) {
        try (PreparedStatement s = SQLite.connect().prepareStatement(String.format("" +
                    "UPDATE %s SET status = '-' WHERE id = ?;", LotsData))) {
            s.setInt(1, showPiece.getId());
            s.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void async(Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(Auction.getInstance(), () -> {
            synchronized (AuctionCore.this) {
                runnable.run();
            }
        });
    }

    public void sync(Runnable runnable) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(Auction.getInstance(), runnable);
    }

    public void close() {
        if (sqlDatabase != null) sqlDatabase.closeConnection();
    }
}
