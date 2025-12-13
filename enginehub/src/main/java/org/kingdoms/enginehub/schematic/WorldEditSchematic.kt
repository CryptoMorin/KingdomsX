package org.kingdoms.enginehub.schematic

import com.cryptomorin.xseries.XMaterial
import com.cryptomorin.xseries.XTag
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.math.transform.AffineTransform
import org.bukkit.Material
import org.kingdoms.enginehub.schematic.blocks.ClipboardBlockIterator
import org.kingdoms.enginehub.schematic.blocks.ClipboardTransformBaker
import org.kingdoms.enginehub.schematic.blocks.WorldEditExtentBlock
import org.kingdoms.enginehub.worldedit.XClipboardFormat
import org.kingdoms.server.location.BlockVector3
import org.kingdoms.server.location.Direction
import org.kingdoms.utils.internal.nonnull.NonNullMap
import java.nio.file.Path

typealias CalculatedBlocks = MutableMap<BlockVector3, WorldEditExtentBlock>

class WorldEditSchematic(
    val name: String,
    val storedFile: Path,
    internal val clipboard: Clipboard,
    val clipboardFormat: XClipboardFormat
) {
    override fun toString() = "${this.javaClass.simpleName}(name='$name', storedFile=$storedFile)"
    override fun hashCode() = this.name.hashCode()
    override fun equals(other: Any?): Boolean {
        return other is WorldEditSchematic && this.name == other.name
    }

    companion object {
        @JvmStatic
        fun isOriginPartOfSchematic(clipboard: Clipboard): Boolean {
            val blocks = HashSet<BlockVector3>()
            val kOrigin: BlockVector3 = clipboard.origin.run { BlockVector3.of(x, y, z) }

            val clipboardBlocks = ClipboardBlockIterator(
                kOrigin, clipboard, null
            )

            for (weBlock in clipboardBlocks) {
                val adapted: Material? = BukkitAdapter.adapt(weBlock.block.blockType)
                if (adapted !== null) {
                    val xMaterial = XMaterial.matchXMaterial(adapted)
                    if (XTag.AIR.isTagged(xMaterial)) continue
                }
                blocks.add(weBlock.relativePosition)
            }

            return blocks.contains(kOrigin)
        }
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
            if (ignoreAir) {
                // We used: weBlock.block.blockType.material.isAir
                // But BlockType#getMaterial() causes ConcurrentModificationException in newer versions for some reason.
                // This only happens since the MC 1.21.9 and 1.21.10 updates.
                // https://github.com/EngineHub/WorldEdit/blob/46e1923e7d83a022a2d33101dbcd95faef48de88/worldedit-bukkit/src/main/java/com/sk89q/worldedit/bukkit/BukkitBlockRegistry.java#L48-L62
                val adapted: Material? = BukkitAdapter.adapt(weBlock.block.blockType)
                if (adapted !== null) {
                    val xMaterial = XMaterial.matchXMaterial(adapted)
                    if (XTag.AIR.isTagged(xMaterial)) continue
                }
            }
            blocks[weBlock.absolutePosition] = weBlock
        }

        if (blocks.isEmpty()) throw IllegalArgumentException("No blocks to paste")
        return Pair(transformedClipboard, blocks)
    }
}