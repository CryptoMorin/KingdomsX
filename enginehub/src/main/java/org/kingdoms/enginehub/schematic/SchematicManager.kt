package org.kingdoms.enginehub.schematic

import com.sk89q.worldedit.bukkit.adapter.UnsupportedVersionEditException
import com.sk89q.worldedit.extent.clipboard.Clipboard
import org.bukkit.entity.Player
import org.kingdoms.enginehub.EngineHubAddon
import org.kingdoms.enginehub.commands.CommandAdminSchematicSetup
import org.kingdoms.enginehub.schematic.WorldEditSchematicHandler.getClipboardFormat
import org.kingdoms.enginehub.schematic.WorldEditSchematicHandler.isUsingFAWE
import org.kingdoms.locale.MessageHandler
import org.kingdoms.main.KLogger
import org.kingdoms.main.Kingdoms
import org.kingdoms.main.KingdomsGlobalsCenter
import org.kingdoms.server.location.BlockVector3
import org.kingdoms.server.location.Direction
import org.kingdoms.utils.config.ConfigSection
import org.kingdoms.utils.internal.reflection.Reflect
import org.kingdoms.utils.internal.stacktrace.StackTraces
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.name

object SchematicManager {
    // We can't really directly check this.
    // Also, this option can influence this:
    // WorldEditPlugin.getInstance().getLocalConfiguration().unsupportedVersionEditing
    var IS_SUPPORTED_VERSION = true
    var UNSUPPORTED_FAWE = false
    private val UnsupportedVersionEditException_exists =
        Reflect.classExists("com.sk89q.worldedit.bukkit.adapter.UnsupportedVersionEditException")

    val folder: Path = Kingdoms.getPath("schematics")
    internal val loaded: MutableMap<String, WorldEditSchematic> = java.util.concurrent.ConcurrentHashMap()

    fun isUnsupportedFAWEError(exception: Throwable): Boolean {
        return exception is NullPointerException
                && exception.message == "Cannot invoke \"org.enginehub.linbus.tree.LinTag.value()\" because \"rotTag\" is null"
                && isUsingFAWE()
    }

    fun isUnsupportedVersionError(exception: Throwable): Boolean {
        return UnsupportedVersionEditException_exists &&
                StackTraces.findCause(exception) { it is UnsupportedVersionEditException } !== null
    }

    fun warnUnsupportedFawe(exception: Throwable) {
        UNSUPPORTED_FAWE = true
        val logger = EngineHubAddon.INSTANCE.logger
        logger.severe("-------------------------------------------------------------------")
        logger.severe("FastAsyncWorldEdit is not supported.")
        logger.severe("FastAsyncWorldEdit: " + exception.message)
        logger.severe("-------------------------------------------------------------------")
        if (KLogger.isDebugging()) exception.printStackTrace()
    }

    fun warnUnsupported(exception: Throwable) {
        IS_SUPPORTED_VERSION = false
        val logger = EngineHubAddon.INSTANCE.logger
        logger.severe("-------------------------------------------------------------------")
        logger.severe("The current WorldEdit you're using doesn't support your server version. Buildings will not be built properly.")
        logger.severe("WorldEdit: " + exception.message)
        logger.severe("-------------------------------------------------------------------")
        if (KLogger.isDebugging()) exception.printStackTrace()
    }

    @JvmStatic
    fun loadAll() {
        loaded.clear()
        SchematicFolderRegistry("Schematic", folder.name).useDefaults(true).copyDefaults(true).register()
        MessageHandler.sendConsolePluginMessage("&2Loaded a total of &6${loaded.size} &2schematics. from ${folder.name}");
    }

    @JvmStatic
    fun setup() {
        val globals = KingdomsGlobalsCenter.get()
        val enginehub = globals.createSection("enginehub")
        val setupVersion = enginehub.getInt("setup-version")
        val skipSetups = enginehub.getBoolean("skip-setups")

        fun setupMsg(msg: String): String {
            return if (skipSetups) "However setups are skipped in globals.yml, ignoring..." else msg
        }

        if (setupVersion == 0) {
            val msg = "No setup version detected. " + setupMsg("Setting up and changing the building configs...")
            EngineHubAddon.INSTANCE.logger.info(msg)

            if (!skipSetups) setupLatest(enginehub)
        } else {
            if (setupVersion < 2) {
                val msg = "Outdated setup version $setupVersion. " + setupMsg("Updating your building configs...")
                EngineHubAddon.INSTANCE.logger.info(msg)

                if (!skipSetups) setupLatest(enginehub)
            } else {
                EngineHubAddon.INSTANCE.logger.info("Setup version: $setupVersion" + (if (skipSetups) " (Setups are skipped)" else ""))
            }
        }
    }

    private fun setupLatest(enginehub: ConfigSection) {
        CommandAdminSchematicSetup.setup()
        enginehub.set("setup-version", 2)
        KingdomsGlobalsCenter.adapter().saveConfig()
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
        val clipboardFormat = getClipboardFormat(null)
        val extension = WorldEditSchematicHandler.getUsedFileExtension(clipboardFormat).extension

        val path = folder.resolve(Paths.get(name + extension))
        Files.createDirectories(path.parent)

        val schematic = WorldEditSchematicHandler.savePlayerClipboard(player, path, name, clipboardFormat)
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