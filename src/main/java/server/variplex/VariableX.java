package server.variplex;

import org.bukkit.plugin.java.JavaPlugin;
import server.variplex.Auction.Auction;
import server.variplex.DonateCase.ThunderBolt;
import server.variplex.RegionBlocks.RegionBlocks;
import server.variplex.Variable.Tools.ModuleManager;

public final class VariableX extends JavaPlugin {
    public static VariableX instance;
    private ModuleManager manager;

    @Override
    public void onLoad() {
        instance = this;
        this.manager = new ModuleManager(this);
        // Регистрируем здесь все модули
        manager.registerModule(new ThunderBolt()); // <-- регистрируем, а не включаем
        manager.registerModule(new RegionBlocks());
        manager.registerModule(new Auction());
        // другие manager.registerModule(...)
    }

    @Override
    public void onEnable() {
        // Включаем все модули только в onEnable
//        new ThunderBoltX(this);
        manager.enableAll();
        getLogger().info("VariableX был успешно загружен!");
    }

    @Override
    public void onDisable() {
        manager.disableAll();
    }

    public static VariableX getInstance() {
        return instance;
    }
}

