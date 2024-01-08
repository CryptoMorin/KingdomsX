package org.kingdoms.platform.bukkit.location

import org.kingdoms.server.location.World

class BukkitWorld(val world: org.bukkit.World) : World {
    override fun getName() = world.name
    override fun getId() = world.uid
}