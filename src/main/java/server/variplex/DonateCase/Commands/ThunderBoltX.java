package server.variplex.DonateCase.Commands;

import all.api.UTILS.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import server.variplex.DonateCase.Tools.Animation;
import server.variplex.DonateCase.ThunderBolt;
import server.variplex.Variable.Commands.Variable;
import server.variplex.VariableX;

public class ThunderBoltX extends Command {

    public ThunderBoltX() {
        super("thunderbolt", VariableX.getInstance().getClass());
    }

    @Override
    public void execute(CommandSender sender, String s, String[] args) {
        if (!(sender instanceof Player)) return;
        if (args.length == 0) {
            new Animation().startAnimation(((Player) sender).getPlayer(), 100, 100, 100);
            return;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("thunderbolt.reload")) return;
            ;
            return;
        }
    }
}
