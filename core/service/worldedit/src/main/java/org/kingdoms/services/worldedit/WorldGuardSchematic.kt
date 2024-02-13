package org.kingdoms.services.worldedit

import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.math.BlockVector3
import org.kingdoms.server.location.ImmutablePoint3D
import java.nio.file.Path

class WorldGuardSchematic(val storedFile: Path, internal val clipboard: Clipboard) {
    val mappedBlockLocations: Set<ImmutablePoint3D> = mapBlockLocations()

    private fun mapBlockLocations(): Set<ImmutablePoint3D> {
        val min = clipboard.minimumPoint
        val max = clipboard.maximumPoint
        val locations: MutableSet<ImmutablePoint3D> = hashSetOf()

        for (x in min.x..max.x) {
            for (y in min.y..max.y) {
                for (z in min.z..max.z) {
                    val block = clipboard.getBlock(BlockVector3.at(x, y, z))
                    if (!block.blockType.material.isAir) {
                        locations.add(ImmutablePoint3D.of(x, y, z))
                    }
                }
            }
        }

        return locations
    }
}