package org.kingdoms.server.location

/**
 * @since 1.16.21
 */
enum class Compass {
    SELF(0, 0, 0),
    NORTH(0, 0, -1),
    EAST(1, 0, 0),
    SOUTH(0, 0, 1),
    WEST(-1, 0, 0),
    UP(0, 1, 0),
    DOWN(0, -1, 0),
    NORTH_EAST(NORTH, EAST),
    NORTH_WEST(NORTH, WEST),
    SOUTH_EAST(SOUTH, EAST),
    SOUTH_WEST(SOUTH, WEST),
    WEST_NORTH_WEST(WEST, NORTH_WEST),
    NORTH_NORTH_WEST(NORTH, NORTH_WEST),
    NORTH_NORTH_EAST(NORTH, NORTH_EAST),
    EAST_NORTH_EAST(EAST, NORTH_EAST),
    EAST_SOUTH_EAST(EAST, SOUTH_EAST),
    SOUTH_SOUTH_EAST(SOUTH, SOUTH_EAST),
    SOUTH_SOUTH_WEST(SOUTH, SOUTH_WEST),
    WEST_SOUTH_WEST(WEST, SOUTH_WEST);

    val modX: Int
    val modY: Int
    val modZ: Int

    constructor(modX: Int, modY: Int, modZ: Int) {
        this.modX = modX
        this.modY = modY
        this.modZ = modZ
    }

    constructor(face1: Compass, face2: Compass) {
        modX = face1.modX + face2.modX
        modY = face1.modY + face2.modY
        modZ = face1.modZ + face2.modZ
    }

    fun isCardinalDirection() = when (this) {
        NORTH, EAST, SOUTH, WEST -> true
        else -> false
    }

    fun isCartesian() = when (this) {
        NORTH, EAST, SOUTH, WEST, UP, DOWN -> true
        else -> false
    }

    fun getOppositeFace() = when (this) {
        NORTH -> SOUTH
        EAST -> WEST
        SOUTH -> NORTH
        WEST -> EAST
        UP -> DOWN
        DOWN -> UP
        NORTH_EAST -> SOUTH_WEST
        NORTH_WEST -> SOUTH_EAST
        SOUTH_EAST -> NORTH_WEST
        SOUTH_WEST -> NORTH_EAST
        WEST_NORTH_WEST -> EAST_SOUTH_EAST
        NORTH_NORTH_WEST -> SOUTH_SOUTH_EAST
        NORTH_NORTH_EAST -> SOUTH_SOUTH_WEST
        EAST_NORTH_EAST -> WEST_SOUTH_WEST
        EAST_SOUTH_EAST -> WEST_NORTH_WEST
        SOUTH_SOUTH_EAST -> NORTH_NORTH_WEST
        SOUTH_SOUTH_WEST -> NORTH_NORTH_EAST
        WEST_SOUTH_WEST -> EAST_NORTH_EAST
        SELF -> SELF
    }
}
