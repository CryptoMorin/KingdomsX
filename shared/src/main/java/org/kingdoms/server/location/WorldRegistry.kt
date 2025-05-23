package org.kingdoms.server.location

import java.util.*

interface WorldRegistry {
    fun getWorld(name: String): World?
    fun getWorld(id: UUID): World?
    val worlds: List<World>

    companion object {
        @JvmField val WORLD_REMAPPER: Map<String, String> = hashMapOf(
            "Midgard" to "world",
        )

        @Suppress("NOTHING_TO_INLINE")
        @JvmStatic inline fun remap(world: String): String {
            // return WORLD_REMAPPER.getOrDefault(world, world)
            return world
        }
    }
}