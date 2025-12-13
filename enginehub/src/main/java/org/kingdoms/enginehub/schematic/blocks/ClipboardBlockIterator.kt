package org.kingdoms.enginehub.schematic.blocks

import com.sk89q.worldedit.extent.clipboard.Clipboard
import org.kingdoms.enginehub.WorldEditAdapter.adapt
import org.kingdoms.enginehub.WorldEditAdapter.getAbsolutePosition
import org.kingdoms.enginehub.worldedit.XWorldEditBukkitAdapterFactory
import org.kingdoms.server.location.BlockVector3

class ClipboardBlockIterator(
    private val origin: BlockVector3,
    private val clipboard: Clipboard,
    sortingStrategy: Comparator<BlockVector3>?
) : Iterator<WorldEditExtentBlock> {
    private val minimumPoint: BlockVector3 = XWorldEditBukkitAdapterFactory.INSTANCE.adapt(clipboard).minimumPoint
    private val maximumPoint: BlockVector3 = XWorldEditBukkitAdapterFactory.INSTANCE.adapt(clipboard).maximumPoint

    private val minX = minimumPoint.x
    private val maxX = maximumPoint.x
    private val minY = minimumPoint.y
    private val maxY = maximumPoint.y
    private val minZ = minimumPoint.z
    private val maxZ = maximumPoint.z

    // true because we don't want to proceed the coords on the first iteration.
    private var cachedHasNext: Boolean? = true
    private val locationIterator: Iterator<BlockVector3>

    init {
        locationIterator = if (sortingStrategy === null) populateCoordinates().iterator()
        else populateCoordinates().sortedWith(sortingStrategy).iterator()
    }

    fun populateCoordinates(): List<BlockVector3> {
        val blocks: MutableList<BlockVector3> = mutableListOf()
        for (x in minX..maxX) {
            for (y in minY..maxY) {
                for (z in minZ..maxZ) {
                    blocks.add(BlockVector3.of(x, y, z))
                }
            }
        }
        return blocks
    }

    override fun hasNext(): Boolean {
        cachedHasNext?.let { return it }
        locationIterator.hasNext().let {
            cachedHasNext = it
            return it
        }
    }

    override fun next(): WorldEditExtentBlock {
        if (!hasNext()) throw NoSuchElementException("No more blocks to load: $clipboard")

        val relativePos: BlockVector3 = locationIterator.next()
        val absolutePos: BlockVector3 = relativePos.getAbsolutePosition(origin, clipboard)

        cachedHasNext = null
        return WorldEditExtentBlock(clipboard, relativePos, absolutePos)
    }
}