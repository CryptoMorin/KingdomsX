package org.kingdoms.enginehub.worldedit

import org.kingdoms.server.location.BlockVector2
import org.kingdoms.server.location.BlockVector3
import org.kingdoms.server.location.Vector3

interface XRegion {
    /**
     * Get the lower point of a region.
     *
     * @return min. point
     */
    val minimumPoint: BlockVector3

    /**
     * Get the upper point of a region.
     *
     * @return max. point
     */
    val maximumPoint: BlockVector3

    /**
     * Get the center point of a region.
     * Note: Coordinates will not be integers
     * if the corresponding lengths are even.
     *
     * @return center point
     */
    val center: Vector3

    /**
     * Get a list of chunks.
     *
     * @return a list of chunk coordinates
     */
    val chunks: Set<BlockVector2>
}