package server.variplex.RegionBlocks;

import all.api.UTILS.Config;
import all.api.UTILS.Message;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import server.variplex.RegionBlocks.Commands.ProtectionPrivate;
import server.variplex.RegionBlocks.Database.Private;
import server.variplex.RegionBlocks.Database.PrivateCore;
import server.variplex.RegionBlocks.Listeners.PlaceBreakRegion;
import server.variplex.RegionBlocks.Tools.WorldGuardHelper;
import server.variplex.Variable.Tools.Module;

import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

public class RegionBlocks implements Listener, Module {
    static PrivateCore privateCore;
    public static Plugin instance;
    public static Config config;

    @Override
    public String getName() {
        return "RegionBlocks";
    }

    @Override
    public void onLoad(JavaPlugin plugin) {
        instance = plugin;
        config = new Config(plugin, "ResionBlocks/config.yml");
        plugin.saveDefaultConfig();
    }

    @Override
    public void onEnable() {
        new ProtectionPrivate();

        try {
            privateCore = new PrivateCore();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

//        System.out.println(Message.translateColorCodes(" "));
//        System.out.println(Message.translateColorCodes("  &#E90000┏━━&#D70400"));
//        System.out.println(Message.translateColorCodes("  &#E90000┃  &#D70400 ┃  ┃  &#C60700L&#B40B00O&#A30E00A&#911200D &av" + pdf.getVersion() + "&8 • Spigot"));
//        System.out.println(Message.translateColorCodes("  &#E90000┗━━&#D70400 ┗━━┛"));
//        System.out.println(Message.translateColorCodes(" "));
        Bukkit.getServer().getPluginManager().registerEvents(new PlaceBreakRegion(), getInstance());
        Bukkit.getScheduler().runTaskTimer(getInstance(), () -> {
            for (Private privateUnit : privateCore.getPrivateList()) {
                if (privateUnit.getPrivateID().endsWith("_S"))
                    for (ArmorStand armorStand : Objects.requireNonNull(Bukkit.getWorld(privateUnit.getWorld())).getEntitiesByClass(ArmorStand.class))
                        if (armorStand.getUniqueId().equals(UUID.fromString(privateUnit.getUUID_3()))) {
                            World world = Bukkit.getWorld(privateUnit.getWorld());
                            Location centerLocation = new Location(world, privateUnit.getBlockX(), privateUnit.getBlockY(), privateUnit.getBlockZ());
                            armorStand.setCustomName(Message.translateColorCodes(getRemainingTime(Bukkit.getPlayer(UUID.fromString(privateUnit.getPlayerUUID())), centerLocation, privateUnit.getTime() + 86401000)));
                        }
                if (privateUnit.getPrivateID().endsWith("_M"))
                    for (ArmorStand armorStand : Objects.requireNonNull(Bukkit.getWorld(privateUnit.getWorld())).getEntitiesByClass(ArmorStand.class))
                        if (armorStand.getUniqueId().equals(UUID.fromString(privateUnit.getUUID_3()))) {
                            World world = Bukkit.getWorld(privateUnit.getWorld());
                            Location centerLocation = new Location(world, privateUnit.getBlockX(), privateUnit.getBlockY(), privateUnit.getBlockZ());
                            armorStand.setCustomName(Message.translateColorCodes(getRemainingTime(Bukkit.getPlayer(UUID.fromString(privateUnit.getPlayerUUID())), centerLocation, privateUnit.getTime() + 70000)));
                        }
                if (privateUnit.getPrivateID().endsWith("_B"))
                    for (ArmorStand armorStand : Objects.requireNonNull(Bukkit.getWorld(privateUnit.getWorld())).getEntitiesByClass(ArmorStand.class))
                        if (armorStand.getUniqueId().equals(UUID.fromString(privateUnit.getUUID_3()))) {
                            World world = Bukkit.getWorld(privateUnit.getWorld());
                            Location centerLocation = new Location(world, privateUnit.getBlockX(), privateUnit.getBlockY(), privateUnit.getBlockZ());
                            armorStand.setCustomName(Message.translateColorCodes(getRemainingTime(Bukkit.getPlayer(UUID.fromString(privateUnit.getPlayerUUID())), centerLocation, privateUnit.getTime() + 259201000)));
                        }
                if (privateUnit.getPrivateID().endsWith("_H"))
                    for (ArmorStand armorStand : Objects.requireNonNull(Bukkit.getWorld(privateUnit.getWorld())).getEntitiesByClass(ArmorStand.class))
                        if (armorStand.getUniqueId().equals(UUID.fromString(privateUnit.getUUID_3()))) {
                            World world = Bukkit.getWorld(privateUnit.getWorld());
                            Location centerLocation = new Location(world, privateUnit.getBlockX(), privateUnit.getBlockY(), privateUnit.getBlockZ());
                            armorStand.setCustomName(Message.translateColorCodes(getRemainingTime(Bukkit.getPlayer(UUID.fromString(privateUnit.getPlayerUUID())), centerLocation, privateUnit.getTime() + 432001000)));
                        }
            }
        }, 0, 20);
    }

    @Override
    public void onDisable() {
        if (privateCore != null) {
            privateCore.close();
        }
    }

    public static Plugin getInstance() {
        return instance;
    }

    public static Config getSettings() {
        return config;
    }

    public static PrivateCore getPrivateCore() {
        return privateCore;
    }

    public static String getRemainingTime(Player player, Location centerLocation, long endTimeMillis) {
        long nowMillis = System.currentTimeMillis();
        long remainingMillis = endTimeMillis - nowMillis;
        if (remainingMillis <= 1000)
            WorldGuardHelper.deletePrivateRegion(player, centerLocation);
        long seconds = remainingMillis / 1000 % 60;
        long minutes = remainingMillis / (1000 * 60) % 60;
        long hours = remainingMillis / (1000 * 60 * 60) % 24;
        long days = remainingMillis / (1000 * 60 * 60 * 24);
        if (days > 0)
            return String.format("&#FFFFFFВремя: &#1690D4%d &#FFFFFFдн. &#1690D4%d &#FFFFFFч.", days, hours);
        else if (hours > 0)
            return String.format("&#FFFFFFВремя: &#1690D4%d &#FFFFFFч. &#1690D4%d &#FFFFFFм.", hours, minutes);
        else if (minutes > 0)
            return String.format("&#FFFFFFВремя: &#1690D4%d &#FFFFFFм. &#1690D4%d &#FFFFFFс.", minutes, seconds);
        else
            return String.format("&#FFFFFFВремя: &#1690D4%d &#FFFFFFс.", seconds);
    }
}