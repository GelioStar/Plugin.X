package server.variplex.RegionBlocks.Database;

import org.bukkit.Bukkit;
import org.bukkit.World;
import server.variplex.RegionBlocks.RegionBlocks;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class PrivateCore {
    private final Database sqlDatabase;

    public PrivateCore() throws SQLException {
        sqlDatabase = new SQLConfig(new File(RegionBlocks.getInstance().getDataFolder(), "regions.db"));
    }

    public void addPrivateToList(String playerUUID, String privateID, World world, int blockX, int blockY, int blockZ, UUID uuid_1, UUID uuid_2, UUID uuid_3) {
        Private spawner = Private.create(playerUUID, privateID, world.getName(), blockX, blockY, blockZ, uuid_1.toString(), uuid_2.toString(), uuid_3.toString());
        async(() -> sqlDatabase.savePrivate(spawner));
    }

    public List<Private> getPrivateList() {
        return sqlDatabase.getPrivates();
    }

    public void removeSpawner(World world, int blockX, int blockY, int blockZ, Consumer<Private> spawnerConsumer) {
        async(() -> {
            Private privateBlock = sqlDatabase.removePrivate(world, blockX, blockY, blockZ);
            sync(() -> spawnerConsumer.accept(privateBlock));
        });
    }

    public void async(Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(RegionBlocks.getInstance(), () -> {
            synchronized (PrivateCore.this) {
                runnable.run();
            }
        });
    }

    public void sync(Runnable runnable) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(RegionBlocks.getInstance(), runnable);
    }

    public void close() {
        if (sqlDatabase != null) sqlDatabase.closeConnection();
    }
}
