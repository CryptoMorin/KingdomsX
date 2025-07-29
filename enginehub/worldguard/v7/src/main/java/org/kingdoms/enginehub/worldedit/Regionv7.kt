package org.kingdoms.enginehub.worldedit

import com.sk89q.worldedit.regions.Region
import org.kingdoms.server.location.BlockVector2
import org.kingdoms.server.location.BlockVector3
import org.kingdoms.server.location.Vector3

private class Regionv7(private val region: Region) : XRegion {
    override val minimumPoint: BlockVector3
        get() = WorldEditBukkitAdapterv7.adapt(region.minimumPoint)

    override val maximumPoint: BlockVector3
        get() = WorldEditBukkitAdapterv7.adapt(region.maximumPoint)

    override val center: Vector3
        get() = WorldEditBukkitAdapterv7.adapt(region.center)

    override val chunks: Set<BlockVector2>
        get() = region.chunks.mapTo(HashSet(region.chunks.size)) { WorldEditBukkitAdapterv7.adapt(it) }
}