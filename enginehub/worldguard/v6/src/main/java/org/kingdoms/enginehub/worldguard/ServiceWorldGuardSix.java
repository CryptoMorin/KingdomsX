package org.kingdoms.enginehub.worldguard;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.bukkit.RegionContainer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

public final class ServiceWorldGuardSix extends ServiceWorldGuard {
    @Override
    public ProtectedRegion isLocationInRegion(World world, CuboidRegionProperties properties) {
        try {
            BlockVector pos1 = BlockVector.toBlockPoint(properties.minX, 0, properties.minZ);
            BlockVector pos2 = BlockVector.toBlockPoint(properties.maxX, world.getMaxHeight(), properties.maxZ);

            ProtectedCuboidRegion region = new ProtectedCuboidRegion(ServiceWorldGuard.CHECK_REGION_ID, pos1, pos2);
            RegionManager manager = getRegionManager(world);
            ApplicableRegionSet regions = manager.getApplicableRegions(region);

            if (regions.size() == 0) return null;
            return regions.iterator().next();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean isLocationInRegion(Location location, String regionName) {
        try {
            com.sk89q.worldguard.protection.managers.RegionManager manager = getRegionManager(location.getWorld());
            if (manager == null) return false;

            // Object pt1 = getToBlockPoint.invoke(location.getX(), location.getY(), location.getZ());
            com.sk89q.worldguard.protection.ApplicableRegionSet regions = (ApplicableRegionSet) manager.getApplicableRegions(location);
            for (ProtectedRegion region : regions.getRegions()) {
                if (region.getId().equals(regionName)) return false;
            }
            return false;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return false;
        }
    }

    public RegionContainer getRegionContainer() {
        return WorldGuardPlugin.inst().getRegionContainer();
    }

    @Override
    protected FlagRegistry getFlagRegistry() {
        return WorldGuardPlugin.inst().getFlagRegistry();
    }

    @Override
    protected @Nullable RegionManager getRegionManager(@NonNull World world) {
        Objects.requireNonNull(world, "Cannot get WorldGuard region manager from a null world");
        try {
            return getRegionContainer().get(world);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean hasFlag(Player player, Location location, Flag<?> flag) {
        Objects.requireNonNull(player);
        Objects.requireNonNull(location);
        Objects.requireNonNull(flag);

        com.sk89q.worldguard.protection.managers.RegionManager manager = getRegionManager(location.getWorld());
        if (manager == null) return false;

        return manager.getApplicableRegions(location)
                .queryValue(WorldGuardPlugin.inst().wrapPlayer(player), flag) == StateFlag.State.ALLOW;
    }
}
