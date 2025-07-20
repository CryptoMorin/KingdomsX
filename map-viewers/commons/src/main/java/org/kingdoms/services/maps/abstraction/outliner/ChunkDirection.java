package org.kingdoms.services.maps.abstraction.outliner;

enum ChunkDirection {
    RIGHT(1, 0),
    LEFT(-1, 0),

    UP(0, 1),
    DOWN(0, -1);

    protected static final ChunkDirection[] DIRECTIONS = values();
    public final int x, z;

    ChunkDirection(int x, int z) {
        this.x = x;
        this.z = z;
    }
}
