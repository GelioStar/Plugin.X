package server.variplex.Auction;

import all.api.UTILS.Config;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import server.variplex.Auction.SQL.AuctionCore;
import server.variplex.Variable.Tools.Module;

import static server.variplex.Auction.Tools.JSON_Parse.loadTranslations;

public final class Auction implements Module {
    private static Plugin instance;
    private static AuctionCore auctionCore;
    private static Config settings;

    @Override
    public String getName() {
        return "Auction";
    }

    @Override
    public void onLoad(JavaPlugin plugin) {
        instance = plugin;
        settings = new Config(plugin, "Auction/config.yml");
        plugin.saveDefaultConfig();
        loadTranslations("E:/PROJECTS/#1_MEDIA_PROJECTS/#1_YOUTUBE/#2_VARIABLE_PROJECT/#1_VARIABLE/#0_VARIABLE/plugins/VariableX/ru_ru.json");
    }

    @Override
    public void onEnable() {
        new server.variplex.Auction.Commands.Auction();
        Bukkit.getServer().getPluginManager().registerEvents(new server.variplex.Auction.Commands.Auction(), getInstance());
        auctionCore = new AuctionCore();
//        System.out.println(Message.translateColorCodes(" "));
//        System.out.println(Message.translateColorCodes("  &#E90000┏━━&#D70400"));
//        System.out.println(Message.translateColorCodes("  &#E90000┃  &#D70400 ┃  ┃  &8VARIABLE_PROJECT • Spigot"));
//        System.out.println(Message.translateColorCodes("  &#E90000┗━━&#D70400 ┗━━┛   &8CUBAUCTION • " + pdf.getVersion()));
//        System.out.println(Message.translateColorCodes(" "));
    }

    @Override
    public void onDisable() { if(auctionCore != null) auctionCore.close(); }
    public static Plugin getInstance() { return instance; }
    public static Config getSettings() { return settings; }


    public static AuctionCore getAuctionCore() { return auctionCore; }

    public static ItemStack createCustomBook() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();

        // Название и автор
        assert meta != null;
        meta.setTitle("§6Кастомная Книга");
        meta.setAuthor("§bВашСервер");

        // Создание страниц с форматированием
        for (int i = 1; i <= 3; i++) {
            BaseComponent[] page = new ComponentBuilder()
                    .append("§lСтраница " + i + "\n\n")
                    .append("§7Это пример текста.\n")
                    .append("§eКликабельная ссылка: ")
                    .append("§9[Нажми]")
                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/say Hello!"))
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§aВыполнить команду!").create()))
                    .create();
            meta.spigot().addPage(page);
        }

        book.setItemMeta(meta);
        return book;
    }
}
