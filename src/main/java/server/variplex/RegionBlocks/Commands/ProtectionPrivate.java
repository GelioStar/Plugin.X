package server.variplex.RegionBlocks.Commands;

import all.api.CUBEAPI;
import all.api.UTILS.Command;
import all.api.UTILS.Message;
import com.google.common.collect.Lists;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import server.variplex.RegionBlocks.RegionBlocks;
import server.variplex.RegionBlocks.Listeners.PlaceBreakRegion;

import java.util.List;
import java.util.Objects;

public class ProtectionPrivate extends Command {
    public ProtectionPrivate() {
        super("protections", CUBEAPI.getInstance().getClass());
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("curegion.commands.pp")) {
            sender.sendMessage(Message.translateColorCodes(Objects.requireNonNull(RegionBlocks.getSettings().getConfig().getString("CommandMessages.ErrorMessage"))));
            return;
        }
        String mode = args[0];
        Player player = Bukkit.getPlayer(args[1]);
        String type = args[2];
        int amount = Integer.parseInt(args[3]);
        assert player != null;
        PlayerInventory playerInventory = player.getInventory();
        if (!sender.hasPermission("curegion.commands.pp.give")) {
            sender.sendMessage(Message.translateColorCodes(Objects.requireNonNull(RegionBlocks.getSettings().getConfig().getString("CommandMessages.ErrorMessage"))));
            return;
        } if (mode.equals("give"))
            switch (type) {
                case "colossal":
                    if (!playerInventory.isEmpty()) {
                        Item item = (Item) Objects.requireNonNull(Bukkit.getWorld(player.getWorld().getName())).spawnEntity(player.getLocation(), EntityType.DROPPED_ITEM);
                        item.setItemStack(PlaceBreakRegion.ColossalPrivate(amount));
                    } else
                        playerInventory.addItem(PlaceBreakRegion.ColossalPrivate(amount));
                    break;
                case "legendary":
                    if (!playerInventory.isEmpty()) {
                        Item item = (Item) Objects.requireNonNull(Bukkit.getWorld(player.getWorld().getName())).spawnEntity(player.getLocation(), EntityType.DROPPED_ITEM);
                        item.setItemStack(PlaceBreakRegion.LegendaryPrivate(amount));
                    } else
                        playerInventory.addItem(PlaceBreakRegion.LegendaryPrivate(amount));
                    break;
            }
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1) return Lists.newArrayList("give");
        if (args.length == 2) return null;
        if (args.length == 3) return Lists.newArrayList("colossal", "legendary");
        return Lists.newArrayList();
    }
}
