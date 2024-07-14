package org.kingdoms.enginehub.worldguard;

public final class CuboidRegionProperties {
    public final int minX, minZ, maxX, maxZ;

    public CuboidRegionProperties(int minX, int minZ, int maxX, int maxZ) {
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
    }
}