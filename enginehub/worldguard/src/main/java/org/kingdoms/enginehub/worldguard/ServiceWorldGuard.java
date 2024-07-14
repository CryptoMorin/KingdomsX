package org.kingdoms.enginehub.worldguard;

import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.kingdoms.services.Service;

import java.util.HashSet;
import java.util.Set;

public abstract class ServiceWorldGuard implements Service {
    private static StateFlag KINGDOMS_CLAIMABLE, KINGDOMS_FRIENDLY_FIRE, KINGDOMS_DAMAGE_CHAMPION;
    protected static final String CHECK_REGION_ID = "ChunkRegion";

    public void registerFlags() {
        StateFlag claimable, friendlyFire, damageChampion;
        try {
            claimable = registerFlag("kingdoms-claimable", false);
            friendlyFire = registerFlag("kingdoms-friendly-fire", false);
            damageChampion = registerFlag("kingdoms-damage-champion", true);
        } catch (Throwable ex) {
            throw new RuntimeException("Failed to register WorldGuard flags", ex);
        }

        KINGDOMS_CLAIMABLE = claimable;
        KINGDOMS_FRIENDLY_FIRE = friendlyFire;
        KINGDOMS_DAMAGE_CHAMPION = damageChampion;
    }

    @Override
    public final Throwable checkAvailability() {
        try {
            getFlagRegistry();
            return null;
        } catch (Throwable ex) {
            return ex;
        }
    }

    public StateFlag registerFlag(String name, boolean defaultState) {
        // https://worldguard.enginehub.org/en/latest/developer/regions/custom-flags/
        FlagRegistry registry = this.getFlagRegistry();

        try {
            // https://github.com/EngineHub/WorldGuard/blob/master/worldguard-core/src/main/java/com/sk89q/worldguard/protection/flags/StateFlag.java
            // create a flag with the name "my-custom-flag", defaulting to true
            // only set our field if there was no error
            // Default flag causes the getFlag() method to return:
            // true -> ALLOW
            // false -> null
            StateFlag flag = new StateFlag(name, defaultState);
            registry.register(flag);
            return flag;
        } catch (FlagConflictException e) {
            // some other plugin registered a flag by the same name already.
            // you can use the existing flag, but this may cause conflicts - be sure to check type
            e.printStackTrace();
            Flag<?> existing = registry.get(name);
            if (existing instanceof StateFlag) return (StateFlag) existing;
            return null;
        }
    }

    public final boolean isChunkInRegion(World world, int x, int z, int radius) {
        if (radius < 0) throw new IllegalArgumentException("Cannot check chunk in regions with radius: " + radius);

        int chunkMX = x - radius;
        int chunkMZ = z - radius;

        int chunkPX = x + radius;
        int chunkPZ = z + radius;

        int minX = chunkMX << 4;
        int minZ = chunkMZ << 4;

        int maxX = (chunkPX << 4) + 15;
        int maxZ = (chunkPZ << 4) + 15;

        CuboidRegionProperties properties = new CuboidRegionProperties(minX, minZ, maxX, maxZ);
        ProtectedRegion region = isLocationInRegion(world, properties);
        return region != null && region.getFlag(KINGDOMS_CLAIMABLE) != StateFlag.State.ALLOW;
    }

    public boolean hasRegion(@NonNull World world, String region) {
        RegionManager regionManager = getRegionManager(world);
        return regionManager != null && regionManager.hasRegion(region);
    }

    public Flag<?> getFlag(String name) {
        return getFlagRegistry().get(name);
    }

    @Nullable
    protected abstract RegionManager getRegionManager(@NonNull World world);

    @NonNull
    public Set<String> getRegions(World world) {
        RegionManager manager = getRegionManager(world);
        return manager == null ? new HashSet<>() : manager.getRegions().keySet();
    }

    protected abstract FlagRegistry getFlagRegistry();

    public abstract boolean hasFlag(Player player, Location location, Flag<?> flag);

    public final boolean hasFriendlyFireFlag(Player player) {
        return hasFlag(player, KINGDOMS_FRIENDLY_FIRE);
    }

    private final boolean hasFlag(Player player, StateFlag flag) {
        return hasFlag(player, player.getLocation(), flag);
    }

    public final boolean canDamageChampion(Player player) {
        return hasFlag(player, KINGDOMS_DAMAGE_CHAMPION);
    }

    public abstract boolean isLocationInRegion(Location location, String regionName);

    public boolean canFly(Player player, Location location) {
        // https://github.com/aromaa/WorldGuardExtraFlags/blob/master/WG/src/main/java/net/goldtreeservers/worldguardextraflags/flags/Flags.java#L49
        Flag<?> flyFlag = getFlag("fly");
        return flyFlag != null && hasFlag(player, location, flyFlag);
    }

    public abstract ProtectedRegion isLocationInRegion(World world, CuboidRegionProperties properties);
}
