package org.kingdoms.enginehub.schematic

import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.math.transform.AffineTransform
import org.kingdoms.enginehub.schematic.blocks.ClipboardBlockIterator
import org.kingdoms.enginehub.schematic.blocks.ClipboardTransformBaker
import org.kingdoms.enginehub.schematic.blocks.WorldEditExtentBlock
import org.kingdoms.server.location.BlockVector3
import org.kingdoms.server.location.Direction
import org.kingdoms.utils.internal.nonnull.NonNullMap
import java.nio.file.Path

typealias CalculatedBlocks = MutableMap<BlockVector3, WorldEditExtentBlock>

class WorldEditSchematic(
    val name: String,
    val storedFile: Path,
    internal val clipboard: Clipboard
) {
    override fun toString() = "WorldEditSchematic(name='$name', storedFile=$storedFile)"
    override fun hashCode() = this.name.hashCode()
    override fun equals(other: Any?): Boolean {
        if (other !is WorldEditSchematic) return false
        return this.name == other.name
    }

    fun populate(
        origin: BlockVector3,
        facing: Direction?,
        ignoreAir: Boolean,
        useMap: CalculatedBlocks,
        sortingStrategy: Comparator<BlockVector3>
    ): Pair<Clipboard, CalculatedBlocks> {
        val blocks: CalculatedBlocks = NonNullMap.of(useMap)

        // Rotate the clipboard
        val transformedClipboard = if (facing != null) {
            // https://github.com/EngineHub/WorldEdit/blob/f85e3c463881100f7feab8b165582211f429a8a4/worldedit-core/src/main/java/com/sk89q/worldedit/command/ClipboardCommands.java#L221-L241
            // What the fuck is the negative for...
            val yaw = -(facing.yaw - Direction.EAST.yaw)
            val transform = AffineTransform().rotateY(yaw.toDouble())
            ClipboardTransformBaker.bakeTransform(clipboard, transform)
            // clipboard.transform(transform) WorldEdit v7.3.0+
        } else {
            clipboard
        }

        val iterator = ClipboardBlockIterator(origin, transformedClipboard, sortingStrategy)
        for (weBlock in iterator) {
            if (ignoreAir && weBlock.block.blockType.material.isAir) continue
            blocks[weBlock.absolutePosition] = weBlock
        }

        if (blocks.isEmpty()) throw IllegalArgumentException("No blocks to paste")
        return Pair(transformedClipboard, blocks)
    }
}