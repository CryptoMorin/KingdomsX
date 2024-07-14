package org.kingdoms.server.location

import org.kingdoms.utils.internal.enumeration.Enums

/**
 * Based on the [Compass](https://en.wikipedia.org/wiki/Cardinal_direction) model.
 * @since 1.16.21
 */
enum class Direction : Directional {
    NORTH(0, 0, -1, Type.CARDINAL),
    EAST(1, 0, 0, Type.CARDINAL),
    SOUTH(0, 0, 1, Type.CARDINAL),
    WEST(-1, 0, 0, Type.CARDINAL),

    UP(0, 1, 0, Type.VERTICAL),
    DOWN(0, -1, 0, Type.VERTICAL),

    NORTH_EAST(NORTH, EAST, Type.ORDINAL),
    NORTH_WEST(NORTH, WEST, Type.ORDINAL),
    SOUTH_EAST(SOUTH, EAST, Type.ORDINAL),
    SOUTH_WEST(SOUTH, WEST, Type.ORDINAL),

    WEST_NORTH_WEST(WEST, NORTH_WEST, Type.SECONDARY_ORDINAL),
    NORTH_NORTH_WEST(NORTH, NORTH_WEST, Type.SECONDARY_ORDINAL),
    NORTH_NORTH_EAST(NORTH, NORTH_EAST, Type.SECONDARY_ORDINAL),
    EAST_NORTH_EAST(EAST, NORTH_EAST, Type.SECONDARY_ORDINAL),
    EAST_SOUTH_EAST(EAST, SOUTH_EAST, Type.SECONDARY_ORDINAL),
    SOUTH_SOUTH_EAST(SOUTH, SOUTH_EAST, Type.SECONDARY_ORDINAL),
    SOUTH_SOUTH_WEST(SOUTH, SOUTH_WEST, Type.SECONDARY_ORDINAL),
    WEST_SOUTH_WEST(WEST, SOUTH_WEST, Type.SECONDARY_ORDINAL);

    val type: Type

    val x: Int
    val y: Int
    val z: Int

    override val yaw: Float
    override val pitch: Float

    constructor(modX: Int, modY: Int, modZ: Int, type: Type) {
        this.x = modX
        this.y = modY
        this.z = modZ
        this.type = type

        val directional = LocationUtils.fromDirection(Vector3.of(modX, modY, modZ))

        // Normalized (cant use the static method because of the stupid Kotlin companion object)
        if (modX == 0 && modZ == 0) {
            this.yaw = 0f
            this.pitch = directional.pitch
        } else if (modY == 0) {
            this.yaw = (directional.yaw + 180f) % 360
            this.pitch = 0f
        } else {
            this.yaw = (directional.yaw + 180f) % 360
            this.pitch = directional.pitch
        }
    }

    constructor(face1: Direction, face2: Direction, type: Type) : this(
        face1.x + face2.x,
        face1.y + face2.y,
        face1.z + face2.z,
        type
    )

    val isCartesian
        get() = when (this) {
            NORTH, EAST, SOUTH, WEST, UP, DOWN -> true
            else -> false
        }

    val oppositeFace
        get() = when (this) {
            NORTH -> SOUTH
            SOUTH -> NORTH

            EAST -> WEST
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
        }

    enum class Type {
        CARDINAL,

        /**
         * aka. intercardinal directions
         */
        ORDINAL,
        SECONDARY_ORDINAL,
        VERTICAL;
    }

    companion object {
        @JvmField val VALUES: Array<Direction> = Direction.values()

        private val MAPPINGS = Enums.createMapping(VALUES)

        @JvmStatic fun fromString(name: String): Direction? = MAPPINGS[name]

        /**
         * Find the closest direction to the given direction vector.
         *
         * @param immutableVector the vector
         * @param allowedTypes the only flags that are permitted
         * @return the closest direction, or null if no direction can be returned
         */
        @JvmStatic fun findClosest(immutableVector: Vector3, allowedTypes: Collection<Type>): Direction? {
            var vector = immutableVector

            if (allowedTypes.contains(Type.VERTICAL)) vector = vector.withY(0.0)
            vector = vector.normalize()

            var closest: Direction? = null
            var closestDot = -2.0
            for (direction in Direction.values()) {
                if (allowedTypes.contains(direction.type)) {
                    continue
                }

                // Add dot()
                // val dot = direction.dot(vector)
                // if (dot >= closestDot) {
                //     closest = direction
                //     closestDot = dot
                // }
            }

            return closest
        }

        /**
         * Normalizes Minecraft's stupid direction system based on the image below:
         * https://ambientweather.com/media/wysiwyg/smartwave/porto/ambient/faq/images/compass-rose.jpg
         */
        @Suppress("NAME_SHADOWING")
        @JvmStatic
        fun normalizeMinecraftYaw(yaw: Float): Float {
            val yaw = (yaw + 180f) % 360
            return if (yaw < 0) yaw + 360 else yaw
        }

        @JvmStatic
        fun getPitchDirection(pitch: Float): Direction = when (pitch) {
            in -90f..0f -> DOWN
            // in -45f..45f -> SELF
            in 0f..90f -> UP
            else -> throw IllegalArgumentException("Unknown Minecraft pitch value: $pitch")
        }

        @JvmStatic
        fun cardinalDirectionFromYaw(yaw: Float): Direction {
            return when (val transformedYaw = normalizeMinecraftYaw(yaw)) {
                in 0f..45f -> NORTH
                in 45f..135f -> EAST
                in 135f..225f -> SOUTH
                in 225f..315f -> WEST
                in 315f..360f -> NORTH
                else -> throw AssertionError("Unexpected yaw for cardinal direction: $yaw -> $transformedYaw")
            }
        }

        @Suppress("NAME_SHADOWING")
        @JvmStatic
        fun fromYaw(yaw: Float): Direction {
            var yaw = yaw
            yaw %= 360f
            if (yaw < 0) yaw += 360f

            return when (yaw) {
                in 337.5f..360.0f -> SOUTH
                in 292.5f..337.5f -> NORTH_EAST
                in 247.5f..292.5f -> EAST
                in 202.5f..247.5f -> SOUTH_EAST
                in 157.5f..202.5f -> NORTH
                in 112.5f..157.5f -> NORTH_WEST
                in 67.5f..112.5f -> WEST
                in 22.5f..67.5f -> SOUTH_WEST
                in 0f..22.5f -> SOUTH
                else -> throw AssertionError("Unexpected yaw for direction: $yaw")
            }
        }
    }
}
