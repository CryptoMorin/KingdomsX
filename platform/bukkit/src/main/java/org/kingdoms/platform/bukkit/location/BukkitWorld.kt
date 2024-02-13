package org.kingdoms.platform.bukkit.location

import org.bukkit.Bukkit
import org.kingdoms.server.location.World
import java.util.*
import java.util.stream.Collectors

class BukkitWorld(val world: org.bukkit.World) : World {
    override fun getName() = world.name
    override fun getId() = world.uid

    companion object {
        @JvmStatic fun getWorld(world: String, id: Any): org.bukkit.World {
            val bukkitWorld = Bukkit.getWorld(world) ?: cantFindWorld(world, id)
            return bukkitWorld
        }

        @JvmStatic fun getWorld(worldId: UUID, id: Any): org.bukkit.World {
            val bukkitWorld = Bukkit.getWorld(worldId) ?: cantFindWorld(worldId, id)
            return bukkitWorld
        }

        @Suppress("NOTHING_TO_INLINE")
        private inline fun cantFindWorld(world: Any, id: Any): Nothing {
            val worlds = Bukkit.getWorlds().stream()
                .map { x: org.bukkit.World -> x.name + ':' + x.uid }
                .collect(Collectors.joining(", "))

            throw IllegalStateException("Cannot find world with ID: $world ($id) available worlds: $worlds")
        }
    }
}