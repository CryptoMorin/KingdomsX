package org.kingdoms.enginehub.schematic

import com.sk89q.worldedit.extent.clipboard.Clipboard
import org.bukkit.entity.Player
import org.kingdoms.config.managers.ConfigManager
import org.kingdoms.enginehub.EngineHubAddon
import org.kingdoms.enginehub.commands.CommandAdminSchematicSetup
import org.kingdoms.locale.MessageHandler
import org.kingdoms.main.Kingdoms
import org.kingdoms.server.location.BlockVector3
import org.kingdoms.server.location.Direction
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.name

object SchematicManager {
    val folder: Path = Kingdoms.getPath("schematics")
    internal val loaded: MutableMap<String, WorldEditSchematic> = java.util.concurrent.ConcurrentHashMap()

    @JvmStatic
    fun loadAll() {
        loaded.clear()
        SchematicFolderRegistry("Schematic", folder.name).useDefaults(true).copyDefaults(true).register()
        MessageHandler.sendConsolePluginMessage("&2Loaded a total of &6${loaded.size} &2schematics. from ${folder.name}");
    }

    @JvmStatic
    fun setup() {
        val globals = ConfigManager.getGlobals()
        val enginehub = globals.createSection("enginehub")
        val setupVersion = enginehub.getInt("setup-version")
        if (setupVersion == 0) {
            EngineHubAddon.INSTANCE.logger.info("No setup version detected. Setting up and changing the building configs...")
            CommandAdminSchematicSetup.setup()
            enginehub.set("setup-version", 1)
            ConfigManager.getGlobalsAdapter().saveConfig()
        } else {
            EngineHubAddon.INSTANCE.logger.info("Setup version: $setupVersion")
        }
    }

    @JvmStatic
    fun hasClipboard(player: Player): Boolean = WorldEditSchematicHandler.playerHasClipboard(player)

    @JvmStatic
    fun hasSelection(player: Player): Boolean = WorldEditSchematicHandler.playerHasSelection(player)

    @JvmStatic
    fun getSchematic(name: String): WorldEditSchematic? = loaded[name]

    @JvmStatic
    fun loadSchematic(name: String, player: Player) {
        val schematic = loaded[name] ?: throw IllegalArgumentException("Unknown schematic: $name")
        WorldEditSchematicHandler.loadSchematicIntoClipboard(player, schematic)
    }

    @JvmStatic
    fun saveSchematic(name: String, player: Player): Path {
        val path = folder.resolve(Paths.get(name + WorldEditSchematicHandler.FILE_EXTENSION))
        Files.createDirectories(path.parent)

        val schematic = WorldEditSchematicHandler.savePlayerClipboard(player, path, name)
        loaded[schematic.name] = schematic
        return path
    }

    @JvmStatic
    fun schematicExists(name: String): Boolean = loaded.containsKey(name)

    fun getSchematics(): Map<String, WorldEditSchematic> = loaded

    fun getPopulatedBlocks(
        schematic: WorldEditSchematic, origin: BlockVector3,
        facing: Direction, sortingStrategy: Comparator<BlockVector3>
    ): Pair<Clipboard, CalculatedBlocks> {
        return schematic.populate(
            origin,
            facing,
            ignoreAir = true,
            useMap = linkedMapOf(),
            sortingStrategy = sortingStrategy
        )
    }
}