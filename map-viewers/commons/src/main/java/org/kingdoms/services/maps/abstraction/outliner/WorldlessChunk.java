package org.kingdoms.services.maps.abstraction.outliner;

import org.jetbrains.annotations.NotNull;
import org.kingdoms.server.location.Vector2;

public final class WorldlessChunk {
    public final int x, z;

    public WorldlessChunk(int x, int z) {
        this.x = x;
        this.z = z;
    }

    @NotNull
    public Vector2 getLowerLeft(int chunkSize) {
        return getPoint(chunkSize, 0, 0);
    }

    @NotNull
    public Vector2 getLowerRight(int chunkSize) {
        return getPoint(chunkSize, chunkSize, 0);
    }

    @NotNull
    public Vector2 getUpperLeft(int chunkSize) {
        return getPoint(chunkSize, 0, chunkSize);
    }

    @NotNull
    public Vector2 getUpperRight(int chunkSize) {
        return getPoint(chunkSize, chunkSize, chunkSize);
    }

    private Vector2 getPoint(int chunkSize, int offsetX, int offsetZ) {
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

    private WorldlessChunk offset(int xOffset, int zOffset) {
        return new WorldlessChunk(this.x + xOffset, this.z + zOffset);
    }
}
