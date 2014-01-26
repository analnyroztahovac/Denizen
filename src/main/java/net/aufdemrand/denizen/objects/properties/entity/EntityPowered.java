package net.aufdemrand.denizen.objects.properties.entity;

import net.aufdemrand.denizen.objects.Element;
import net.aufdemrand.denizen.objects.Mechanism;
import net.aufdemrand.denizen.objects.dEntity;
import net.aufdemrand.denizen.objects.dObject;
import net.aufdemrand.denizen.objects.properties.Property;
import net.aufdemrand.denizen.tags.Attribute;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;

public class EntityPowered implements Property {

    public static boolean describes(dObject entity) {
        return entity instanceof dEntity && ((dEntity)entity).getEntityType() == EntityType.CREEPER;
    }

    public static EntityPowered getFrom(dObject entity) {
        if (!describes(entity)) return null;

        else return new EntityPowered((dEntity) entity);
    }

    ///////////////////
    // Instance Fields and Methods
    /////////////

    private EntityPowered(dEntity entity) {
        powered = entity;
    }

    dEntity powered;

    private Boolean getPowered() {
        if (powered == null) return null;

        return ((Creeper)(powered.getBukkitEntity())).isPowered();
    }

    private void setPowered(boolean power) {
        if (powered == null) return;

        ((Creeper)(powered.getBukkitEntity())).setPowered(power);
    }

    /////////
    // Property Methods
    ///////

    @Override
    public String getPropertyString() {
        if (!getPowered()) return null;
        else return "true";
    }

    @Override
    public String getPropertyId() {
        return "powered";
    }

    ///////////
    // dObject Attributes
    ////////

    @Override
    public String getAttribute(Attribute attribute) {

        if (attribute == null) return "null";

        // <--[tag]
        // @attribute <e@entity.powered>
        // @returns Element
        // @description
        // Returns the powered state of a creeper.
        // -->
        if (attribute.startsWith("powered"))
            return new Element(getPowered())
                    .getAttribute(attribute.fulfill(1));

        return null;
    }

    @Override
    public void adjust(Mechanism mechanism) {

        // <--[mechanism]
        // @object dEntity
        // @name powered
        // @input Element
        // @description
        // Changes the powered state of a Creeper.
        // @tags
        // <e@entity.powered>
        // -->

        if (mechanism.matches("powered")) {
            setPowered(mechanism.getValue().asBoolean());
        }
    }
}