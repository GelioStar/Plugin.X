package server.variplex.Variable.Commands;

import all.api.UTILS.Command;
import org.bukkit.command.CommandSender;
import server.variplex.VariableX;

public class Variable extends Command {
    public Variable() {
        super("variable", VariableX.class);
    }

    @Override
    public void execute(CommandSender commandSender, String s, String[] strings) {

    }
}
