package org.kingdoms.enginehub.schematic.blocks

import com.sk89q.worldedit.extent.clipboard.Clipboard
import org.kingdoms.enginehub.WorldEditAdapter.adapt
import org.kingdoms.enginehub.WorldEditAdapter.getAbsolutePosition
import org.kingdoms.server.location.BlockVector3

class ClipboardBlockIterator(
    private val origin: BlockVector3,
    private val clipboard: Clipboard,
    sortingStrategy: Comparator<BlockVector3>
) : Iterator<WorldEditExtentBlock> {
    private val minimumPoint: com.sk89q.worldedit.math.BlockVector3 = clipboard.minimumPoint
    private val maximumPoint: com.sk89q.worldedit.math.BlockVector3 = clipboard.maximumPoint
    private val minX = minimumPoint.blockX
    private val maxX = maximumPoint.blockX
    private val minY = minimumPoint.blockY
    private val maxY = maximumPoint.blockY
    private val minZ = minimumPoint.blockZ
    private val maxZ = maximumPoint.blockZ

    // true because we don't want to proceed the coords on the first iteration.
    private var cachedHasNext: Boolean? = true
    private val locationIterator: Iterator<BlockVector3> = populateCoordinates().sortedWith(sortingStrategy).iterator()

    private fun populateCoordinates(): List<BlockVector3> {
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

        val next = locationIterator.next()
        val relativePos = com.sk89q.worldedit.math.BlockVector3.at(next.x, next.y, next.z)
        val absolutePos: BlockVector3 = relativePos.getAbsolutePosition(origin, clipboard)

        cachedHasNext = null
        return WorldEditExtentBlock(clipboard, relativePos.adapt, absolutePos)
    }
}