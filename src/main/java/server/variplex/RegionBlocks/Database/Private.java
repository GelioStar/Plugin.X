package server.variplex.RegionBlocks.Database;

public class Private {
    public final String playerUUID, privateID, world;
    public final int blockX, blockY, blockZ;
    public final String UUID_1, UUID_2, UUID_3;
    public final long time;

    public Private(String playerUUID, String privateID, String world, int blockX, int blockY, int blockZ, String UUID_1, String UUID_2, String UUID_3, long time) {
        this.playerUUID = playerUUID;
        this.privateID = privateID;
        this.world = world;
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        this.UUID_1 = UUID_1;
        this.UUID_2 = UUID_2;
        this.UUID_3 = UUID_3;
        this.time = time;
    }

    public static Private create(String playerUUID, String privateID, String world, int blockX, int blockY, int blockZ, String uuid_1, String uuid_2, String uuid_3){
        return new Private(playerUUID, privateID, world, blockX, blockY, blockZ, uuid_1, uuid_2, uuid_3, System.currentTimeMillis());
    }

    public String getPlayerUUID() {
        return playerUUID;
    }

    public String getPrivateID() {
        return privateID;
    }

    public String getWorld() {
        return world;
    }

    public int getBlockX() {
        return blockX;
    }

    public int getBlockY() {
        return blockY;
    }

    public int getBlockZ() {
        return blockZ;
    }

    public String getUUID_1() {
        return UUID_1;
    }

    public String getUUID_2() {
        return UUID_2;
    }

    public String getUUID_3() {
        return UUID_3;
    }

    public long getTime() {
        return time;
    }
}
