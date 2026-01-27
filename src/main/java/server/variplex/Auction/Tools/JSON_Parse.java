package server.variplex.Auction.Tools;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Material;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JSON_Parse {
    public static final Map<Material, String> materialTranslations = new HashMap<>();

    public static void loadTranslations(String filePath) {
        try (FileReader reader = new FileReader(filePath)) {
            JsonParser parser = new JsonParser();
            JsonElement jsonElement = parser.parse(reader);
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue().getAsString();
                if (key.startsWith("item.minecraft.") || key.startsWith("block.minecraft.")) {
                    String materialName = key.substring(key.lastIndexOf(".") + 1).toUpperCase();
                    try {
                        Material material = Material.valueOf(materialName);
                        materialTranslations.put(material, value);
                    } catch (IllegalArgumentException ignored) { }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static Map<String, String> parseItemAndBlockNames(String filePath) {
        Map<String, String> itemNames = new HashMap<>();
        try (FileReader reader = new FileReader(filePath)) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("block.minecraft.") || key.startsWith("item.minecraft."))
                    itemNames.put(key, entry.getValue().getAsString());
            }
        } catch (IOException e) { e.printStackTrace(); }
        return itemNames;
    }
}
