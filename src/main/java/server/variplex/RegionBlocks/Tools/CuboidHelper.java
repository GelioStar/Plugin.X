package server.variplex.RegionBlocks.Tools;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import org.bukkit.block.Block;

public class CuboidHelper {

    // Create a cuboid region around the placed block
    public static CuboidRegion createCuboid(Block block, int size) {
        BlockVector3 min = BlockVector3.at(
                block.getX() - size / 2,
                block.getY() - size / 2,
                block.getZ() - size / 2
        );
        BlockVector3 max = BlockVector3.at(
                block.getX() + size / 2,
                block.getY() + size / 2,
                block.getZ() + size / 2
        );
        return new CuboidRegion(min, max);
    }
}