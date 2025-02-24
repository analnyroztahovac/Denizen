package com.denizenscript.denizen.scripts.commands.world;

import com.denizenscript.denizen.nms.NMSHandler;
import com.denizenscript.denizen.nms.NMSVersion;
import com.denizenscript.denizen.objects.*;
import com.denizenscript.denizen.objects.properties.bukkit.BukkitColorExtensions;
import com.denizenscript.denizen.utilities.BukkitImplDeprecations;
import com.denizenscript.denizen.utilities.LegacyParticleNaming;
import com.denizenscript.denizen.utilities.Utilities;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.*;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class PlayEffectCommand extends AbstractCommand {

    public static final List<Particle> VISIBLE_PARTICLES = new ArrayList<>(Arrays.asList(Particle.values()));

    static {
        VISIBLE_PARTICLES.removeAll(List.of(Particle.valueOf("SUSPENDED"), Particle.valueOf("SUSPENDED_DEPTH"), Particle.valueOf("WATER_BUBBLE")));
    }

    public PlayEffectCommand() {
        setName("playeffect");
        setSyntax("playeffect [effect:<name>] [at:<location>|...] (data:<#.#>) (special_data:<map>) (visibility:<#.#>) (quantity:<#>) (offset:<#.#>,<#.#>,<#.#>) (targets:<player>|...) (velocity:<vector>)");
        setRequiredArguments(2, 8);
        isProcedural = false;
    }

    // <--[language]
    // @name Particle Effects
    // @group Useful Lists
    // @description
    // All the effects listed here can be used by <@link command PlayEffect> to display visual effects or play sounds
    //
    // Effects:
    // - Everything on <@link url https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Particle.html>
    // - Everything on <@link url https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Effect.html>
    // - RANDOM (chooses a random visual effect from the Particle list)
    // -->

    // <--[command]
    // @Name PlayEffect
    // @Syntax playeffect [effect:<name>] [at:<location>|...] (data:<#.#>) (special_data:<map>) (visibility:<#.#>) (quantity:<#>) (offset:<#.#>,<#.#>,<#.#>) (targets:<player>|...) (velocity:<vector>)
    // @Required 2
    // @Maximum 8
    // @Short Plays a visible or audible effect at the location.
    // @Synonyms Particle
    // @Group world
    //
    // @Description
    // Allows the playing of particle effects anywhere without the need of the source it comes from originally.
    // The particles you may use, can come from sources such as a potion effect or a portal/Enderman with their particles respectively.
    // Some particles have different data which may include different behavior depending on the data. Default data is 0
    // Specifying a visibility value changes the sight radius of the effect. For example if visibility is 15; Targeted players won't see it unless they are 15 blocks or closer.
    // You can add a quantity value that allow multiple of the same effect played at the same time. If an offset is set, each particle will be played at a different location in the offset area.
    // Everyone will see the particle effects unless a target has been specified.
    // See <@link language Particle Effects> for a list of valid effect names.
    //
    // Version change note: The original PlayEffect command raised all location inputs 1 block-height upward to avoid effects playing underground when played at eg a player's location.
    // This was found to cause too much confusion, so it is no longer on by default. However, it will still happen for older commands.
    // The distinction is in whether you include the (now expected to use) "at:" prefix on your location argument.
    // If you do not have this prefix, the system will assume your command is older, and will apply the 1-block height offset.
    //
    // Some particles will require input to the "special_data" argument. The data input is a MapTag with different keys per particle.
    // - For DUST particles, the input is of format [size=<size>;color=<color>], e.g. "[size=1.2;color=red]".
    // - For DUST_COLOR_TRANSITION particles, the input is of format [size=<size>;from=<color>;to=<color>], e.g "[size=1.2;from=red;to=blue]".
    // - For BLOCK, BLOCK_CRUMBLE, BLOCK_MARKER, DUST_PILLAR, or FALLING_DUST particles, the input is of format [material=<material>], e.g. [material=stone]
    // - For VIBRATION particles, the input is of format [origin=<location>;destination=<location/entity>;duration=<duration>], for example: [origin=<player.location>;destination=<player.cursor_on>;duration=5s]
    // - For ITEM particles, the input is of format [item=<item>], e.g. "[item=stick]".
    // - For TRAIL particles, the input is of format [color=<color>;target=<location>;duration=<duration>], e.g. "[color=red;target=<player.cursor_on>;duration=20s]".
    // - For ENTITY_EFFECT particles, the input is of format [color=<color>], e.g. "[color=green]".
    // - For SHRIEK particles, the input is of format [duration=<duration>], e.g. "[duration=1m]".
    // - For SCULK_CHARGE particles, the input is of format [radians=<element>], e.g. "[radians=<element[90].to_radians>]".
    //
    // Optionally specify a velocity vector for standard particles to move. Note that this ignores the 'data' input if used.
    //
    // @Tags
    // <server.effect_types>
    // <server.particle_types>
    //
    // @Usage
    // Use to create a fake explosion.
    // - playeffect effect:EXPLOSION_HUGE at:<player.location> visibility:500 quantity:10 offset:2.0
    //
    // @Usage
    // Use to play a cloud effect.
    // - playeffect effect:CLOUD at:<player.location.add[0,5,0]> quantity:20 data:1 offset:0.0
    //
    // @Usage
    // Use to play some effects at spawn.
    // - playeffect effect:FIREWORKS_SPARK at:<world[world].spawn_location> visibility:100 quantity:375 data:0 offset:50.0
    //
    // @Usage
    // Use to spawn a cloud of rainbow-colored ENTITY_EFFECT particles around yourself.
    // - foreach <util.color_names> as:color:
    //     - playeffect effect:ENTITY_EFFECT at:<player.eye_location> quantity:25 special_data:[color=<[color]>]
    //
    // @Usage
    // Use to shoot particles in to the direction you're looking at.
    // - repeat 10:
    //     - playeffect effect:TRAIL at:<player.eye_location> quantity:1 offset:0 special_data:[color=red;target=<player.eye_location.ray_trace[default=air]>;duration=5s]
    //     - wait 1t
    //
    // @Usage
    // Use to spawn a SCULK_CHARGE effect upside down.
    // - playeffect effect:SCULK_CHARGE at:<player.eye_location.add[0,1,0]> quantity:1 offset:0 special_data:[radians=<element[180].to_radians>]
    //
    // @Usage
    // Use to play a SHRIEK effect with a 5-second delay.
    // - playeffect effect:SHRIEK at:<player.eye_location.add[0,1,0]> quantity:1 special_data:[duration=5s]
    //
    // -->

    @Override
    public void addCustomTabCompletions(TabCompletionsBuilder tab) {
        tab.addWithPrefix("effect:", Particle.values());
        tab.addWithPrefix("effect:", Effect.values());
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : scriptEntry) {
            if (!scriptEntry.hasObject("location")
                    && arg.matchesArgumentList(LocationTag.class)
                    && (arg.matchesPrefix("at") || !arg.hasPrefix())) {
                if (arg.matchesPrefix("at")) {
                    scriptEntry.addObject("no_offset", new ElementTag(true));
                }
                scriptEntry.addObject("location", arg.asType(ListTag.class).filter(LocationTag.class, scriptEntry));
                continue;
            }
            else if (!scriptEntry.hasObject("effect") &&
                    !scriptEntry.hasObject("particleeffect") &&
                    !scriptEntry.hasObject("iconcrack")) {
                String particleName = CoreUtilities.toUpperCase(arg.getValue());
                Particle particle = Utilities.elementToEnumlike(new ElementTag(particleName), Particle.class);
                if (particle != null) {
                    scriptEntry.addObject("particleeffect", particle);
                    continue;
                }
                particle = LegacyParticleNaming.legacyParticleNames.get(particleName);
                if (particle != null) {
                    BukkitImplDeprecations.oldSpigotNames.warn(scriptEntry);
                    scriptEntry.addObject("particleeffect", particle);
                    continue;
                }
                if (arg.matches("barrier") && NMSHandler.getVersion().isAtLeast(NMSVersion.v1_18)) {
                    scriptEntry.addObject("particleeffect", Particle.BLOCK_MARKER);
                    scriptEntry.addObject("special_data", new ElementTag("barrier"));
                    continue;
                }
                else if (arg.matches("random")) {
                    // Get another effect if "RANDOM" is used
                    scriptEntry.addObject("particleeffect", VISIBLE_PARTICLES.get(CoreUtilities.getRandom().nextInt(VISIBLE_PARTICLES.size())));
                    continue;
                }
                else if (arg.startsWith("iconcrack_")) {
                    BukkitImplDeprecations.oldPlayEffectSpecials.warn(scriptEntry);
                    // Allow iconcrack_[item] for item break effects (ex: iconcrack_stone)
                    String shrunk = arg.getValue().substring("iconcrack_".length());
                    ItemTag item = ItemTag.valueOf(shrunk, scriptEntry.context);
                    if (item != null) {
                        scriptEntry.addObject("iconcrack", item);
                    }
                    else {
                        Debug.echoError("Invalid iconcrack_[item]. Must be a valid ItemTag!");
                    }
                    continue;
                }
                else if (arg.matchesEnum(Effect.class)) {
                    scriptEntry.addObject("effect", Effect.valueOf(arg.getValue().toUpperCase()));
                    continue;
                }
            }
            if (!scriptEntry.hasObject("radius")
                    && arg.matchesFloat()
                    && arg.matchesPrefix("visibility", "v", "radius", "r")) {
                scriptEntry.addObject("radius", arg.asElement());
            }
            else if (!scriptEntry.hasObject("data")
                    && arg.matchesFloat()
                    && arg.matchesPrefix("data", "d")) {
                scriptEntry.addObject("data", arg.asElement());
            }
            else if (!scriptEntry.hasObject("special_data")
                    && arg.matchesPrefix("special_data")) {
                scriptEntry.addObject("special_data", arg.asElement());
            }
            else if (!scriptEntry.hasObject("quantity")
                    && arg.matchesInteger()
                    && arg.matchesPrefix("qty", "q", "quantity")) {
                if (arg.matchesPrefix("q", "qty")) {
                    BukkitImplDeprecations.qtyTags.warn(scriptEntry);
                }
                scriptEntry.addObject("quantity", arg.asElement());
            }
            else if (!scriptEntry.hasObject("offset")
                    && arg.matchesFloat()
                    && arg.matchesPrefix("offset", "o")) {
                double offset = arg.asElement().asDouble();
                scriptEntry.addObject("offset", new LocationTag(null, offset, offset, offset));
            }
            else if (!scriptEntry.hasObject("offset")
                    && arg.matchesArgumentType(LocationTag.class)
                    && arg.matchesPrefix("offset", "o")) {
                scriptEntry.addObject("offset", arg.asType(LocationTag.class));
            }
            else if (!scriptEntry.hasObject("velocity")
                    && arg.matchesArgumentType(LocationTag.class)
                    && arg.matchesPrefix("velocity")) {
                scriptEntry.addObject("velocity", arg.asType(LocationTag.class));
            }
            else if (!scriptEntry.hasObject("targets")
                    && arg.matchesArgumentList(PlayerTag.class)
                    && arg.matchesPrefix("targets", "target", "t")) {
                scriptEntry.addObject("targets", arg.asType(ListTag.class).filter(PlayerTag.class, scriptEntry));
            }
            else {
                arg.reportUnhandled();
            }
        }
        scriptEntry.defaultObject("data", new ElementTag(0));
        scriptEntry.defaultObject("radius", new ElementTag(15));
        scriptEntry.defaultObject("quantity", new ElementTag(1));
        scriptEntry.defaultObject("offset", new LocationTag(null, 0.5, 0.5, 0.5));
        if (!scriptEntry.hasObject("effect") &&
                !scriptEntry.hasObject("particleeffect") &&
                !scriptEntry.hasObject("iconcrack")) {
            throw new InvalidArgumentsException("Missing effect argument!");
        }
        if (!scriptEntry.hasObject("location")) {
            throw new InvalidArgumentsException("Missing location argument!");
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        List<LocationTag> locations = (List<LocationTag>) scriptEntry.getObject("location");
        List<PlayerTag> targets = (List<PlayerTag>) scriptEntry.getObject("targets");
        Effect effect = (Effect) scriptEntry.getObject("effect");
        Particle particleEffect = (Particle) scriptEntry.getObject("particleeffect");
        ItemTag iconcrack = scriptEntry.getObjectTag("iconcrack");
        ElementTag radius = scriptEntry.getElement("radius");
        ElementTag data = scriptEntry.getElement("data");
        ElementTag quantity = scriptEntry.getElement("quantity");
        ElementTag no_offset = scriptEntry.getElement("no_offset");
        boolean should_offset = no_offset == null || !no_offset.asBoolean();
        LocationTag offset = scriptEntry.getObjectTag("offset");
        ElementTag special_data = scriptEntry.getElement("special_data");
        LocationTag velocity = scriptEntry.getObjectTag("velocity");
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), (effect != null ? db("effect", effect.name()) : particleEffect != null ? db("special effect", particleEffect.name()) : iconcrack),
                    db("locations", locations), db("targets", targets), radius, data, quantity, offset, special_data, velocity, (should_offset ? db("note", "Location will be offset 1 block-height upward (see documentation)") : ""));
        }
        for (LocationTag location : locations) {
            if (should_offset) {
                // Slightly increase the location's Y so effects don't seem to come out of the ground
                location = new LocationTag(location.clone().add(0, 1, 0));
            }
            // Play the Bukkit effect the number of times specified
            if (effect != null) {
                for (int n = 0; n < quantity.asInt(); n++) {
                    if (targets != null) {
                        for (PlayerTag player : targets) {
                            if (player.isValid() && player.isOnline()) {
                                player.getPlayerEntity().playEffect(location, effect, data.asInt());
                            }
                        }
                    }
                    else {
                        location.getWorld().playEffect(location, effect, data.asInt(), radius.asInt());
                    }
                }
            }
            // Play a ParticleEffect
            else if (particleEffect != null) {
                List<Player> players = new ArrayList<>();
                if (targets == null) {
                    float rad = radius.asFloat();
                    for (Player player : location.getWorld().getPlayers()) {
                        if (player.getLocation().distanceSquared(location) < rad * rad) {
                            players.add(player);
                        }
                    }
                }
                else {
                    for (PlayerTag player : targets) {
                        if (player.isValid() && player.isOnline()) {
                            players.add(player.getPlayerEntity());
                        }
                    }
                }
                Class<?> clazz = particleEffect.getDataType() == Void.class ? null : particleEffect.getDataType();
                Object dataObject = null;
                if (clazz != null) {
                    if (special_data == null) {
                        Debug.echoError("Missing required special data for particle: " + particleEffect.name());
                        return;
                    }
                    MapTag dataMap = MapTag.valueOf(special_data.asString(), scriptEntry.getContext());
                    if (dataMap == null) {
                        ListTag dataList = ListTag.valueOf(special_data.asString(), scriptEntry.getContext());
                        BukkitImplDeprecations.playEffectSpecialDataListInput.warn(scriptEntry.getContext());
                        if (clazz == Particle.DustOptions.class) {
                            if (dataList.size() != 2) {
                                Debug.echoError("DustOptions special_data must have 2 list entries for particle: " + particleEffect.name());
                                return;
                            }
                            float size = Float.parseFloat(dataList.get(0));
                            ColorTag color = ColorTag.valueOf(dataList.get(1), scriptEntry.context);
                            dataObject = new Particle.DustOptions(BukkitColorExtensions.getColor(color), size);
                        }
                        else if (clazz == BlockData.class) {
                            MaterialTag blockMaterial = MaterialTag.valueOf(special_data.asString(), scriptEntry.getContext());
                            dataObject = blockMaterial.getModernData();
                        }
                        else if (clazz == ItemStack.class) {
                            ItemTag itemType = ItemTag.valueOf(special_data.asString(), scriptEntry.getContext());
                            dataObject = itemType.getItemStack();
                        }
                        else if (clazz == Particle.DustTransition.class) {
                            if (dataList.size() != 3) {
                                Debug.echoError("DustTransition special_data must have 3 list entries for particle: " + particleEffect.name());
                                return;
                            }
                            else {
                                float size = Float.parseFloat(dataList.get(0));
                                ColorTag fromColor = ColorTag.valueOf(dataList.get(1), scriptEntry.context);
                                ColorTag toColor = ColorTag.valueOf(dataList.get(2), scriptEntry.context);
                                dataObject = new Particle.DustTransition(BukkitColorExtensions.getColor(fromColor), BukkitColorExtensions.getColor(toColor), size);
                            }
                        }
                        else if (clazz == Vibration.class) {
                            if (dataList.size() != 3) {
                                Debug.echoError("Vibration special_data must have 3 list entries for particle: " + particleEffect.name());
                                return;
                            }
                            else {
                                DurationTag duration = dataList.getObject(0).asType(DurationTag.class, scriptEntry.context);
                                LocationTag origin = dataList.getObject(1).asType(LocationTag.class, scriptEntry.context);
                                ObjectTag destination = dataList.getObject(2);
                                Vibration.Destination destObj;
                                if (destination.shouldBeType(EntityTag.class)) {
                                    destObj = new Vibration.Destination.EntityDestination(destination.asType(EntityTag.class, scriptEntry.context).getBukkitEntity());
                                }
                                else {
                                    destObj = new Vibration.Destination.BlockDestination(destination.asType(LocationTag.class, scriptEntry.context));
                                }
                                dataObject = new Vibration(origin, destObj, duration.getTicksAsInt());
                            }
                        }
                        else {
                            Debug.echoError("Unknown particle data type: " + clazz.getCanonicalName() + " for particle: " + particleEffect.name() + ". Are you sure it exists and are not using a legacy format?");
                            return;
                        }
                    }
                    else {
                        if (clazz == Particle.DustOptions.class) {
                            ElementTag size = dataMap.getObjectAs("size", ElementTag.class, scriptEntry.context);
                            if (size == null || !size.isFloat()) {
                                Debug.echoError("special_data input must have a 'size' key with a valid number, for particle " + particleEffect.name());
                                return;
                            }
                            ColorTag color = dataMap.getObjectAs("color", ColorTag.class, scriptEntry.context);
                            if (color == null) {
                                Debug.echoError("special_data input must have a 'color' key with a valid ColorTag, for particle: " + particleEffect.name());
                                return;
                            }
                            dataObject = new Particle.DustOptions(BukkitColorExtensions.getColor(color), size.asFloat());
                        }
                        else if (clazz == BlockData.class) {
                            MaterialTag blockMaterial = dataMap.getObjectAs("material", MaterialTag.class, scriptEntry.context);
                            if (blockMaterial == null) {
                                Debug.echoError("special_data input must have a 'material' key with a valid MaterialTag, for particle: " + particleEffect.name());
                                return;
                            }
                            dataObject = blockMaterial.getModernData();
                        }
                        else if (clazz == ItemStack.class) {
                            ItemTag itemType = dataMap.getObjectAs("item", ItemTag.class, scriptEntry.context);
                            if (itemType == null) {
                                Debug.echoError("special_data input must have a 'item' key with a valid ItemTag, for particle: " + particleEffect.name());
                                return;
                            }
                            dataObject = itemType.getItemStack();
                        }
                        else if (clazz == Particle.DustTransition.class) {
                            ElementTag size = dataMap.getObjectAs("size", ElementTag.class, scriptEntry.context);
                            if (size == null || !size.isFloat()) {
                                Debug.echoError("special_data input must have a 'size' key with a valid number, for particle: " + particleEffect.name());
                                return;
                            }
                            ColorTag fromColor = dataMap.getObjectAs("from", ColorTag.class, scriptEntry.context);
                            ColorTag toColor = dataMap.getObjectAs("to", ColorTag.class, scriptEntry.context);
                            if (fromColor == null || toColor == null) {
                                Debug.echoError("special_data input must have a 'to' and 'size' key with a valid ColorTag, for particle: " + particleEffect.name());
                                return;
                            }
                            dataObject = new Particle.DustTransition(BukkitColorExtensions.getColor(fromColor), BukkitColorExtensions.getColor(toColor), size.asFloat());
                        }
                        else if (clazz == Vibration.class) {
                            DurationTag duration = dataMap.getObjectAs("duration", DurationTag.class, scriptEntry.context);
                            if (duration == null) {
                                Debug.echoError("special_data input must have a 'duration' key with a valid LocationTag, for particle: " + particleEffect.name());
                                return;
                            }
                            LocationTag origin = dataMap.getObjectAs("origin", LocationTag.class, scriptEntry.context);
                            if (origin == null) {
                                Debug.echoError("special_data input must have a 'origin' key with a valid LocationTag, for particle: " + particleEffect.name());
                                return;
                            }
                            ObjectTag destination = dataMap.getObjectAs("destination", ObjectTag.class, scriptEntry.context);
                            Vibration.Destination destObj;
                            if (destination.shouldBeType(EntityTag.class)) {
                                destObj = new Vibration.Destination.EntityDestination(destination.asType(EntityTag.class, scriptEntry.context).getBukkitEntity());
                            }
                            else if (destination.shouldBeType(LocationTag.class)) {
                                destObj = new Vibration.Destination.BlockDestination(destination.asType(LocationTag.class, scriptEntry.context));
                            }
                            else {
                                Debug.echoError("special_data input must have a 'destination' key with a valid LocationTag or EntityTag, for particle: " + particleEffect.name());
                                return;
                            }
                            dataObject = new Vibration(origin, destObj, duration.getTicksAsInt());
                        }
                        else if (clazz == Particle.Trail.class) {
                            ColorTag color = dataMap.getObjectAs("color", ColorTag.class, scriptEntry.context);
                            if (color == null) {
                                Debug.echoError("special_data input must have a 'color' key with a valid ColorTag, for particle: " + particleEffect.name());
                                return;
                            }
                            LocationTag target = dataMap.getObjectAs("target", LocationTag.class, scriptEntry.context);
                            if (target == null) {
                                Debug.echoError("special_data input must have a 'target' key with a valid LocationTag, for particle: " + particleEffect.name());
                                return;
                            }
                            DurationTag duration = dataMap.getObjectAs("duration", DurationTag.class, scriptEntry.context);
                            if (duration == null) {
                                Debug.echoError("special_data input must have a 'duration' key with a valid DurationTag, for particle: " + particleEffect.name());
                                return;
                            }
                            dataObject = new Particle.Trail(target, BukkitColorExtensions.getColor(color), duration.getTicksAsInt());
                        }
                        else if (clazz == Color.class) {
                            ColorTag color = dataMap.getObjectAs("color", ColorTag.class, scriptEntry.context);
                            if (!dataMap.getObject("color").canBeType(ColorTag.class)) {
                                Debug.echoError("special_data input must have a 'color' key with a valid ColorTag, for particle: " + particleEffect.name());
                                return;
                            }
                            dataObject = BukkitColorExtensions.getColor(color);
                        }
                        else if (clazz == Integer.class) {
                            DurationTag duration = dataMap.getObjectAs("duration", DurationTag.class, scriptEntry.context);
                            if (duration == null) {
                                Debug.echoError("special_data input must have a 'duration' key with a valid DurationTag, for particle: " + particleEffect.name());
                                return;
                            }
                            dataObject = duration.getTicksAsInt();
                        }
                        else if (clazz == Float.class) {
                            ElementTag radians = dataMap.getObjectAs("radians", ElementTag.class, scriptEntry.context);
                            if (radians == null || !radians.isFloat()) {
                                Debug.echoError("special_data input must have a 'radians' key with a valid number, for particle: " + particleEffect.name());
                                return;
                            }
                            dataObject = radians.asFloat();
                        }
                        else {
                            Debug.echoError("Unknown particle data type: " + clazz.getCanonicalName() + " for particle: " + particleEffect.name());
                            return;
                        }
                    }
                }
                else if (special_data != null) {
                    Debug.echoError("Particles of type '" + particleEffect.name() + "' cannot take special_data as input.");
                    return;
                }
                Random random = CoreUtilities.getRandom();
                int quantityInt = quantity.asInt();
                for (Player player : players) {
                    if (velocity == null) {
                        player.spawnParticle(particleEffect, location, quantityInt, offset.getX(), offset.getY(), offset.getZ(), data.asDouble(), dataObject);
                    }
                    else {
                        for (int i = 0; i < quantityInt; i++) {
                            LocationTag singleLocation = location.clone().add((random.nextDouble() - 0.5) * offset.getX(),
                                    (random.nextDouble() - 0.5) * offset.getY(),
                                    (random.nextDouble() - 0.5) * offset.getZ());
                            player.spawnParticle(particleEffect, singleLocation, 0, velocity.getX(), velocity.getY(), velocity.getZ(), 1, dataObject);
                        }
                    }
                }
            }
            // Play an iconcrack (item break) effect
            else {
                List<Player> players = new ArrayList<>();
                if (targets == null) {
                    float rad = radius.asFloat();
                    for (Player player : location.getWorld().getPlayers()) {
                        if (player.getLocation().distanceSquared(location) < rad * rad) {
                            players.add(player);
                        }
                    }
                }
                else {
                    for (PlayerTag player : targets) {
                        if (player.isValid() && player.isOnline()) {
                            players.add(player.getPlayerEntity());
                        }
                    }
                }
                if (iconcrack != null) {
                    ItemStack itemStack = iconcrack.getItemStack();
                    Particle particle = Particle.valueOf("ITEM_CRACK");
                    for (Player player : players) {
                        player.spawnParticle(particle, location, quantity.asInt(), offset.getX(), offset.getY(), offset.getZ(), data.asFloat(), itemStack);
                    }
                }
            }
        }
    }
}
