package org.kingdoms.services.worldedit

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.function.operation.Operation
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.session.ClipboardHolder
import org.bukkit.Bukkit
import org.kingdoms.server.location.ImmutableLocation
import org.kingdoms.services.Service
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream


/**
 * https://worldedit.enginehub.org/en/latest/api/
 * https://worldedit.enginehub.org/en/latest/api/examples/clipboard/#schematic-examples
 */
class ServiceWorldEdit : Service {
    companion object {
        private val openOptions = arrayOf(
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
        )
    }

    fun saveSchematic(schematic: WorldGuardSchematic, path: Path): WorldGuardSchematic {
        BuiltInClipboardFormat.MCEDIT_SCHEMATIC.getWriter(path.outputStream(*openOptions)).use { writer ->
            writer.write(schematic.clipboard)
            return WorldGuardSchematic(path, schematic.clipboard)
        }
    }

    fun loadSchematic(path: Path): WorldGuardSchematic {
        val format = ClipboardFormats.findByFile(path.toFile())
        format!!.getReader(path.inputStream(StandardOpenOption.READ)).use { reader ->
            val cb = reader.read()
            return WorldGuardSchematic(path, cb)
        }
    }

    fun pasteSchematic(schematic: WorldGuardSchematic, location: ImmutableLocation): Operation {
        val world = BukkitAdapter.adapt(Bukkit.getWorld(location.getWorld().getId()))
        WorldEdit.getInstance().newEditSession(world).use { editSession ->
            return ClipboardHolder(schematic.clipboard)
                .createPaste(editSession)
                .copyBiomes(false)
                .copyEntities(false)
                .ignoreAirBlocks(true)
                .ignoreStructureVoidBlocks(false)
                .to(BlockVector3.at(location.getX(), location.getY(), location.getZ()))
                .build()
        }
    }
}