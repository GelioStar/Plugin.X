package server.variplex.RegionBlocks.Tools;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

public class RegionCreator {

    private final int CUBOID_SIZE = 10; // Set cuboid size (length of each side)

    // Create the cuboid region
    public CuboidRegion createCuboid(Block block, int size) {
        Location loc = block.getLocation();
        BlockVector3 min = BlockVector3.at(loc.getX() - size / 2, loc.getY() - size / 2, loc.getZ() - size / 2);
        BlockVector3 max = BlockVector3.at(loc.getX() + size / 2, loc.getY() + size / 2, loc.getZ() + size / 2);

        return new CuboidRegion(min, max);
    }

    // Create the WorldGuard region
    public void createCuboidRegion(Player player, Location blockLocation, BlockPlaceEvent event) {
        Block placedBlock = blockLocation.getBlock();

        // Create cuboid based on block placed
        CuboidRegion cuboid = createCuboid(placedBlock, CUBOID_SIZE);

        // Give the region a unique name, like "playername_region"
        String regionName = player.getName() + "_region";

        // Hook into WorldGuard to create a private region
//        WorldGuardHelper.createPrivateRegion(player, placedBlock, regionName, event);

        // Notify player
        player.sendMessage("A private cuboid region has been created for you!");
    }
}