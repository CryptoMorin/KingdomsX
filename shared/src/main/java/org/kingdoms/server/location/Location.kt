package org.kingdoms.server.location

import kotlin.math.cos
import kotlin.math.sin

class Location(
    override val world: World,
    override val x: Double,
    override val y: Double,
    override val z: Double,
    override val yaw: Float,
    override val pitch: Float
) : Directional, WorldContainer, Point3D {
    override fun hashCode(): Int = throw UnsupportedOperationException()
    override fun equals(other: Any?): Boolean {
        if (other !is Location) return false
        return world == other.world &&
                x == other.x && y == other.y && z == other.z &&
                yaw == other.yaw && pitch == other.pitch
    }

    override fun toString(): String {
        return "Location(world=$world, x=$x, y=$y, z=$z, yaw=$yaw, pitch=$pitch)"
    }

    fun add(x: Number, y: Number, z: Number): Location = simpleAdd(x, y, z)
    fun add(other: BlockPoint3D): Location = add(other.x, other.y, other.z)
    fun add(other: Point3D): Location = add(other.x, other.y, other.z)
    fun subtract(other: BlockPoint3D) = subtract(other.x, other.y, other.z)
    fun subtract(x: Number, y: Number, z: Number) = simpleAdd(-x.toDouble(), -y.toDouble(), -z.toDouble())

    private fun simpleAdd(x: Number, y: Number, z: Number) =
        Location.of(world, this.x + x.toDouble(), this.y + y.toDouble(), this.z + z.toDouble(), yaw, pitch)

    companion object {
        @Suppress("NOTHING_TO_INLINE")
        @JvmStatic
        inline fun modify(): Nothing {
            throw UnsupportedOperationException("Cannot modify immutable location")
        }

        @JvmStatic
        fun of(
            world: World,
            x: Double, y: Double, z: Double,
            yaw: Float, pitch: Float,
        ): Location = Location(world, x, y, z, yaw, pitch)
    }
}

interface Directional {
    val yaw: Float
    val pitch: Float

    fun getDirection(): Vector3 {
        val rotX = yaw.toDouble()
        val rotY = pitch.toDouble()
        val xz = cos(Math.toRadians(rotY))

        return Vector3.of(
            -xz * sin(Math.toRadians(rotX)),
            -sin(Math.toRadians(rotY)),
            xz * cos(Math.toRadians(rotX))
        )
    }

    companion object {
        @JvmStatic fun of(yaw: Number, pitch: Number): Directional =
            AbstractDirectional(pitch = pitch.toFloat(), yaw = yaw.toFloat())
    }
}

private class AbstractDirectional(override val pitch: Float, override val yaw: Float) : Directional

interface WorldContainer {
    val world: World
}