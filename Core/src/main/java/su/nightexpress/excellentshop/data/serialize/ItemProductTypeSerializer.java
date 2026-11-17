package su.nightexpress.excellentshop.data.serialize;

import java.lang.reflect.Type;

import org.bukkit.inventory.ItemStack;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import su.nightexpress.excellentshop.product.content.ItemContent;
import su.nightexpress.nightcore.bridge.item.AdaptedItem;
import su.nightexpress.nightcore.integration.item.ItemBridge;
import su.nightexpress.nightcore.integration.item.bukkit.BukkitItem;
import su.nightexpress.nightcore.util.ItemTag;

public class ItemProductTypeSerializer implements JsonSerializer<ItemContent>, JsonDeserializer<ItemContent> {

    @Override
    public ItemContent deserialize(JsonElement json, Type typeOfT,
                                   JsonDeserializationContext context) throws JsonParseException {
        JsonObject object = json.getAsJsonObject();
        if (!object.has("itemTag")) return null;

        String itemTag = object.get("itemTag").getAsString();
        int dataVersion = object.get("dataVersion").getAsInt();

        ItemTag tag = new ItemTag(itemTag, dataVersion);
        ItemStack itemStack = tag.getItemStack();
        if (itemStack == null) return new ItemContent(new BukkitItem(tag), true);

        if (object.has("amount")) {
            itemStack.setAmount(Math.max(1, object.get("amount").getAsInt()));
        }

        // Let the item bridge resolve the original provider (e.g. ExcellentCrates) from the item data.
        AdaptedItem adaptedItem = object.has("provider") && ItemBridge.isAvailable()
            ? ItemBridge.get().adapt(itemStack).orElseGet(() -> new BukkitItem(tag))
            : new BukkitItem(tag);

        return new ItemContent(adaptedItem, true);
    }

    @Override
    public JsonElement serialize(ItemContent type, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject object = new JsonObject();

        AdaptedItem adaptedItem = type.getAdaptedItem();
        if (adaptedItem instanceof BukkitItem vanillaStack) {
            object.addProperty("itemTag", vanillaStack.getData().getTag());
            object.addProperty("dataVersion", vanillaStack.getData().getDataVersion());
            return object;
        }

        ItemStack itemStack = adaptedItem.getItemStack();
        if (itemStack == null) return object;

        itemStack.setAmount(adaptedItem.getAmount());
        ItemTag tag = ItemTag.of(itemStack);

        object.addProperty("provider", adaptedItem.getProviderId());
        object.addProperty("itemTag", tag.getTag());
        object.addProperty("dataVersion", tag.getDataVersion());
        object.addProperty("amount", adaptedItem.getAmount());

        return object;
    }
}
