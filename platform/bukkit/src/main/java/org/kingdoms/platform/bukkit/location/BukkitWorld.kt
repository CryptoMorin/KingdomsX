package org.kingdoms.platform.bukkit.location

import org.bukkit.Bukkit
import org.kingdoms.server.location.World
import java.util.*
import java.util.stream.Collectors

class BukkitWorld(val world: org.bukkit.World) : World {
    override fun getName() = world.name
    override fun getId() = world.uid
    override fun getMaxHeight(): Int = world.maxHeight
    override fun getMinHeight(): Int = if (SUPPORTS_MIN_HEIGHT) world.minHeight else 0

    override fun equals(other: Any?): Boolean {
        if (other !is World) return false
        return this.getId() == other.getId()
    }

    override fun hashCode(): Int = getId().hashCode()
    override fun toString(): String = "World(${getName()})"

    companion object {
        private val SUPPORTS_MIN_HEIGHT: Boolean

        init {
            var supportsMin: Boolean
            try {
                Class.forName("org.bukkit.World").getMethod("getMinHeight")
                supportsMin = true
            } catch (e: Throwable) {
                supportsMin = false
            }
            SUPPORTS_MIN_HEIGHT = supportsMin
        }

        @JvmStatic
        fun getWorld(world: String, id: Any): org.bukkit.World {
            val bukkitWorld = Bukkit.getWorld(world) ?: cantFindWorld(world, id)
            return bukkitWorld
        }

        @JvmStatic
        fun from(world: World): org.bukkit.World {
            if (world is BukkitWorld) return world.world
            return getWorld(world.getId(), world)
        }

        @JvmStatic
        fun getWorld(worldId: UUID, id: Any): org.bukkit.World {
            val bukkitWorld = Bukkit.getWorld(worldId) ?: cantFindWorld(worldId, id)
            return bukkitWorld
        }

        @Suppress("NOTHING_TO_INLINE")
        private inline fun cantFindWorld(world: Any, id: Any): Nothing {
            val worlds = Bukkit.getWorlds().stream()
                .map { x: org.bukkit.World -> x.name + ':' + x.uid }
                .collect(Collectors.joining(", "))

            throw IllegalStateException("Cannot find world '$world' from id ($id) available worlds: [$worlds]")
        }
    }
}