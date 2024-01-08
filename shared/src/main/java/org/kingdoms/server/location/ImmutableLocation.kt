package org.kingdoms.server.location

interface ImmutableLocation {
    fun getWorld(): World
    fun getX(): Double
    fun getY(): Double
    fun getZ(): Double
    fun getYaw(): Float
    fun getPitch(): Float

    fun getBlockX(): Int = LocationUtils.toBlock(getX())
    fun getBlockY(): Int = LocationUtils.toBlock(getY())
    fun getBlockZ(): Int = LocationUtils.toBlock(getZ())

    companion object {
        @Suppress("NOTHING_TO_INLINE")
        @JvmStatic inline fun modify(): Nothing {
            throw UnsupportedOperationException("Cannot modify immutable location")
        }

        @JvmStatic fun of(
            world: World,
            x: Double,
            y: Double,
            z: Double,
            yaw: Float,
            pitch: Float,
        ): ImmutableLocation = AbstractImmutableLocation(world, x, y, z, yaw, pitch)
    }
}