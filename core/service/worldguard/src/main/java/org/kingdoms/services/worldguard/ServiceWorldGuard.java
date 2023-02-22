package org.kingdoms.services.worldguard;

import com.sk89q.worldguard.protection.flags.StateFlag;
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
    protected static final String CHECK_REGION_ID = "ChunkRegion";

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
        return region != null && !isClaimable(region);
    }

    public boolean hasRegion(@NonNull World world, String region) {
        RegionManager regionManager = getRegionManager(world);
        return regionManager != null && regionManager.hasRegion(region);
    }

    @Nullable
    protected abstract RegionManager getRegionManager(@NonNull World world);

    @NonNull
    public Set<String> getRegions(World world) {
        RegionManager manager = getRegionManager(world);
        return manager == null ? new HashSet<>() : manager.getRegions().keySet();
    }


    public abstract boolean hasFlag(Player player, Location location, StateFlag flag);

    public StateFlag getFriendlyFireFlag() {return null;}

    public final boolean hasFriendlyFireFlag(Player player) {
        return hasFlag(player, player.getLocation(), getFriendlyFireFlag());
    }

    public abstract boolean isLocationInRegion(Location location, String regionName);

    public abstract ProtectedRegion isLocationInRegion(World world, CuboidRegionProperties properties);

    public abstract boolean isClaimable(ProtectedRegion region);
}
