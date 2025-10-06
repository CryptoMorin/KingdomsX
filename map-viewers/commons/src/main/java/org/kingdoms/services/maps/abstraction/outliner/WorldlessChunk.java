package org.kingdoms.services.maps.abstraction.outliner;

import org.jetbrains.annotations.NotNull;
import org.kingdoms.server.location.Vector2;

public final class WorldlessChunk {
    public final int x, z;

    public WorldlessChunk(int x, int z) {
        this.x = x;
        this.z = z;
    }

    /**
     * We need this to prevent borders from connecting diagonally, otherwise when all points of our
     * polygon are plotted, sharp turns will be visible because the renderer thinks that it should
     * "fill" the polygon differently.
     */
    private static final double BOUNDING_LESS = 0.1;

    @NotNull
    public Vector2 getLowerLeft(int chunkSize) {
        return getPoint(chunkSize, 0, 0);
    }

    @NotNull
    public Vector2 getLowerRight(int chunkSize) {
        return getPoint(chunkSize, chunkSize - BOUNDING_LESS, 0);
    }

    @NotNull
    public Vector2 getUpperLeft(int chunkSize) {
        return getPoint(chunkSize, 0, chunkSize - BOUNDING_LESS);
    }

    @NotNull
    public Vector2 getUpperRight(int chunkSize) {
        return getPoint(chunkSize, chunkSize - BOUNDING_LESS, chunkSize - BOUNDING_LESS);
    }

    private Vector2 getPoint(double chunkSize, double offsetX, double offsetZ) {
        return Vector2.of((x * chunkSize) + offsetX, (z * chunkSize) + offsetZ);
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object obj) {
        // We don't need to check the type, if it doesn't match, something more
        // weird is going on that needs fixing instead of this.
        WorldlessChunk otherChunk = (WorldlessChunk) obj;
        return x == otherChunk.x && z == otherChunk.z;
    }

    @Override
    public int hashCode() {
        return x * 31 + z;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + '(' + x + ", " + z + ')';
    }

    @SuppressWarnings("ClassEscapesDefinedScope")
    protected WorldlessChunk offset(ChunkDirection direction) {
        return offset(direction.x, direction.z);
    }

    protected WorldlessChunk offset(int xOffset, int zOffset) {
        return new WorldlessChunk(this.x + xOffset, this.z + zOffset);
    }
}
