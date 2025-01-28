package org.kingdoms.platform.bukkit.location

import org.bukkit.Bukkit
import org.kingdoms.server.location.World
import org.kingdoms.server.location.WorldRegistry
import java.util.*

class BukkitWorldRegistry : WorldRegistry {
    override fun getWorld(name: String) =
        Bukkit.getWorld(WorldRegistry.remap(name))?.let { BukkitWorld(it) }

    override fun getWorld(id: UUID) = Bukkit.getWorld(id)?.let { BukkitWorld(it) }

    override val worlds: List<World> = Bukkit.getWorlds().map { BukkitWorld(it) }
}