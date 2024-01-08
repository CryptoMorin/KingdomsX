package org.kingdoms.server.location

class AbstractImmutableLocation(
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