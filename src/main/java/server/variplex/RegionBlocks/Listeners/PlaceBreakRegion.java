package server.variplex.RegionBlocks.Listeners;

import all.api.UTILS.Message;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import server.variplex.RegionBlocks.RegionBlocks;
import server.variplex.RegionBlocks.Tools.WorldGuardHelper;

import java.util.List;
import java.util.stream.Collectors;

public class PlaceBreakRegion implements Listener {
    private static final int SMALL_PRIVATE = 21,
            MEDIUM_PRIVATE = 33,
            LARGE_PRIVATE = 45,
            HUGE_PRIVATE = 53,
            COLOSSAL_PRIVATE = 61,
            LEGENDARY_PRIVATE = 57;

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        switch (event.getBlockPlaced().getType()){
            case DIAMOND_BLOCK:
                WorldGuardHelper.createPrivateRegion(event.getPlayer(), event.getBlockPlaced(), "_S", event); break; // Version 1.0
            case EMERALD_ORE:
                WorldGuardHelper.createPrivateRegion(event.getPlayer(), event.getBlockPlaced(), "_M", event); break; // Version 1.0
            case ANCIENT_DEBRIS:
                WorldGuardHelper.createPrivateRegion(event.getPlayer(), event.getBlockPlaced(), "_B", event); break; // Version 1.0
            case NETHERITE_BLOCK:
                WorldGuardHelper.createPrivateRegion(event.getPlayer(), event.getBlockPlaced(), "_H", event); break; // Version 1.1
        }
        if(event.getPlayer().getInventory().getItemInMainHand().equals(ColossalPrivate(1)))
            WorldGuardHelper.createPrivateRegion(event.getPlayer(), event.getBlockPlaced(), "_C", event); // Version 1.0
        if(event.getPlayer().getInventory().getItemInMainHand().equals(LegendaryPrivate(1)))
            WorldGuardHelper.createPrivateRegion(event.getPlayer(), event.getBlockPlaced(), "_L", event); // Version 1.1
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if(event.getBlock().getType().equals(Material.DIAMOND_BLOCK) ||
                event.getBlock().getType().equals(Material.EMERALD_ORE) ||
                event.getBlock().getType().equals(Material.ANCIENT_DEBRIS) ||
                event.getBlock().getType().equals(Material.NETHERITE_BLOCK) ||
                event.getBlock().getType().equals(Material.CRYING_OBSIDIAN) ||
                event.getBlock().getType().equals(Material.GILDED_BLACKSTONE)){
            WorldGuardHelper.deletePrivateRegion(event.getPlayer(), event.getBlock().getLocation(), event);
        }
    }
    public static ItemStack ColossalPrivate(int amount){
        return createItemFromConfig(Material.CRYING_OBSIDIAN, amount, RegionBlocks.getSettings().getConfig().getStringList("Items.ColossalPrivate"));
    }
    public static ItemStack LegendaryPrivate(int amount) {
        return createItemFromConfig(Material.GILDED_BLACKSTONE, amount, RegionBlocks.getSettings().getConfig().getStringList("Items.LegendaryPrivate"));
    }
    public static ItemStack createItemFromConfig(Material material, int amount, List<String> configList) {
        if (configList == null || configList.isEmpty())
            throw new IllegalArgumentException("Список из config.yml пуст или не задан.");
        String itemName = Message.translateColorCodes(configList.stream().findFirst().orElse(""));
        List<String> lore = configList.stream().skip(1).map(Message::translateColorCodes).collect(Collectors.toList());
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        meta.setDisplayName(itemName);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}