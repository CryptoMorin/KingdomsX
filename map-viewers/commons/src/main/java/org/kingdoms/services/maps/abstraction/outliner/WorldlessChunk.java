package org.kingdoms.services.maps.abstraction.outliner;

import org.jetbrains.annotations.NotNull;
import org.kingdoms.server.location.Vector2;

public final class WorldlessChunk {
    public final int x, z;

    public WorldlessChunk(int x, int z) {
        this.x = x;
        this.z = z;
    }

    private int getLLX(int chunkSize) {
        return x * chunkSize;
    }

    private int getLLZ(int chunkSize) {
        return z * chunkSize;
    }

    @NotNull
    public Vector2 getLowerLeft(int chunkSize) {
        return new Vector2(getLLX(chunkSize), getLLZ(chunkSize));
    }

    @NotNull
    public Vector2 getLowerRight(int chunkSize) {
        return new Vector2(getLLX(chunkSize) + (chunkSize - 1), getLLZ(chunkSize));
    }

    @NotNull
    public Vector2 getUpperLeft(int chunkSize) {
        return new Vector2(getLLX(chunkSize), getLLZ(chunkSize) + (chunkSize - 1));
    }

    @NotNull
    public Vector2 getUpperRight(int chunkSize) {
        final int offset = chunkSize - 1;
        return new Vector2(getLLX(chunkSize) + offset, getLLZ(chunkSize) + offset);
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
