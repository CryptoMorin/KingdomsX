package org.kingdoms.platform.bukkit.adapters

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.kingdoms.nbt.NBTTagConverterRegistry
import org.kingdoms.nbt.tag.NBTTag
import org.kingdoms.nbt.tag.NBTTagType
import org.kingdoms.platform.bukkit.location.BukkitWorld
import org.kingdoms.server.location.BlockLocation3
import org.kingdoms.server.location.BlockVector3
import org.kingdoms.server.location.Direction
import org.kingdoms.server.location.Vector3
import org.kingdoms.utils.internal.functional.Fn
import java.awt.Color

object BukkitAdapter {
    @JvmStatic fun adapt(direction: Direction): BlockFace = BlockFace.valueOf(direction.name)
    @JvmStatic fun adapt(direction: BlockFace): Direction = Direction.valueOf(direction.name)
    @JvmStatic fun <T : NBTTag<*>> adapt(type: NBTTagType<T>, nbt: Any): T {
        val converter = NBTTagConverterRegistry.get(type.id())!!
        return Fn.cast(converter.fromNBT(Fn.cast(nbt)))
    }

    @JvmStatic fun adapt(nbt: NBTTag<*>): Any {
        val converter = NBTTagConverterRegistry.get(nbt.type().id())!!
        return converter.toNBT(nbt)!!
    }

    @JvmStatic fun adapt(color: java.awt.Color): org.bukkit.Color = org.bukkit.Color.fromARGB(color.alpha, color.red, color.green, color.blue)

    @JvmStatic fun adapt(world: World): org.kingdoms.server.location.World = BukkitWorld(world)
    @JvmStatic fun adapt(world: org.kingdoms.server.location.World): World = (world as BukkitWorld).world

    @JvmStatic fun adapt(world: World, location: Vector3): Location = Location(
        world,
        location.x, location.y, location.z
    )

    @JvmStatic fun adapt(location: Location?): org.kingdoms.server.location.Location? = location?.run {
        org.kingdoms.server.location.Location(adapt(world!!), x, y, z, yaw, pitch)
    }

    @JvmStatic fun adaptBlockLocation(location: Location?): BlockLocation3? = location?.run {
        BlockLocation3(adapt(world!!), blockX, blockY, blockZ)
    }

    @JvmStatic fun adapt(location: org.kingdoms.server.location.Location?): Location? = location?.run {
        Location(adapt(world), x, y, z, yaw, pitch)
    }

    @JvmStatic fun adaptLocation(location: BlockLocation3): Location = location.run {
        Location(adapt(world), x.toDouble(), y.toDouble(), z.toDouble())
    }

    @JvmStatic fun adapt(location: BlockLocation3): Location = location.run {
        Location(adapt(world), x.toDouble(), y.toDouble(), z.toDouble(), 0f, 0f)
    }

    @JvmStatic fun adaptVector(block: Block): BlockVector3 = block.location.run {
        BlockVector3.of(blockX, blockY, blockZ)
    }

    @JvmStatic fun adaptLocation(block: Block): BlockLocation3 = block.location.run {
        BlockLocation3.of(adapt(world!!), blockX, blockY, blockZ)
    }

    @JvmStatic fun adapt(world: World, location: BlockVector3): Location = Location(
        world,
        location.x.toDouble(),
        location.y.toDouble(),
        location.z.toDouble()
    )
}