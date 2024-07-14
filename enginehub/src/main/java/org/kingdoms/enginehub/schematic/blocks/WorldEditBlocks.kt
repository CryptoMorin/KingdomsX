package org.kingdoms.enginehub.schematic.blocks

import com.sk89q.worldedit.extent.Extent
import com.sk89q.worldedit.function.RegionFunction
import com.sk89q.worldedit.world.block.BaseBlock
import org.kingdoms.enginehub.WorldEditAdapter.adapt
import org.kingdoms.server.location.BlockVector3

open class WorldEditExtentBlock(
    val extent: Extent,
    val relativePosition: BlockVector3,
    val absolutePosition: BlockVector3
) {
    val block: BaseBlock get() = extent.getFullBlock(relativePosition.adapt)
}

open class FunctionalWorldEditExtentBlock(
    extent: Extent,
    relativePosition: BlockVector3,
    absolutePosition: BlockVector3,
    private val function: RegionFunction
) :
    WorldEditExtentBlock(extent, relativePosition, absolutePosition) {
    fun applyFunction(): Boolean {
        return this.function.apply(this.relativePosition.adapt)
    }
}