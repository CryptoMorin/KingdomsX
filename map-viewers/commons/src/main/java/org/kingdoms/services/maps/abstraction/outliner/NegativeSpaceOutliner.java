package org.kingdoms.services.maps.abstraction.outliner;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Finds all holes (negative spaces) in a given chunk cluster.
 * "Holes" in this case refers to one or more unclaimed lands
 * surrounded by claimed lands.
 * This is currently bugged for BlueMap where it connects random points together.
 * See the PNG in this addon's root folder for more info.
 * <p>
 * Inspired by MapTowny's map system.
 * https://github.com/TownyAdvanced/MapTowny/blob/44bedbc91084c1603a480d8222964bb36fc935c5/maptowny-api/src/main/java/me/silverwolfg11/maptowny/util/NegativeSpaceFinder.java
 * <p>
 * NeincraftPlugin
 * https://github.com/Mark-225/NeincraftPlugin/blob/25fb4e30836620a90fe7df74f8a9d2be680861b2/src/main/java/de/neincraft/neincraftplugin/modules/plots/util/PlotUtils.java
 * <p>
 * and of course the cheese!
 * https://github.com/TechnicJelle/BMUtils/blob/main/src/main/java/com/technicjelle/BMUtils/Cheese.java
 */
public final class NegativeSpaceOutliner {
    private final ConnectedChunkCluster cluster;
    private final Set<WorldlessChunk> freeSpaces = new HashSet<>();

    public NegativeSpaceOutliner(ConnectedChunkCluster cluster) {this.cluster = cluster;}

    @NotNull
    public static Collection<WorldlessChunk> findNegativeSpace(ConnectedChunkCluster cluster) {
        // Not possible to form a hole with only 8 square chunks
        if (cluster.size() < 8) return Collections.emptyList();

        NegativeSpaceOutliner negativeSpaceFinder = new NegativeSpaceOutliner(cluster);
        Deque<WorldlessChunk> potentialNSpace = negativeSpaceFinder.sortClusterIntoSpaces();
        return negativeSpaceFinder.propagateFreeSpaces(potentialNSpace);
    }

    /**
     * Sort the chunk cluster into free space and potential negative space.
     * It is potential negative space because some negative spaces may be connected to free spaces.
     */
    private Deque<WorldlessChunk> sortClusterIntoSpaces() {
        Deque<WorldlessChunk> potentialNSpace = new ArrayDeque<>();

        // Find the edge corners of the cluster (corners don't need to be in cluster)
        ChunkClusterExtent edges = cluster.getChunks().stream().collect(ChunkClusterExtent.collect());
        final int minX = edges.minX, maxX = edges.maxX;
        final int minZ = edges.minZ, maxZ = edges.maxZ;

        int firstLastEncounteredChunkX = minX; // X pos of the first encountered chunk of the LAST ROW
        int lastLastEncounteredChunkX = maxX; // X pos of the last encountered chunk of the LAST ROW

        // Start from upper-left corner and go row by row
        // This will mark our free spaces and possible negative spaces
        for (int posZ = maxZ; posZ >= minZ; posZ--) {
            boolean encounteredChunk = false;
            int encounteredChunkX = minX;

            // X-position of the start of potential negative space for a certain row
            int startNegativeSpace = minX;
            // X-position of the end of potential negative space for a certain row
            int endNegativeSpace = minX;

            for (int posX = minX; posX <= maxX; posX++) {
                WorldlessChunk hash = new WorldlessChunk(posX, posZ);
                boolean chunkInCluster = cluster.has(hash);

                // These are just unclaimed chunks on the outline of the polygon
                if (!encounteredChunk && !chunkInCluster)
                    continue;

                if (chunkInCluster) {
                    // Mark the chunk as the first in the row
                    if (!encounteredChunk) {
                        encounteredChunk = true;
                        encounteredChunkX = posX;
                    }

                    if (startNegativeSpace != endNegativeSpace) {
                        for (int negSpaceX = startNegativeSpace + 1; negSpaceX <= endNegativeSpace; negSpaceX++) {
                            WorldlessChunk chunk = new WorldlessChunk(negSpaceX, posZ);

                            // Check if the potential space matches one of the following conditions to be a free space:
                            //  - On the Z-border
                            //  - Above an unclaimed wild-space
                            //  - Diagonally touching an unclaimed space on the Z-border
                            //  - Surrounded by a free space.
                            if (posZ == maxZ || posZ == minZ
                                    || negSpaceX < firstLastEncounteredChunkX || negSpaceX > lastLastEncounteredChunkX
                                    || touchingDiagonalBorders(negSpaceX, posZ, maxZ, minZ)
                                    || hasFreeSpace(chunk))
                                freeSpaces.add(chunk);
                            else {
                                // Mark it as a possible negative space
                                potentialNSpace.add(chunk);
                            }
                        }
                    }

                    startNegativeSpace = posX;
                    endNegativeSpace = posX;
                } else {
                    // The chunk is not in the cluster, so it may be potential negative space.
                    endNegativeSpace++;
                }
            }
            firstLastEncounteredChunkX = encounteredChunkX;
            lastLastEncounteredChunkX = Math.max(firstLastEncounteredChunkX, startNegativeSpace);
        }

        return potentialNSpace;
    }

