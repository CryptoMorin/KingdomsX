package org.kingdoms.server.location

open class AbstractImmutableLocation(
    private val world: World,
    private val x: Double,
    private val y: Double,
    private val z: Double,
    private val yaw: Float,
    private val pitch: Float,
) : ImmutableLocation {
    override fun getWorld(): World = world
    override fun getX(): Double = x
    override fun getY(): Double = y
    override fun getZ(): Double = z
    override fun getYaw(): Float = yaw
    override fun getPitch(): Float = pitch
}

open class AbstractImmutablePoint3D(
    private val x: Double, private val y: Double, private val z: Double
) : ImmutablePoint3D {
    override fun getX() = x
    override fun getY() = y
    override fun getZ() = z

    override fun equals(other: Any?): Boolean {
        if (other !is ImmutablePoint3D) return false
        return this.x == other.getX() && this.y == other.getY() && this.z == other.getX()
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 14

        result = (prime * result + x).toInt()
        result = (prime * result + y).toInt()
        result = (prime * result + z).toInt()

        return result
    }
}