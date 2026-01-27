package server.variplex.DonateCase;

import all.api.UTILS.Config;
import org.bukkit.plugin.java.JavaPlugin;
import server.variplex.DonateCase.Commands.ThunderBoltX;
import server.variplex.Variable.Tools.Module;
import server.variplex.VariableX;

public final class ThunderBolt implements Module {

    private JavaPlugin host;
    private static Config settings;

    @Override
    public String getName() {
        return "Thunderbolt";
    }

    @Override
    public void onLoad(JavaPlugin plugin) {
        this.host = plugin;
        settings = new Config(plugin, "Thunderbolt/config.yml");
    }

    @Override
    public void onEnable() {
        new ThunderBoltX();
    }

    @Override
    public void onDisable() {
        host.getLogger().info(getName() + " disabled.");
    }

    public JavaPlugin getHost() {
        return host;
    }

    public static Config getSettings() {
        return settings;
    }
}


