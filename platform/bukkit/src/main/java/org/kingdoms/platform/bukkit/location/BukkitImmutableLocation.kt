package org.kingdoms.platform.bukkit.location

import org.bukkit.Bukkit
import org.bukkit.Location
import org.kingdoms.server.location.AbstractImmutableLocation
import org.kingdoms.server.location.ImmutableLocation

object BukkitImmutableLocation {
    @JvmStatic fun of(location: Location?): ImmutableLocation? {
        location ?: return null
        return AbstractImmutableLocation(
            BukkitWorld(location.world!!),
            location.x, location.y, location.z, location.yaw, location.pitch
        )
    }

    @JvmStatic fun from(location: ImmutableLocation?): Location? {
        location ?: return null
        return location.run {
            Location(BukkitWorld.getWorld(getWorld().getId(), location), getX(), getY(), getZ(), getYaw(), getPitch())
        }
    }
}