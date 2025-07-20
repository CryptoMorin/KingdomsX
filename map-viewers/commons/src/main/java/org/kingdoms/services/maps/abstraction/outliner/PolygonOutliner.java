package org.kingdoms.services.maps.abstraction.outliner;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kingdoms.server.location.Vector2;

import java.util.*;

/**
 * Inspired by MapTowny's map system.
 * https://github.com/TownyAdvanced/MapTowny/blob/44bedbc91084c1603a480d8222964bb36fc935c5/maptowny-api/src/main/java/me/silverwolfg11/maptowny/util/NegativeSpaceFinder.java
 */
public final class PolygonOutliner {
    private final ConnectedChunkCluster cluster;
    private final int chunkBlockSize;

    // Path algorithm inserts unnecessary duplicate points
    private final Set<Vector2> poly = new LinkedHashSet<>();
    private final Queue<WorldlessChunk> chunksToVisit = new ArrayDeque<>(1);
    private final Vector2 originPoint; // upper-left of the origin chunk

    // Data for each iteration
    private ChunkDirection direction = ChunkDirection.RIGHT;
    private WorldlessChunk chunk;
    private WorldlessChunk offsetChunk;
    private boolean isOriginPoint = true;

    public PolygonOutliner(ConnectedChunkCluster cluster, int chunkBlockSize) {
        this.cluster = cluster;
        this.chunkBlockSize = chunkBlockSize;

        WorldlessChunk rightMostBlock = Objects.requireNonNull(findUpperRightMost());
        this.originPoint = rightMostBlock.getUpperLeft(chunkBlockSize);
        chunksToVisit.add(rightMostBlock);
    }

    private void findPoints() {
        while (!chunksToVisit.isEmpty()) {
            chunk = chunksToVisit.poll();
            switch (direction) {
                case RIGHT:
                    visitRight();
                    break;
                case LEFT:
                    visitLeft();
                    break;
                case DOWN:
                    visitDown();
                    break;
                case UP:
                    visitUp();
                    break;
                default:
                    throw new AssertionError("Direction: " + direction);
            }
        }
    }

    /**
     * Checks if we hit a corner while searching.
     */
    private boolean hasChunk(ChunkDirection direction) {
        offsetChunk = chunk.offset(direction);
        return cluster.has(offsetChunk);
    }

    private void visitRight() {
        Vector2 upperLeft = chunk.getUpperLeft(chunkBlockSize);

        // We approach the origin from the right so verify we are not back at the origin.
        if (!isOriginPoint && upperLeft.equals(originPoint)) return;
        else if (isOriginPoint) isOriginPoint = false;

        // Might only happen if we are going towards the origin, not away from it.
        if (hasChunk(ChunkDirection.UP)) {
            chunksToVisit.add(offsetChunk);
            direction = ChunkDirection.UP;
            poly.add(upperLeft);
        } else if (hasChunk(ChunkDirection.RIGHT)) {
            chunksToVisit.add(offsetChunk); // Keep going right
        } else {
            // We're the right-most, so go down.
            direction = ChunkDirection.DOWN;
            chunksToVisit.add(chunk);
            poly.add(chunk.getUpperRight(chunkBlockSize));
        }
    }

    private void visitUp() {
        if (hasChunk(ChunkDirection.LEFT)) {
            chunksToVisit.add(offsetChunk);
            direction = ChunkDirection.LEFT;
            poly.add(chunk.getLowerLeft(chunkBlockSize));
        } else if (hasChunk(ChunkDirection.UP)) {
            chunksToVisit.add(offsetChunk); // Keep going up
        } else {
            // We're the top-most, so make a right turn.
            direction = ChunkDirection.RIGHT;
            chunksToVisit.add(chunk);
            poly.add(chunk.getUpperLeft(chunkBlockSize));
        }
    }

    private void visitDown() {
        if (hasChunk(ChunkDirection.RIGHT)) {
            chunksToVisit.add(offsetChunk);
            direction = ChunkDirection.RIGHT;
            poly.add(chunk.getUpperRight(chunkBlockSize));
        } else if (hasChunk(ChunkDirection.DOWN)) {
            chunksToVisit.add(offsetChunk); // Keep going down
        } else {
            // We're the bottom-most, so make a left turn.
            direction = ChunkDirection.LEFT;
            chunksToVisit.add(chunk);
            poly.add(chunk.getLowerRight(chunkBlockSize));
        }
    }

    private void visitLeft() {
        if (hasChunk(ChunkDirection.DOWN)) {
            chunksToVisit.add(offsetChunk);
            direction = ChunkDirection.DOWN;
            poly.add(chunk.getLowerRight(chunkBlockSize));
        } else if (hasChunk(ChunkDirection.LEFT)) {
            chunksToVisit.add(offsetChunk); // Keep going left
        } else {
            // We're at the left-most, so go up.
            direction = ChunkDirection.UP;
            chunksToVisit.add(chunk);
            poly.add(chunk.getLowerLeft(chunkBlockSize));
        }
    }

    /**
     * Finds the upper right-most chunk, prioritizing right-most over upper location.
     */
    @Nullable
    private WorldlessChunk findUpperRightMost() {
        WorldlessChunk rightMostBlock = cluster.findAny();

        for (WorldlessChunk chunk : cluster.getChunks()) {
            if (chunk.x > rightMostBlock.x
                    || (chunk.x == rightMostBlock.x && chunk.z > rightMostBlock.z)) {
                rightMostBlock = chunk;
            }
        }

        return rightMostBlock;
    }


    /**
     * Returns a list of points that outline the polygon shape of a chunk cluster.
     */
    @NotNull
    public static List<Vector2> findPointsOf(ConnectedChunkCluster cluster, int chunkBlockSize) {
        PolygonOutliner finder = new PolygonOutliner(cluster, chunkBlockSize);
        finder.findPoints();
        return new ArrayList<>(finder.poly); // Points must be ordered, we already use a LinkedHashSet
    }
}
