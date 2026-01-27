package server.variplex.RegionBlocks.Database;

import org.bukkit.World;

import java.util.List;

public interface Database {
    void savePrivate(Private privateBlock);

    List<Private> savePrivates(Private privateBlock);

    List<Private> getPrivates();
    Private removePrivate(World world, int blockX, int blockY, int blockZ);
    void closeConnection();
}