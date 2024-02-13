package org.kingdoms.server.location

interface ImmutableLocation : Directional, ImmutablePoint3D {
    fun getWorld(): World

    companion object {
        @Suppress("NOTHING_TO_INLINE")
        @JvmStatic
        inline fun modify(): Nothing {
            throw UnsupportedOperationException("Cannot modify immutable location")
        }

        @JvmStatic
        fun of(
            world: World,
            x: Double,
            y: Double,
            z: Double,
            yaw: Float,
            pitch: Float,
        ): ImmutableLocation = AbstractImmutableLocation(world, x, y, z, yaw, pitch)
    }
}

interface Directional {
    fun getYaw(): Float
    fun getPitch(): Float
}

interface ImmutablePoint3D {
    fun getX(): Double
    fun getY(): Double
    fun getZ(): Double

    fun getBlockX(): Int = LocationUtils.toBlock(getX())
    fun getBlockY(): Int = LocationUtils.toBlock(getY())
    fun getBlockZ(): Int = LocationUtils.toBlock(getZ())

    companion object {
        @JvmStatic
        fun of(x: Number, y: Number, z: Number): ImmutablePoint3D =
            AbstractImmutablePoint3D(x.toDouble(), y.toDouble(), z.toDouble())
    }
}

class ImmutablePoint2D(val x: Double, val z: Double) {
    override fun hashCode(): Int {
        var bits = java.lang.Double.doubleToLongBits(x)
        bits = bits xor java.lang.Double.doubleToLongBits(z) * 31
        return ((bits.toInt()) xor ((bits shr 32).toInt()))
    }

    override fun equals(obj: Any?): Boolean {
        val other = obj as? ImmutablePoint2D ?: return false
        return this.x == other.x && this.z == other.z
    }
}