    /**
     * Applies propagation from free spaces to potential negative spaces.
     *
     * @param possibleNegativeSpaces Potential negative spaces
     * @return list of actual negative space.
     */
    @Contract(mutates = "param1")
    private Collection<WorldlessChunk> propagateFreeSpaces(Deque<WorldlessChunk> possibleNegativeSpaces) {
        if (possibleNegativeSpaces.isEmpty())
            return Collections.emptyList();

        // Potential Negative Spaces are ordered from min X min Z to max X max Z.
        Set<WorldlessChunk> negativeSpace = new HashSet<>(possibleNegativeSpaces.size());
        Deque<WorldlessChunk> newFreeSpaces = new ArrayDeque<>();

        // First-pass, check if any potential negative space is adjacent to a free space.
        // If it is, convert it to a free space, and add it to the list to be propagated
        // in the second-pass.
        while (!possibleNegativeSpaces.isEmpty()) {
            // Pop the negative space with min X min Z (going from -z -> z, -x -> x)
            WorldlessChunk chunk = possibleNegativeSpaces.pollLast();

            // Check if the potential negative space is adjacent to a free space
            // or is above an empty space (hits the min Z border)
            if (hasFreeSpace(chunk) || isEmptyBelow(negativeSpace, chunk)) {
                freeSpaces.add(chunk);
                newFreeSpaces.push(chunk);
            } else {
                negativeSpace.add(chunk);
            }
        }

        // Perform second-pass only if there are new free spaces.
        if (!newFreeSpaces.isEmpty()) {
            // Checks if a location is a negative space and convert it to a free space.

            // Second-pass
            // Propagate all the converted new free spaces to adjacent potential negative spaces.
            while (!newFreeSpaces.isEmpty()) {
                WorldlessChunk free = newFreeSpaces.pop();

                // Do a surrounding check on the specificed chunk
                for (int xOffset = -1; xOffset <= 1; ++xOffset) {
                    for (int zOffset = -1; zOffset <= 1; ++zOffset) {
                        if (xOffset == 0 && zOffset == 0) continue;

                        WorldlessChunk up = free.offset(xOffset, zOffset);
                        if (negativeSpace.contains(up)) {
                            negativeSpace.remove(up);
                            newFreeSpaces.push(up);
                        }
                    }
                }
            }
        }

        return negativeSpace;
    }

    /**
     * Checks if the specific chunk is surrounded by any free space.
     * +++
     * +o+
     * +++
     */
    private boolean hasFreeSpace(WorldlessChunk chunk) {
        if (freeSpaces.isEmpty()) return false;

        for (int xOffset = -1; xOffset <= 1; ++xOffset) {
            for (int zOffset = -1; zOffset <= 1; ++zOffset) {
                if (xOffset == 0 && zOffset == 0) continue;

                WorldlessChunk offSetHash = chunk.offset(xOffset, zOffset);
                if (freeSpaces.contains(offSetHash)) return true;
            }
        }

        return false;
    }

    /**
     * Check if the position below the specified position is an empty space.
     * It is an empty space if it:
     * - Is beyond the bottom border of the chunk cluster.
     * - Is not a potential negative space.
     */
    private boolean isEmptyBelow(Collection<WorldlessChunk> negSpace, WorldlessChunk chunk) {
        WorldlessChunk belowHash = chunk.offset(0, -1);
        return !negSpace.contains(belowHash) && !cluster.has(belowHash);
    }

    /**
     * Check if a chunk is diagonal to an unclaimed chunk on the Z borders.
     * Any chunks on the Z-borders are guaranteed to not be negative space
     * Thus if any chunk passes this check, it should be considered a free-space / empty-space.
     */
    private boolean touchingDiagonalBorders(int posX, int posZ, int maxZ, int minZ) {
        int newZ;

        if ((posZ + 1) == maxZ) {
            // Check if Z is one below the upper Z-border
            newZ = posZ + 1;
        } else if ((posZ - 1) == minZ) {
            // Check if Z is one above the bottom Z-border
            newZ = posZ - 1;
        } else {
            return false;
        }

        WorldlessChunk leftDiag = new WorldlessChunk(posX - 1, newZ);
        WorldlessChunk rightDiag = new WorldlessChunk(posX + 1, newZ);

        return !cluster.has(leftDiag) || !cluster.has(rightDiag);
    }
}
