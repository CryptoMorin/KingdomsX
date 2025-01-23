package org.kingdoms.server.location

import java.util.*

interface WorldRegistry {
    fun getWorld(name: String): World?
    fun getWorld(id: UUID): World?

    companion object {
        @JvmField val WORLD_REMAPPER: Map<String, String> = hashMapOf(
            "new" to "world",
            "next" to "world",
            "Builders" to "world"
        )

        @Suppress("NOTHING_TO_INLINE")
        @JvmStatic inline fun remap(world: String): String {
            return world
        }
    }
}