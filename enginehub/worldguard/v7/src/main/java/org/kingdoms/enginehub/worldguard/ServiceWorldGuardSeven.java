package org.kingdoms.enginehub.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.index.ConcurrentRegionIndex;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.awt.*;
import java.awt.geom.Area;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Objects;

public final class ServiceWorldGuardSeven extends ServiceWorldGuard {
    private static final MethodHandle INDEX;

    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle index = null;
        try {
            Field field = RegionManager.class.getDeclaredField("index");
            field.setAccessible(true);
            index = lookup.unreflectGetter(field);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        INDEX = index;
    }

    private static Collection<ProtectedRegion> getRegions(RegionManager manager) {
        try {
            ConcurrentRegionIndex index = (ConcurrentRegionIndex) INDEX.invoke(manager);
            return index.values();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return null;
        }
    }

    private static ProtectedRegion doesRegionIntersect(CuboidRegionProperties properties, Collection<ProtectedRegion> regions) {
        Area area = toArea(properties);
        for (ProtectedRegion region : regions) {
            // region.isPhysicalArea() = true
            if (intersectsBoundingBox(properties, region)) {
                Area testArea = toArea(region);
                testArea.intersect(area);
                return !testArea.isEmpty() ? region : null;
            }
        }
        return null;
    }

    private static Area toArea(CuboidRegionProperties properties) {
        int x = properties.minX;
        int z = properties.minZ;

        int width = properties.maxX - x + 1;
        int height = properties.maxZ - z + 1;

        return new Area(new Rectangle(x, z, width, height));
    }

    private static Area toArea(ProtectedRegion region) {
        int x = region.getMinimumPoint().getBlockX();
        int z = region.getMinimumPoint().getBlockZ();
        int width = region.getMaximumPoint().getBlockX() - x + 1;
        int height = region.getMaximumPoint().getBlockZ() - z + 1;
        return new Area(new Rectangle(x, z, width, height));
    }

    @SuppressWarnings("RedundantIfStatement")
    private static boolean intersectsBoundingBox(CuboidRegionProperties properties, ProtectedRegion region) {
        BlockVector3 rMaxPoint = region.getMaximumPoint();
        if (rMaxPoint.getBlockX() < properties.minX) return false;
        if (rMaxPoint.getBlockZ() < properties.minZ) return false;

        BlockVector3 rMinPoint = region.getMinimumPoint();
        if (rMinPoint.getBlockX() > properties.maxX) return false;
        if (rMinPoint.getBlockZ() > properties.maxZ) return false;

        return true;
    }

    @Override
    protected FlagRegistry getFlagRegistry() {
        return WorldGuard.getInstance().getFlagRegistry();
    }

    protected RegionManager getRegionManager(World world) {
        Objects.requireNonNull(world, "Cannot get WorldGuard region manager from a null world");
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        return container.get(BukkitAdapter.adapt(world));
    }

    @SuppressWarnings("unused")
    private boolean isLocationInRegionOld(World world, CuboidRegionProperties properties) {
        RegionManager manager = getRegionManager(world);
        if (manager == null) return false;

        BlockVector3 pos1 = BlockVector3.at(properties.minX, 0, properties.minZ);
        BlockVector3 pos2 = BlockVector3.at(properties.maxX, world.getMaxHeight(), properties.maxZ);

        ProtectedCuboidRegion region = new ProtectedCuboidRegion(ServiceWorldGuard.CHECK_REGION_ID, pos1, pos2);
        ApplicableRegionSet regions = manager.getApplicableRegions(region);

        return regions.size() != 0;
    }

    @Override
    public ProtectedRegion isLocationInRegion(World world, CuboidRegionProperties properties) {
        RegionManager manager = getRegionManager(world);
        if (manager == null) return null;
        return doesRegionIntersect(properties, getRegions(manager));
    }

    @Override
    public boolean isLocationInRegion(Location location, String regionName) {
        Objects.requireNonNull(location);
        Objects.requireNonNull(regionName);

        RegionManager manager = getRegionManager(location.getWorld());
        if (manager == null) return false;

        ApplicableRegionSet regions = manager.getApplicableRegions(BukkitAdapter.asBlockVector(location));
        for (ProtectedRegion region : regions.getRegions()) {
            if (region.getId().equals(regionName)) return true;
        }
        return false;
    }

    @Override
    public boolean hasFlag(Player player, Location location, Flag<?> flag) {
        Objects.requireNonNull(player);
        Objects.requireNonNull(location);
        Objects.requireNonNull(flag);

        RegionManager manager = getRegionManager(location.getWorld());
        if (manager == null) return false;

        ApplicableRegionSet regions = manager.getApplicableRegions(BukkitAdapter.asBlockVector(location));
        return regions.queryValue(WorldGuardPlugin.inst().wrapPlayer(player), flag) == StateFlag.State.ALLOW;
    }
}
