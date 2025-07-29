package org.kingdoms.enginehub.schematic

import com.cryptomorin.xseries.reflection.XReflection
import com.sk89q.worldedit.EmptyClipboardException
import com.sk89q.worldedit.IncompleteRegionException
import com.sk89q.worldedit.LocalSession
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.regions.Region
import org.bukkit.entity.Player
import org.kingdoms.enginehub.EngineHubAddon
import org.kingdoms.enginehub.EngineHubConfig
import org.kingdoms.enginehub.schematic.blocks.ClipboardTransformBaker
import org.kingdoms.enginehub.worldedit.XClipboardFormat
import org.kingdoms.enginehub.worldedit.XClipboardFormatFactory
import org.kingdoms.enginehub.worldedit.XWorldEditBukkitAdapterFactory
import org.kingdoms.main.KLogger
import org.kingdoms.server.location.BlockVector3
import org.kingdoms.utils.debugging.DebugNS
import org.kingdoms.utils.internal.enumeration.Enums
import org.kingdoms.utils.internal.reflection.Reflect
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
    private object Debugger : DebugNS {
        override fun namespace(): String = "ENGINEHUB_WORLDEDIT_SCHEMATICS"
    }

    // FAWE Custom | boolean isFormat(InputStream) https://github.com/IntellectualSites/FastAsyncWorldEdit/blob/cca3b719162246b0ca018cffb55dcaaab67426f3/worldedit-core/src/main/java/com/sk89q/worldedit/extent/clipboard/io/ClipboardFormat.java#L94-L108
    private val FAWE_ISFORMAT_INPUTSTREAM = XReflection
        .of(ClipboardFormat::class.java)
        .method("public boolean isFormat(java.io.InputStream inputStream)")
        .reflectOrNull()

    private val openOptions = arrayOf(
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE
    )

    @JvmStatic
    fun isUsingFAWE(): Boolean = Reflect.classExists("com.fastasyncworldedit.bukkit.FaweBukkit")

    class UsedExtension(val extension: String, val reason: Reason, val clipboardFormat: XClipboardFormat?) {
        enum class Reason { FORCED, USING_FAWE, PREFERRED_FORMAT_EXTENSION, DEFAULT }
    }

    @JvmStatic
    fun getUsedFileExtension(clipboardFormat: XClipboardFormat?): UsedExtension {
        val forced = EngineHubConfig.WORLDEDIT_SCHEMATICS_FORCE_EXTENSION.manager.string
        if (forced !== null && forced.isNotBlank()) return UsedExtension(
            forced,
            UsedExtension.Reason.FORCED,
            clipboardFormat
        )

        if (clipboardFormat !== null) UsedExtension(
            clipboardFormat.primaryFileExtension,
            UsedExtension.Reason.PREFERRED_FORMAT_EXTENSION,
            clipboardFormat
        )
        return if (isUsingFAWE()) UsedExtension(".schem", UsedExtension.Reason.USING_FAWE, clipboardFormat)
        else UsedExtension(".schematic", UsedExtension.Reason.DEFAULT, clipboardFormat)
    }

    @JvmStatic
    fun getClipboardFormat(format: XClipboardFormat? = null): XClipboardFormat {
        if (format !== null) return format

        val defaultFormat = EngineHubConfig.WORLDEDIT_SCHEMATICS_DEFAULT_SAVE_FORMAT.manager.string
        if (defaultFormat !== null && defaultFormat.isNotBlank()) {
            val foundFormat = findClipboardFormat(
                formatName = defaultFormat,
                optionDetails = "default schematic format defined in enginehub.yml default save format"
            )
            if (foundFormat !== null) return foundFormat
        }

        return findHardcodedFormat()
    }

    private fun findHardcodedFormat(): XClipboardFormat {
        val clipboardFormat = if (ClipboardFormat::class.java.isEnum) {
            XReflection.of(ClipboardFormat::class.java).enums().named("SCHEMATIC").enumConstant as ClipboardFormat
        } else {
            Enums.findOneOf(
                BuiltInClipboardFormat::class.java,
                "SPONGE_V3_SCHEMATIC",
                "SPONGE_SCHEMATIC"
            )
        }
        return XClipboardFormatFactory.of(clipboardFormat)
    }

    private fun findClipboardFormat(formatName: String, optionDetails: String): XClipboardFormat? {
        var findByAlias = ClipboardFormats.findByAlias(formatName)
        if (findByAlias === null) {
            findByAlias = ClipboardFormats.getAll().find {
                it.name.equals(formatName, ignoreCase = true) ||
                        it.aliases.any { alias ->
                            alias.equals(formatName, ignoreCase = true)
                        }
            }
        }

        if (findByAlias === null) {
            val availableFormats = ClipboardFormats.getAll().joinToString(", ") {
                it.name + "(" + it.aliases.joinToString(", ") + ")"
            }

            KLogger.warn("The $optionDetails '$formatName' doesn't exist, available formats: $availableFormats")
            return null
        } else {
            return XClipboardFormatFactory.of(findByAlias)
        }
    }

    @JvmStatic
    fun saveSchematic(schematic: WorldEditSchematic): WorldEditSchematic {
        val path = schematic.storedFile

        KLogger.debug(Debugger) { "Saving schematic '$path' using format '${schematic.clipboardFormat.name}'" }
        schematic.clipboardFormat.write(path.outputStream(*openOptions), schematic.clipboard)
        return WorldEditSchematic(schematic.name, path, schematic.clipboard, schematic.clipboardFormat)
    }

    @JvmStatic
    private fun getLocalSession(player: Player): LocalSession {
        // https://worldedit.enginehub.org/en/latest/api/examples/local-sessions/
        val actor = XWorldEditBukkitAdapterFactory.INSTANCE.adapt(player)
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
        XWorldEditBukkitAdapterFactory.INSTANCE.setClipboard(localSession, clipboard.clipboard)
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
    fun getOriginOfClipboard(player: Player): BlockVector3 {
        return XWorldEditBukkitAdapterFactory.INSTANCE.adapt(getLocalSession(player).clipboard.clipboard).origin
    }

    @JvmStatic
    fun changeClipboardOrigin(player: Player, absolute: Boolean, offset: BlockVector3): BlockVector3 {
        val localSession = getLocalSession(player)
        val clipboard = XWorldEditBukkitAdapterFactory.INSTANCE.adapt(localSession.clipboard.clipboard)

        clipboard.origin = if (absolute) offset else clipboard.origin.add(offset)
        return clipboard.origin
    }

    @JvmStatic
    fun savePlayerClipboard(
        player: Player,
        schematicFile: Path,
        schematicName: String,
        clipboardFormat: XClipboardFormat
    ): WorldEditSchematic {
        val clipboard = getCurrentClipboard(player) ?: throw IllegalStateException("Player has no clipboard")
        val schematic = WorldEditSchematic(schematicName, schematicFile, clipboard, clipboardFormat)

        return saveSchematic(schematic)
    }

    @JvmStatic
    fun loadSchematic(path: Path, withName: String?): WorldEditSchematic {
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw IllegalArgumentException(
                "Cannot load schematic from: ${path.absolutePathString()} " +
                        "(${Files.exists(path)} || ${Files.isRegularFile(path)})"
            )
        }

        // FAWE formats: https://github.com/IntellectualSites/FastAsyncWorldEdit/blob/main/worldedit-core/src/main/java/com/sk89q/worldedit/extent/clipboard/io/BuiltInClipboardFormat.java#L525
        var format: XClipboardFormat? = null
        val forcedFormatName = EngineHubConfig.WORLDEDIT_SCHEMATICS_LOADING_MECHANISM_FORCED_FORMAT.manager.string
        if (forcedFormatName !== null && forcedFormatName.isNotBlank()) {
            val foundFormat = findClipboardFormat(
                formatName = forcedFormatName,
                optionDetails = "default schematic format defined in enginehub.yml default save format"
            )
            if (foundFormat !== null) format = foundFormat
        }

        if (format === null) {
            format = tryGetStdClipboardFormat(path)
        }

        if (format === null)
            throw UnknownClipboardFormatException(
                path,
                "Could not find a clipboard format for ${path.absolutePathString()}"
            )

        KLogger.debug(Debugger) { "Loading schematic '$path' using format '${format.name}'" }
        val clipbaord = format.read(path.inputStream(StandardOpenOption.READ))
        return WorldEditSchematic(withName ?: path.nameWithoutExtension, path, clipbaord, format)
    }

    @JvmStatic fun tryGetStdClipboardFormat(path: Path): XClipboardFormat? {
        if (isUsingFAWE() && EngineHubConfig.WORLDEDIT_SCHEMATICS_LOADING_MECHANISM_BYPASS_FAWE_EXTENSION_CHECK.manager.boolean) {
            val handle = FAWE_ISFORMAT_INPUTSTREAM!!
            val inputStream = path.inputStream(StandardOpenOption.READ)
            try {
                val format = ClipboardFormats.getAll().find { allFormats ->
                    handle.invokeExact(allFormats, inputStream) as Boolean
                }
                if (format !== null) return XClipboardFormatFactory.of(format)
            } catch (ex: Throwable) {
                EngineHubAddon.INSTANCE.logger.severe("Error while attempting to bypass FAWE's schematic extension check:")
                ex.printStackTrace()
            }
        }

        return try {
            XClipboardFormatFactory.of(
                ClipboardFormats.findByFile(path.toFile())
                    ?: throw UnknownClipboardFormatException(
                        path,
                        "Unknown clipboard format for file: ${path.absolutePathString()}"
                    )
            )
        } catch (ex: NoClassDefFoundError) {
            findHardcodedFormat()
        }
    }
}