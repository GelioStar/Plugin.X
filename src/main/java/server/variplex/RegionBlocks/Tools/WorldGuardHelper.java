package server.variplex.RegionBlocks.Tools;

import all.api.UTILS.Message;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import server.variplex.RegionBlocks.RegionBlocks;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class WorldGuardHelper {
    public static void createPrivateRegion(Player player, Block block, String regionSuffix, BlockPlaceEvent event) {
        event.setCancelled(true);
        World world = player.getWorld();
        int size = 0;
        // Определяем размер региона в зависимости от типа блока
        if (block.getType() == Material.DIAMOND_BLOCK)
            size = 21;  // Алмазный блок - маленький регион
        else if (block.getType() == Material.EMERALD_ORE)
            size = 33;
        else if (block.getType() == Material.ANCIENT_DEBRIS)
            size = 45;
        else if (block.getType() == Material.NETHERITE_BLOCK) // Version 1.1
            size = 53;
        else if (block.getType().equals(Material.CRYING_OBSIDIAN) && regionSuffix.equals("_C"))
            size = 61;
        else if (block.getType().equals(Material.CRYING_OBSIDIAN) && regionSuffix.equals("_L"))
            size = 57;
        if (checkOverlap(world, block, size)) {
            player.sendMessage("Невозможно создать приват: рядом уже существует другой регион.");
            return;
        }
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(world));

        if (regionManager == null) {
            player.sendMessage("Не удалось получить менеджер региона для этого мира.");
            return;
        }
        // Создаём границы кубоида
        BlockVector3 min = BlockVector3.at(block.getX() - size / 2, block.getY() - size / 2, block.getZ() - size / 2);
        BlockVector3 max = BlockVector3.at(block.getX() + size / 2, block.getY() + size / 2, block.getZ() + size / 2);
        event.setCancelled(false);
        UUID uuid = UUID.randomUUID();
        String regionName = uuid + regionSuffix;
        ProtectedCuboidRegion protectedRegion = new ProtectedCuboidRegion(regionName, min, max);
        // Устанавливаем владельца региона и приоритет
        protectedRegion.getOwners().addPlayer(player.getUniqueId());
        protectedRegion.setPriority(1);
        // Устанавливаем флаги безопасности
        protectedRegion.setFlag(Flags.CHEST_ACCESS, StateFlag.State.DENY);
        protectedRegion.setFlag(Flags.USE, StateFlag.State.ALLOW);
        protectedRegion.setFlag(Flags.BUILD, StateFlag.State.ALLOW);
        try {
            // Добавляем регион в WorldGuard
            regionManager.addRegion(protectedRegion);
            LightningStrike lightningStrike = event.getBlockPlaced().getWorld().strikeLightningEffect(new Location(event.getBlockPlaced().getWorld(), event.getBlockPlaced().getX() + 0.5, event.getBlockPlaced().getY() + 1, event.getBlockPlaced().getZ() + 0.5));
            lightningStrike.isEffect();
            event.getPlayer().damage(0.0d);
            player.sendMessage("Регион " + regionName + " был создан с приоритетом 1.");
            ArmorStand uuid_1 = (ArmorStand) Objects.requireNonNull(block.getLocation().getWorld()).spawnEntity(new Location(event.getBlockPlaced().getWorld(), event.getBlockPlaced().getX() + 0.5, event.getBlockPlaced().getY() + 1.5, event.getBlockPlaced().getZ() + 0.5), EntityType.ARMOR_STAND);
            uuid_1.setInvisible(true);
            uuid_1.setInvulnerable(true);
            uuid_1.setGravity(false);
            if (block.getType().equals(Material.DIAMOND_BLOCK))
                uuid_1.setCustomName(Message.translateColorCodes("&#5B4FE7М&#5952E8а&#5756E9л&#5659EAе&#545DECн&#5260EDь&#5064EEк&#4E67EFи&#4D6BF0й &#4972F2п&#4775F3р&#4579F5и&#447CF6в&#4280F7а&#4083F8т"));
            if (block.getType().equals(Material.EMERALD_ORE))
                uuid_1.setCustomName(Message.translateColorCodes("&#00B762С&#05B55Aр&#0AB453е&#0FB24Bд&#14B144н&#19AF3Cи&#1EAD35й &#28AA26п&#2DA81Eр&#32A717и&#37A50Fв&#3CA408а&#41A200т"));
            if (block.getType().equals(Material.ANCIENT_DEBRIS))
                uuid_1.setCustomName(Message.translateColorCodes("&#864B27Б&#864825о&#864524л&#864222ь&#864021ш&#863D1Fо&#863A1Eй &#85341Bп&#853119р&#852F18и&#852C16в&#852915а&#852613т"));
            if (block.getType().equals(Material.CRYING_OBSIDIAN) && regionSuffix.equals("_C"))
                uuid_1.setCustomName(Message.translateColorCodes("&#8A00FFК&#9000FFо&#9700FFл&#9D00FFо&#A300FFс&#AA00FFс&#B000FFа&#B600FFл&#BD00FFь&#C300FFн&#C900FFы&#D000FFй &#DC00FFп&#E300FFр&#E900FFи&#EF00FFв&#F600FFа&#FC00FFт"));


            if (block.getType().equals(Material.NETHERITE_BLOCK))
                uuid_1.setCustomName(Message.translateColorCodes("&#C44F00О&#C44909г&#C44412р&#C33E1Bо&#C33825м&#C3332Eн&#C32D37ы&#C32840й &#C21C52п&#C2175Bр&#C21165и&#C10B6Eв&#C10677а&#C10080т"));
            if (block.getType().equals(Material.CRYING_OBSIDIAN) && regionSuffix.equals("_L"))
                uuid_1.setCustomName(Message.translateColorCodes("&#E30000Л&#E50400е&#E60800г&#E80D00е&#EA1100н&#EB1500д&#ED1900а&#EF1E00р&#F02200н&#F22600ы&#F32A00й &#F73300п&#F83700р&#FA3B00и&#FC4000в&#FD4400а&#FF4800т"));


            uuid_1.setCustomNameVisible(true);
            uuid_1.setMarker(true);
            uuid_1.setBasePlate(false);
            ArmorStand uuid_2 = (ArmorStand) Objects.requireNonNull(block.getLocation().getWorld()).spawnEntity(new Location(event.getBlockPlaced().getWorld(), event.getBlockPlaced().getX() + 0.5, event.getBlockPlaced().getY() + 1.255, event.getBlockPlaced().getZ() + 0.5), EntityType.ARMOR_STAND);
            uuid_2.setInvisible(true);
            uuid_2.setInvulnerable(true);
            uuid_2.setGravity(false);
            uuid_2.setCustomName(Message.translateColorCodes("&#FFFFFFВладелец: &#1690D4" + event.getPlayer().getName()));
            uuid_2.setCustomNameVisible(true);
            uuid_2.setMarker(true);
            uuid_2.setBasePlate(false);
            ArmorStand uuid_3 = (ArmorStand) Objects.requireNonNull(block.getLocation().getWorld()).spawnEntity(new Location(event.getBlockPlaced().getWorld(), event.getBlockPlaced().getX() + 0.5, event.getBlockPlaced().getY() + 1.01, event.getBlockPlaced().getZ() + 0.5), EntityType.ARMOR_STAND);
            uuid_3.setInvisible(true);
            uuid_3.setInvulnerable(true);
            uuid_3.setGravity(false);
            uuid_3.setCustomName(Message.translateColorCodes(Message.translateColorCodes("&#FFFFFFВремя: Определяется")));
            uuid_3.setCustomNameVisible(true);
            uuid_3.setMarker(true);
            uuid_3.setBasePlate(false);
            RegionBlocks.getPrivateCore().addPrivateToList(player.getUniqueId().toString(), regionName, world, event.getBlockPlaced().getX(), event.getBlockPlaced().getY(), event.getBlockPlaced().getZ(), uuid_1.getUniqueId(), uuid_2.getUniqueId(), uuid_3.getUniqueId());
        } catch (Exception e) {
            event.setCancelled(true);
            player.sendMessage("Ошибка при создании региона: " + e.getMessage());
        }
    }

    // Метод проверки пересечения по ключевым точкам (углы и центр)
    public static boolean checkOverlap(World world, Block block, int size) {
        RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        // Создаём границы кубоида
        BlockVector3 min = BlockVector3.at(
                block.getX() - size / 2, block.getY() - size / 2, block.getZ() - size / 2);
        BlockVector3 max = BlockVector3.at(block.getX() + size / 2, block.getY() + size / 2, block.getZ() + size / 2);
        // Шаг проверки — увеличиваем количество точек в зависимости от размера привата
        int step = Math.max(1, size / 10);  // Устанавливаем шаг по оси X, Y, Z в зависимости от размера
        // Проверяем точки по всему объёму кубоида
        for (int x = min.getX(); x <= max.getX(); x += step) {
            for (int y = min.getY(); y <= max.getY(); y += step) {
                for (int z = min.getZ(); z <= max.getZ(); z += step) {
                    Location loc = new Location(world, x, y, z);
                    ApplicableRegionSet regions = query.getApplicableRegions(BukkitAdapter.adapt(loc));
                    // Проверка пересечения с любыми регионами, кроме глобального
                    boolean hasOverlap = regions.getRegions().stream().anyMatch(region -> !region.getId().equalsIgnoreCase("__global__"));
                    if (hasOverlap) {
                        return true;  // Если пересечение найдено, возвращаем true
                    }
                }
            }
        }
        return false;  // Пересечений не найдено
    }

    // Удаление региона при разрушении блока с проверкой координат в файле regions.yml
    public static void deletePrivateRegion(Player player, Location centerLocation, BlockBreakEvent event) {
        World world = player.getWorld();
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(world));
        if (regionManager == null) {
            player.sendMessage("Не удалось получить менеджер региона для этого мира.");
            return;
        }
        // Получаем все регионы
        for (String regionId : regionManager.getRegions().keySet()) {
            ProtectedRegion region = regionManager.getRegion(regionId);
            // Проверяем совпадение с центром региона
            if (region != null) {
                BlockVector3 regionCenter = region.getMinimumPoint().add(region.getMaximumPoint()).divide(2);
                if (centerLocation.getBlockX() == regionCenter.getX() && centerLocation.getBlockY() == regionCenter.getY() && centerLocation.getBlockZ() == regionCenter.getZ()) {
                    // Удаляем регион
                    regionManager.removeRegion(regionId);
                    LightningStrike lightningStrike = event.getBlock().getWorld().strikeLightningEffect(new Location(event.getBlock().getWorld(), event.getBlock().getX() + 0.5, event.getBlock().getY() + 1, event.getBlock().getZ() + 0.5));
                    lightningStrike.isEffect();
                    player.sendMessage("Регион по координатам " + event.getBlock().getX() + " " + event.getBlock().getY() + " " + event.getBlock().getZ() + " был удален.");
                    RegionBlocks.getPrivateCore().removeSpawner(centerLocation.getWorld(), centerLocation.getBlockX(), centerLocation.getBlockY(), centerLocation.getBlockZ(), (privateBlock -> {
                        Set<UUID> armorStandUUIDs = new HashSet<>();
                        armorStandUUIDs.add(UUID.fromString(privateBlock.getUUID_1()));
                        armorStandUUIDs.add(UUID.fromString(privateBlock.getUUID_2()));
                        armorStandUUIDs.add(UUID.fromString(privateBlock.getUUID_3()));
                        for (ArmorStand armorStand : Objects.requireNonNull(Bukkit.getWorld(privateBlock.getWorld())).getEntitiesByClass(ArmorStand.class))
                            if (armorStandUUIDs.contains(armorStand.getUniqueId())) armorStand.remove();
                    }));
                    return;
                }
            }
        }
    }

    public static void deletePrivateRegion(Player player, Location centerLocation) {
        World world = player.getWorld();
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(world));
        if (regionManager == null) {
            player.sendMessage("Не удалось получить менеджер региона для этого мира.");
            return;
        }
        // Получаем все регионы
        for (String regionId : regionManager.getRegions().keySet()) {
            ProtectedRegion region = regionManager.getRegion(regionId);
            // Проверяем совпадение с центром региона
            if (region != null) {
                BlockVector3 regionCenter = region.getMinimumPoint().add(region.getMaximumPoint()).divide(2);
                if (centerLocation.getBlockX() == regionCenter.getX() && centerLocation.getBlockY() == regionCenter.getY() && centerLocation.getBlockZ() == regionCenter.getZ()) {
                    RegionBlocks.getPrivateCore().removeSpawner(centerLocation.getWorld(), centerLocation.getBlockX(), centerLocation.getBlockY(), centerLocation.getBlockZ(), (privateBlock -> {
                        // Удаляем регион
                        regionManager.removeRegion(regionId);
                        World blockWorld = Bukkit.getWorld(privateBlock.getWorld());
                        Location center = new Location(world, privateBlock.getBlockX(), privateBlock.getBlockY(), privateBlock.getBlockZ());
                        LightningStrike lightningStrike = Objects.requireNonNull(Bukkit.getWorld(privateBlock.getWorld())).strikeLightningEffect(new Location(Bukkit.getWorld(privateBlock.getWorld()), privateBlock.getBlockX() + 0.5, privateBlock.getBlockY() + 1, privateBlock.getBlockZ() + 0.5));
                        lightningStrike.isEffect();
                        player.sendMessage("Регион по координатам " + centerLocation.getBlockX() + " " + centerLocation.getBlockY() + " " + centerLocation.getBlockZ() + " был удален.");
                        Set<UUID> armorStandUUIDs = new HashSet<>();
                        armorStandUUIDs.add(UUID.fromString(privateBlock.getUUID_1()));
                        armorStandUUIDs.add(UUID.fromString(privateBlock.getUUID_2()));
                        armorStandUUIDs.add(UUID.fromString(privateBlock.getUUID_3()));
                        for (ArmorStand armorStand : Objects.requireNonNull(Bukkit.getWorld(privateBlock.getWorld())).getEntitiesByClass(ArmorStand.class))
                            if (armorStandUUIDs.contains(armorStand.getUniqueId())) armorStand.remove();
                        assert blockWorld != null;
                        blockWorld.getBlockAt(center).setType(Material.AIR);
                    }));
                    return;
                }
            }
        }
    }

}