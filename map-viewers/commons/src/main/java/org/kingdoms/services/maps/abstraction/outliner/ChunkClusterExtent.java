package org.kingdoms.services.maps.abstraction.outliner;

import java.util.Collection;

/**
 * Represents a square that outlines a group of chunks together.
 */
final class ChunkClusterExtent {
    protected int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
    protected int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

    private ChunkClusterExtent() {}

    private void accumulate(WorldlessChunk chunk) {
        if (chunk.x < minX) minX = chunk.x;
        if (chunk.x > maxX) maxX = chunk.x;
        if (chunk.z < minZ) minZ = chunk.z;
        if (chunk.z > maxZ) maxZ = chunk.z;
    }

    public static ChunkClusterExtent getExtent(Collection<WorldlessChunk> chunks) {
        ChunkClusterExtent extent = new ChunkClusterExtent();

        for (WorldlessChunk chunk : chunks) {
            extent.accumulate(chunk);
        }

        return extent;
    }
}
