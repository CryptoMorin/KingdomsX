package org.kingdoms.enginehub.schematic

import org.kingdoms.config.managers.ConfigManager
import org.kingdoms.data.Pair
import org.kingdoms.enginehub.EngineHubAddon
import org.kingdoms.enginehub.commands.CommandAdminSchematicSetup
import org.kingdoms.main.KLogger
import org.kingdoms.main.Kingdoms
import org.kingdoms.utils.fs.FolderRegistry
import org.kingdoms.utils.internal.stacktrace.StackTraces
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.name

class SchematicFolderRegistry(displayName: String, private val folderName: String) :
    FolderRegistry(displayName, Kingdoms.getPath(folderName)) {

    override fun getDefaultsURI(): Pair<String, URI> {
        val uri: URI
        try {
            uri = WorldEditSchematic::class.java.getResource("/$folderName")!!.toURI()
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        }
        return Pair.of(folderName, uri)
    }

    override fun filter(path: Path): Boolean = !path.name.endsWith(".yml")

    override fun handle(entry: Entry) {
        val schematicFile = entry.path
        val schematicName = entry.name
        if (schematicName.contains("schematic"))
            StackTraces.printStackTrace("Found illegal schematic name at ${entry.name} -> ${entry.path}")

        val schematic = try {
            WorldEditSchematicHandler.loadSchematic(schematicFile, withName = schematicName)
        } catch (ex: UnknownClipboardFormatException) {
            KLogger.error(
                "Unknown clipboard format for schematic ${ex.path.toAbsolutePath()}, this means that the file " +
                        "you're trying to use is either corrupted, not a schematic file or not supported by your " +
                        "current WorldEdit installation. Skipping this schematic..."
            )
            return
        }

        if (SchematicManager.loaded.containsKey(schematicName)) {
            KLogger.error("Found two schematics with the same name: ${schematicFile.absolutePathString()}")
        }
        if (schematicFile.name.contains(' ')) {
            KLogger.warn("Schematic file names should not contain space: ${schematicFile.absolutePathString()}")
        }
        SchematicManager.loaded[schematicName] = schematic
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