package server.variplex.Variable.Tools;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class ModuleManager {
    private final JavaPlugin plugin;
    private final List<Module> modules = new ArrayList<>();

    public ModuleManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerModule(Module m) {
        try {
            modules.add(m);
            m.onLoad(plugin);
            plugin.getLogger().info("Модуль зарегистрирован: " + m.getName());
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при регистации модуля " + m.getName(), e);
        }
    }

    public void enableAll() {
        for (Module m : modules) {
            try { m.onEnable(); } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Ошибка при включении модуля " + m.getName(), e);
            }
        }
    }

    public void disableAll() {
        for (Module m : modules) {
            try { m.onDisable(); } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Ошибка при выключении модуля " + m.getName(), e);
            }
        }
    }
}