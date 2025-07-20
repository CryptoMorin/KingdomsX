package org.kingdoms.services.maps.abstraction.outliner;

import java.util.stream.Collector;

/**
 * Represents a square that outlines a group of chunks together.
 */
final class ChunkClusterExtent {
    public int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
    public int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

    private ChunkClusterExtent() {}

    private void accumulate(WorldlessChunk chunk) {
        if (chunk.x < minX) minX = chunk.x;
        if (chunk.x > maxX) maxX = chunk.x;
        if (chunk.z < minZ) minZ = chunk.z;
        if (chunk.z > maxZ) maxZ = chunk.z;
    }

    private ChunkClusterExtent combine(ChunkClusterExtent edges) {
        if (edges.minX < this.minX) this.minX = edges.minX;
        if (edges.maxX > this.maxX) this.maxX = edges.maxX;
        if (edges.minZ < this.minZ) this.minZ = edges.minZ;
        if (edges.maxZ > this.maxZ) this.maxZ = edges.maxZ;
        return this;
    }

    public static Collector<WorldlessChunk, ChunkClusterExtent, ChunkClusterExtent> collect() {
        return Collector.of(
                ChunkClusterExtent::new,
                ChunkClusterExtent::accumulate,
                ChunkClusterExtent::combine,
                Collector.Characteristics.IDENTITY_FINISH, Collector.Characteristics.UNORDERED
        );
    }
}
