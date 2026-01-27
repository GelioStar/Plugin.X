package server.variplex.Auction.Tools;

import all.api.UTILS.Message;
import net.md_5.bungee.api.chat.*;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Lectern;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;
import server.variplex.Auction.Auction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class BookManager {

    public static ItemStack createWikiBook() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        ConfigurationSection config = Auction.getSettings().getConfig().getConfigurationSection("Wiki");
        assert meta != null;
        assert config != null;
        meta.setTitle(Message.translateColorCodes(config.getString("title")));
        meta.setAuthor(Message.translateColorCodes(config.getString("author")));
        NamespacedKey key = new NamespacedKey(Auction.getInstance(), "book_id");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, Objects.requireNonNull(config.getString("id")));
        ConfigurationSection pagesSection = config.getConfigurationSection("pages");
        List<BaseComponent[]> pages = new ArrayList<>();
        assert pagesSection != null;
        for (String pageKey : pagesSection.getKeys(false)) {
            List<String> pageLines = pagesSection.getStringList(pageKey);
            List<BaseComponent> components = new ArrayList<>();
            for (String line : pageLines)
                if (line.startsWith("{link:")) {
                    String text = line.split(":")[1].replace("}", "").trim();
                    components.addAll(createClickableText(text, pageKey));
                } else
                    components.add(new TextComponent(Message.translateColorCodes(line + "\n")));

            pages.add(components.toArray(new BaseComponent[0]));
        }
        meta.spigot().setPages(pages);
        book.setItemMeta(meta);
        return book;
    }

    private static List<BaseComponent> createClickableText(String text, String targetPage) {
        ComponentBuilder builder = new ComponentBuilder();
        builder.append(TextComponent.fromLegacyText(Message.translateColorCodes(text)))
                .event(new ClickEvent(ClickEvent.Action.CHANGE_PAGE, targetPage))
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder("§aПерейти к странице " + targetPage).create()));
        return Arrays.asList(builder.create());
    }
    public void openBookOnPage(Player player, ItemStack book, int page) {
        Block lecternBlock = player.getLocation().getBlock().getRelative(BlockFace.UP);
        lecternBlock.setType(Material.LECTERN);
        Lectern lectern = (Lectern) lecternBlock.getState();
        lectern.getInventory().setItem(0, book);
        lectern.setPage(page); // Установка страницы (начиная с 0)
        lectern.update();
        player.openInventory(lectern.getInventory());
    }
}