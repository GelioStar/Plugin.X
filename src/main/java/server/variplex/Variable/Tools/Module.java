package server.variplex.Variable.Tools;

import org.bukkit.plugin.java.JavaPlugin;

public interface Module {
    String getName();
    void onLoad(JavaPlugin plugin);
    void onEnable();
    void onDisable();
}