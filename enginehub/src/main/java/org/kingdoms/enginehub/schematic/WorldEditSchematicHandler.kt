package org.kingdoms.enginehub.schematic

import com.cryptomorin.xseries.reflection.XReflection
import com.sk89q.worldedit.EmptyClipboardException
import com.sk89q.worldedit.IncompleteRegionException
import com.sk89q.worldedit.LocalSession
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.session.ClipboardHolder
import org.bukkit.entity.Player
import org.kingdoms.enginehub.WorldEditAdapter.adapt
import org.kingdoms.enginehub.schematic.blocks.ClipboardTransformBaker
import org.kingdoms.server.location.BlockVector3
import org.kingdoms.utils.internal.enumeration.Enums
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.absolutePathString
import kotlin.io.path.inputStream
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.outputStream

/**
 * https://worldedit.enginehub.org/en/latest/api/
 * https://worldedit.enginehub.org/en/latest/api/examples/clipboard/#schematic-examples
 */
object WorldEditSchematicHandler {
    const val FILE_EXTENSION = ".schematic"

    private val openOptions = arrayOf(
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE
    )

    @JvmStatic
    fun getClipboardFormat(format: ClipboardFormat? = null): ClipboardFormat =
        format ?: if (ClipboardFormat::class.java.isEnum) {
            XReflection.of(ClipboardFormat::class.java).enums().named("SCHEMATIC").enumConstant as ClipboardFormat
        } else {
            Enums.findOneOf(
                BuiltInClipboardFormat::class.java,
                "SPONGE_V3_SCHEMATIC",
                "SPONGE_SCHEMATIC"
            )
        }

    @JvmStatic
    fun saveSchematic(schematic: WorldEditSchematic, format: ClipboardFormat? = null): WorldEditSchematic {
        val path = schematic.storedFile
        val format = getClipboardFormat(format)

        format.getWriter(path.outputStream(*openOptions)).use { writer ->
            writer.write(schematic.clipboard)
            return WorldEditSchematic(schematic.name, path, schematic.clipboard)
        }
    }

    @JvmStatic
    private fun getLocalSession(player: Player): LocalSession {
        // https://worldedit.enginehub.org/en/latest/api/examples/local-sessions/
        val actor = BukkitAdapter.adapt(player)
        val manager = WorldEdit.getInstance().sessionManager
        return manager.get(actor)
    }

    @JvmStatic
    private fun getCurrentSelection(player: Player): Region? {
        try {
            // https://worldedit.enginehub.org/en/latest/api/examples/clipboard/#copying
            val localSession = getLocalSession(player)
            val selectionWorld = localSession.selectionWorld ?: return null
            return localSession.getSelection(selectionWorld) // This returns an empty clipboard for some reason.
        } catch (ex: Exception) {
            when (ex) {
                is EmptyClipboardException, is IncompleteRegionException -> return null
                else -> throw ex
            }
        }
    }

    /**
     * Checks whatever the player has in "//copy" commmand.
     */
    @JvmStatic
    private fun getCurrentClipboard(player: Player): Clipboard? {
        try {
            val clipboard = getLocalSession(player).clipboard
            // Bake in the transformation before getting the clipboard
            // https://github.com/EngineHub/WorldEdit/blob/79a06b1d21bacfb8b6ab1df1c2452da119fdddf4/worldedit-core/src/main/java/com/sk89q/worldedit/command/ClipboardCommands.java#L221-L240
            return ClipboardTransformBaker.bakeTransform(clipboard)
        } catch (ex: Exception) {
            when (ex) {
                is EmptyClipboardException, is IncompleteRegionException -> return null
                else -> throw ex
            }
        }
    }

    @JvmStatic
    fun loadSchematicIntoClipboard(player: Player, clipboard: WorldEditSchematic) {
        val localSession = getLocalSession(player)
        localSession.clipboard = ClipboardHolder(clipboard.clipboard)
    }

    @JvmStatic
    fun playerHasClipboard(player: Player): Boolean {
        return getCurrentClipboard(player) != null
    }

    @JvmStatic
    fun playerHasSelection(player: Player): Boolean {
        return getCurrentSelection(player) != null
    }

    @JvmStatic
    fun getClipboardOrigin(player: Player): BlockVector3 {
        val localSession = getLocalSession(player)
        return localSession.clipboard.clipboard.origin.adapt
    }

    @JvmStatic
    fun getOriginOfClipboard(player: Player): BlockVector3 {
        return getLocalSession(player).clipboard.clipboard.origin.adapt
    }

    @JvmStatic
    fun changeClipboardOrigin(player: Player, absolute: Boolean, offset: BlockVector3): BlockVector3 {
        val localSession = getLocalSession(player)
        val clipboard = localSession.clipboard.clipboard
        clipboard.origin = if (absolute) offset.adapt else clipboard.origin.add(offset.adapt)
        return clipboard.origin.adapt
    }

    @JvmStatic
    fun savePlayerClipboard(player: Player, schematicFile: Path, schematicName: String): WorldEditSchematic {
        val clipboard = getCurrentClipboard(player) ?: throw IllegalStateException("Player has no clipboard")
        val schematic = WorldEditSchematic(schematicName, schematicFile, clipboard)

        saveSchematic(schematic)
        return schematic
    }

    @JvmStatic
    fun loadSchematic(path: Path, withName: String?): WorldEditSchematic {
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw IllegalArgumentException(
                "Cannot load schematic from: ${path.absolutePathString()} (${
                    Files.exists(
                        path
                    )
                } || ${Files.isRegularFile(path)})"
            )
        }

        val format: ClipboardFormat = try {
            ClipboardFormats.findByFile(path.toFile())
                ?: throw UnknownClipboardFormatException(
                    path,
                    "Unknown clipboard format for file: ${path.absolutePathString()}"
                )
        } catch (ex: NoClassDefFoundError) {
            getClipboardFormat(null)
        }

        format.getReader(path.inputStream(StandardOpenOption.READ)).use { reader ->
            val cb = reader.read()
            return WorldEditSchematic(withName ?: path.nameWithoutExtension, path, cb)
        }
    }
}