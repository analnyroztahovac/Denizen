package com.denizenscript.denizen.nms.v1_21.helpers;

import com.denizenscript.denizen.nms.interfaces.FishingHelper;
import com.denizenscript.denizen.nms.v1_21.ReflectionMappingsInfo;
import com.denizenscript.denizencore.utilities.ReflectionHelper;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_21_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftFishHook;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_21_R3.inventory.CraftItemStack;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.lang.reflect.Field;
import java.util.List;

public class FishingHelperImpl implements FishingHelper {

    @Override
    public org.bukkit.inventory.ItemStack getResult(FishHook fishHook, CatchType catchType) {
        FishingHook nmsHook = ((CraftFishHook) fishHook).getHandle();
        ItemStack result = switch (catchType) {
            case DEFAULT -> {
                ServerLevel nmsWorld = ((CraftWorld) fishHook.getWorld()).getHandle();
                ItemStack nmsFishingRod = nmsHook.getPlayerOwner().getMainHandItem();
                float f = nmsWorld.random.nextFloat();
                float i = EnchantmentHelper.getFishingTimeReduction(nmsWorld, nmsFishingRod, nmsHook.getPlayerOwner());
                int j = EnchantmentHelper.getFishingLuckBonus(nmsWorld, nmsFishingRod, nmsHook.getPlayerOwner());
                float f1 = 0.1F - i * 0.025F - (float) j * 0.01F;
                float f2 = 0.05F + i * 0.01F - (float) j * 0.01F;

                f1 = Mth.clamp(f1, 0.0F, 1.0F);
                f2 = Mth.clamp(f2, 0.0F, 1.0F);
                if (f < f1) {
                    yield catchRandomJunk(nmsHook);
                }
                else {
                    f -= f1;
                    if (f < f2) {
                        yield catchRandomTreasure(nmsHook);
                    }
                    else {
                        yield catchRandomFish(nmsHook);
                    }
                }
            }
            case JUNK -> catchRandomJunk(nmsHook);
            case TREASURE -> catchRandomTreasure(nmsHook);
            case FISH -> catchRandomFish(nmsHook);
            default -> null;
        };
        return result != null ? CraftItemStack.asBukkitCopy(result) : null;
    }

    public ItemStack getRandomReward(FishingHook nmsHook, ResourceKey<LootTable> key) {
        ServerLevel nmsWorld = (ServerLevel) nmsHook.level();
        LootParams nmsLootParams = new LootParams.Builder(nmsWorld)
                .withParameter(LootContextParams.ORIGIN, new Vec3(nmsHook.getX(), nmsHook.getY(), nmsHook.getZ()))
                .withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD))
                .create(LootContextParamSets.FISHING);
        List<ItemStack> nmsItems = nmsWorld.getServer().reloadableRegistries().getLootTable(key).getRandomItems(nmsLootParams);
        return nmsItems.get(nmsWorld.random.nextInt(nmsItems.size()));
    }

    @Override
    public FishHook spawnHook(Location location, Player player) {
        ServerLevel nmsWorld = ((CraftWorld) location.getWorld()).getHandle();
        FishingHook hook = new FishingHook(((CraftPlayer) player).getHandle(), nmsWorld, 0, 0);
        nmsWorld.addFreshEntity(hook, CreatureSpawnEvent.SpawnReason.CUSTOM);
        return (FishHook) hook.getBukkitEntity();
    }

    private ItemStack catchRandomJunk(FishingHook fishHook) {
        return getRandomReward(fishHook, BuiltInLootTables.FISHING_JUNK);
    }

    private ItemStack catchRandomTreasure(FishingHook fishHook) {
        return getRandomReward(fishHook, BuiltInLootTables.FISHING_TREASURE);
    }

    private ItemStack catchRandomFish(FishingHook fishHook) {
        return getRandomReward(fishHook, BuiltInLootTables.FISHING_FISH);
    }

    public static final Field FISHING_HOOK_NIBBLE = ReflectionHelper.getFields(FishingHook.class).get(ReflectionMappingsInfo.FishingHook_nibble, int.class);
    public static final Field FISHING_HOOK_LURE_TIME = ReflectionHelper.getFields(FishingHook.class).get(ReflectionMappingsInfo.FishingHook_timeUntilLured, int.class);
    public static final Field FISHING_HOOK_HOOK_TIME = ReflectionHelper.getFields(FishingHook.class).get(ReflectionMappingsInfo.FishingHook_timeUntilHooked, int.class);

    @Override
    public FishHook getHookFrom(Player player) {
        FishingHook nmsHook = ((CraftPlayer) player).getHandle().fishing;
        if (nmsHook == null) {
            return null;
        }
        return (FishHook) nmsHook.getBukkitEntity();
    }

    @Override
    public void setNibble(FishHook hook, int ticks) {
        FishingHook nmsHook = ((CraftFishHook) hook).getHandle();
        try {
            FISHING_HOOK_NIBBLE.setInt(nmsHook, ticks);
        }
        catch (Throwable ex) {
            Debug.echoError(ex);
        }
    }

    @Override
    public void setHookTime(FishHook hook, int ticks) {
        FishingHook nmsHook = ((CraftFishHook) hook).getHandle();
        try {
            FISHING_HOOK_HOOK_TIME.setInt(nmsHook, ticks);
        }
        catch (Throwable ex) {
            Debug.echoError(ex);
        }
    }

    @Override
    public int getLureTime(FishHook hook) {
        FishingHook nmsHook = ((CraftFishHook) hook).getHandle();
        try {
            return FISHING_HOOK_LURE_TIME.getInt(nmsHook);
        }
        catch (Throwable ex) {
            Debug.echoError(ex);
        }
        return -1;
    }

    @Override
    public void setLureTime(FishHook hook, int ticks) {
        FishingHook nmsHook = ((CraftFishHook) hook).getHandle();
        try {
            FISHING_HOOK_LURE_TIME.setInt(nmsHook, ticks);
        }
        catch (Throwable ex) {
            Debug.echoError(ex);
        }
    }
}
