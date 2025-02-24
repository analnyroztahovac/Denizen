package com.denizenscript.denizen.objects.properties.item;

import com.denizenscript.denizen.nms.NMSHandler;
import com.denizenscript.denizen.objects.ItemTag;
import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.objects.properties.PropertyParser;
import com.denizenscript.denizencore.utilities.text.StringHolder;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ItemComponentsPatch extends ItemProperty<MapTag> {

    // <--[property]
    // @object ItemTag
    // @name components_patch
    // @input MapTag
    // @description
    // Controls the item's internal component patch. That is, the changes in components on top of the item type's default components.
    // The map is in <@link language Raw NBT Encoding> format.
    // This is mainly intended for item data persistence, and scripts should prefer using proper item properties instead of setting raw data directly.
    // If you're trying to control custom data (such as data set by other plugins), use <@link property ItemTag.custom_data>.
    // @tag
    // Note that this is just data that isn't already controlled by other ItemTag properties, see <@link tag ItemTag.full_components_patch> for the complete component patch.
    // @warning
    // Due to this being a direct representation of internal data, compatibility for script usage across versions is not guaranteed.
    // -->

    public static final String DATA_VERSION_KEY = "denizen:__data_version";
    public static final PerIdPropertyDataRemover ENTITY_DATA_REMOVER = new PerIdPropertyDataRemover("minecraft:entity_data");
    public static final PerIdPropertyDataRemover BLOCK_ENTITY_DATA_REMOVER = new PerIdPropertyDataRemover("minecraft:block_entity_data");
    public static final StringHolder INSTRUMENT_COMPONENT = new StringHolder("minecraft:instrument");
    public static final Set<String> propertyHandledComponents = new HashSet<>();

    public static void registerHandledComponent(String component) {
        propertyHandledComponents.add("minecraft:" + component);
    }

    static {
        ENTITY_DATA_REMOVER.registerRemoval(EntityType.ITEM_FRAME, "Invisible");
        ENTITY_DATA_REMOVER.registerRemoval(EntityType.ARMOR_STAND, "Pose", "Small", "NoBasePlate", "Marker", "Invisible", "ShowArms");
        BLOCK_ENTITY_DATA_REMOVER.registerRemoval("minecraft:sign", "front_text", "back_text", "is_waxed");
        BLOCK_ENTITY_DATA_REMOVER.registerRemoval("minecraft:hanging_sign", "front_text", "back_text", "is_waxed");
        BLOCK_ENTITY_DATA_REMOVER.registerRemoval("minecraft:spawner",
                "SpawnCount", "Delay", "MinSpawnDelay", "MaxSpawnDelay", "MaxNearbyEntities", "RequiredPlayerRange", "SpawnRange"
//                , "SpawnData", "SpawnPotentials" TODO: needs proper property support
        );
    }

    public static boolean describes(ItemTag item) {
        return item.getBukkitMaterial() != Material.AIR;
    }

    @Override
    public MapTag getPropertyValue() {
        MapTag rawComponents = NMSHandler.itemHelper.getRawComponentsPatch(getItemStack(), true);
        if (rawComponents.isEmpty()) {
            return rawComponents;
        }
        ENTITY_DATA_REMOVER.removeFrom(rawComponents);
        BLOCK_ENTITY_DATA_REMOVER.removeFrom(rawComponents);
        rawComponents.map.computeIfPresent(INSTRUMENT_COMPONENT, (key, value) -> value instanceof ElementTag ? null : value);
        if (rawComponents.size() == 1) { // Just the data version
            return new MapTag();
        }
        return rawComponents;
    }

    @Override
    public boolean isDefaultValue(MapTag value) {
        return value.isEmpty();
    }

    @Override
    public void setPropertyValue(MapTag value, Mechanism mechanism) {
        ElementTag dataVersionInput = value.getElement(DATA_VERSION_KEY);
        int dataVersion;
        if (dataVersionInput == null) {
            dataVersion = Integer.MAX_VALUE;
        }
        else if (!dataVersionInput.isInt()) {
            mechanism.echoError("Invalid data version '" + dataVersionInput + "' specified: must be a valid non-decimal number.");
            return;
        }
        else {
            dataVersion = dataVersionInput.asInt();
            value.remove(DATA_VERSION_KEY);
        }
        setItemStack(NMSHandler.itemHelper.setRawComponentsPatch(getItemStack(), value, dataVersion, mechanism::echoError));
    }

    @Override
    public String getPropertyId() {
        return "components_patch";
    }

    public static void register() {
        autoRegister("components_patch", ItemComponentsPatch.class, MapTag.class, false);

        // <--[tag]
        // @attribute <ItemTag.full_components_patch>
        // @returns MapTag
        // @description
        // Returns the item's entire internal component patch (see <@link tag ItemTag.components_patch>).
        // @warning
        // Due to this being a direct representation of internal data, compatibility for script usage across versions is not guaranteed.
        // -->
        PropertyParser.registerTag(ItemComponentsPatch.class, MapTag.class, "full_components_patch", (attribute, property) -> {
            return NMSHandler.itemHelper.getRawComponentsPatch(property.getItemStack(), false);
        });
    }

    public record PerIdPropertyDataRemover(StringHolder propertyId, Map<String, Set<StringHolder>> removalsPerId) {

        public static final StringHolder ID_STRING_HOLDER = new StringHolder("id");

        public PerIdPropertyDataRemover(String propertyId) {
            this(new StringHolder(propertyId), new HashMap<>());
        }

        public void registerRemoval(Keyed type, String... keys) {
            registerRemoval(type.getKey().toString(), keys);
        }

        public void registerRemoval(String id, String... keys) {
            Set<StringHolder> toRemove = new HashSet<>(keys.length + 1);
            toRemove.add(ID_STRING_HOLDER);
            for (String key : keys) {
                toRemove.add(new StringHolder(key));
            }
            removalsPerId.put("string:" + id, toRemove);
        }

        public void removeFrom(MapTag rawComponents) {
            rawComponents.map.computeIfPresent(propertyId, (key, rawValue) -> {
                MapTag value = (MapTag) rawValue;
                Set<StringHolder> toRemove = removalsPerId.get(value.getObject(ID_STRING_HOLDER).toString());
                if (toRemove != null && toRemove.size() >= value.size() && toRemove.containsAll(value.keySet())) {
                    return null;
                }
                return rawValue;
            });
        }
    }
}
