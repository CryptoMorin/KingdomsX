package org.kingdoms.enginehub.worldedit

import com.sk89q.worldedit.EditSession
import com.sk89q.worldedit.LocalSession
import com.sk89q.worldedit.bukkit.BukkitPlayer
import com.sk89q.worldedit.bukkit.BukkitWorld
import com.sk89q.worldedit.extent.Extent
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.transform.BlockTransformExtent
import com.sk89q.worldedit.function.operation.ForwardExtentCopy
import com.sk89q.worldedit.math.transform.Transform
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.world.World
import org.bukkit.Location
import org.bukkit.entity.Player
import org.kingdoms.server.location.BlockVector3
import org.kingdoms.server.location.Vector3

interface XWorldEditBukkitAdapter {
    fun adapt(player: Player): BukkitPlayer

    fun asBukkitWorld(world: World): BukkitWorld

    fun adapt(world: org.bukkit.World): World

    fun adapt(world: org.bukkit.World, vector: Any): Location

    fun blockData(block: Any): Any

    fun createEditSession(world: org.bukkit.World): EditSession

    fun adaptBlockVector(blockVector: Any): BlockVector3

    fun adapt(clipboard: Clipboard): XClipboard

    fun adapt(region: Region): XRegion

    fun setClipboard(session: LocalSession, clipboard: Clipboard)

    fun createCuboidRegion(min: BlockVector3, max: BlockVector3): CuboidRegion

    fun applyTransformation(transform: Transform, input: Vector3): Vector3

    fun createBlockTransformExtent(extent: Extent, transform: Transform): BlockTransformExtent

    fun createForwardExtentCopy(
        source: Extent,
        region: Region,
        from: BlockVector3,
        destination: Extent,
        to: BlockVector3
    ): ForwardExtentCopy
}
