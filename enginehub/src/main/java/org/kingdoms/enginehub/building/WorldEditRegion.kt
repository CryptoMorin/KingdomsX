package org.kingdoms.enginehub.building

import com.sk89q.worldedit.extent.clipboard.Clipboard
import org.kingdoms.constants.land.building.Region
import org.kingdoms.enginehub.WorldEditAdapter.getAbsolutePosition
import org.kingdoms.enginehub.schematic.CalculatedBlocks
import org.kingdoms.server.location.BlockVector2
import org.kingdoms.server.location.BlockVector3
import org.kingdoms.server.location.Vector3

class WorldEditRegion(origin: BlockVector3, private val blocks: CalculatedBlocks, clipboard: Clipboard) : Region {
    private val weRegion = clipboard.region

    private val _center = weRegion.center.let { Vector3.of(it.x, it.y, it.z).add(origin) }
    private val _min = weRegion.minimumPoint.getAbsolutePosition(origin, clipboard)
    private val _max = weRegion.maximumPoint.getAbsolutePosition(origin, clipboard)
    private val _chunks = weRegion.chunks.map { BlockVector2.of(it.x, it.z) }.toSet()

    override fun getBlocks(): Set<BlockVector3> = blocks.keys
    override fun getCenter(): Vector3 = _center
    override fun getMinimumPoint(): BlockVector3 = _min
    override fun getMaximumPoint(): BlockVector3 = _max
    override fun getChunks(): Set<BlockVector2> = _chunks
}