package org.kingdoms.enginehub.schematic

import org.kingdoms.data.Pair
import org.kingdoms.enginehub.EngineHubAddon
import org.kingdoms.main.KLogger
import org.kingdoms.main.Kingdoms
import org.kingdoms.utils.fs.FolderRegistry
import org.kingdoms.utils.internal.stacktrace.StackTraces
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Path
import kotlin.io.path.*

class SchematicFolderRegistry(displayName: String, private val folderName: String) :
    FolderRegistry(displayName, Kingdoms.getPath(folderName)) {
    val extension: String?

    init {
        val usedExtension = WorldEditSchematicHandler.getUsedFileExtension(null)
        if (usedExtension.reason !== WorldEditSchematicHandler.UsedExtension.Reason.DEFAULT) {
            this.extension = usedExtension.extension
        } else {
            this.extension = null
        }

        val msg: String? = when (usedExtension.reason) {
            WorldEditSchematicHandler.UsedExtension.Reason.FORCED -> "it's explicitly set in enginehub.yml"
            WorldEditSchematicHandler.UsedExtension.Reason.USING_FAWE -> "you're using FastAsyncWorldEdit"
            else -> null
        }

        if (msg !== null)
            EngineHubAddon.INSTANCE.logger.info("Renaming all schematics with '${usedExtension.extension}' extension because $msg")
    }

    override fun getDefaultsURI(): Pair<String, URI> {
        val uri: URI
        try {
            uri = WorldEditSchematic::class.java.getResource("/$folderName")!!.toURI()
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        }
        return Pair.of(folderName, uri)
    }

    override fun handle(entry: Entry) {
        var schematicFile = entry.path
        val schematicName = entry.name
        if (schematicName.contains("schematic"))
            StackTraces.printStackTrace("Found illegal duplicated .schematic extension for ${entry.name} -> ${entry.path}")

        var schematic: WorldEditSchematic? = null
        val extension = if (this.extension === null) {
            schematic = loadSchem(schematicName, schematicFile) ?: return
            WorldEditSchematicHandler.getUsedFileExtension(schematic.clipboardFormat).extension
        } else {
            this.extension
        }

        if (extension.substringAfter('.') != schematicFile.extension) {
            val rename = schematicFile.parent.resolve(schematicFile.nameWithoutExtension + extension)
            EngineHubAddon.INSTANCE.logger.info("Renaming $schematicFile -> $rename")

            // Don't explicitly use StandardCopyOption.COPY_ATTRIBUTES, because it'll throw
            // java.lang.UnsupportedOperationException: Unsupported option: COPY_ATTRIBUTES
            // Looks like moving a file will automatically copy attributes regardless?
            schematicFile.moveTo(rename)
            schematicFile = rename
            if (schematic !== null) {
                schematic = schematic.apply { WorldEditSchematic(name, schematicFile, clipboard, clipboardFormat) }
            }
        }

        if (schematic === null) {
            schematic = loadSchem(schematicName, schematicFile) ?: return
        }
        if (SchematicManager.loaded.containsKey(schematicName)) {
            KLogger.error("Found two schematics with the same name: ${schematicFile.absolutePathString()}")
        }
        if (schematicFile.name.contains(' ')) {
            KLogger.warn("Schematic file names should not contain space: ${schematicFile.absolutePathString()}")
        }
        SchematicManager.loaded[schematicName] = schematic
    }

    private fun loadSchem(schematicName: String, schematicFile: Path): WorldEditSchematic? {
        return try {
            WorldEditSchematicHandler.loadSchematic(schematicFile, withName = schematicName)
        } catch (ex: UnknownClipboardFormatException) {
            KLogger.error(
                "Unknown clipboard format for schematic ${ex.path.toAbsolutePath()}, this means that the file " +
                        "you're trying to use is either corrupted, not a schematic file or not supported by your " +
                        "current WorldEdit installation. Skipping this schematic: " + ex.message
            )
            return null
        } catch (ex: Throwable) {
            KLogger.error("Failed to load schematic: $schematicFile")
            ex.printStackTrace()
            return null
        }
    }

    override fun register() {
        if (folder.exists()) {
            visitPresent()
            if (useDefaults) visitDefaults()
        } else {
            visitDefaults()
        }
    }
}