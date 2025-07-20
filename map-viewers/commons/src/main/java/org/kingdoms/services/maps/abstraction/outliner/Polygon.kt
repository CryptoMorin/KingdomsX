package org.kingdoms.services.maps.abstraction.outliner

import org.kingdoms.server.location.Vector2

/**
 * Represents instructions on how a polygon looks like with the given 2D points
 * and another set of points to show the holes inside this polygon.
 */
class Polygon(
    /**
     * List of connected points of the polygon shape.
     */
    val points: List<Vector2>,

    /**
     * [Negative Space](https://en.wikipedia.org/wiki/Negative_space)
     * A list of polygon shapes that represent the empty holes of a polygon.
     */
    val negativeSpace: List<List<Vector2>>
)
