package org.kingdoms.enginehub.commands

import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.kingdoms.commands.*
import org.kingdoms.constants.namespace.Namespace
import org.kingdoms.enginehub.EngineHubLang
import org.kingdoms.enginehub.schematic.SchematicManager
import org.kingdoms.enginehub.schematic.WorldEditSchematicHandler
import org.kingdoms.main.Kingdoms
import org.kingdoms.managers.backup.FolderZipper
import org.kingdoms.server.location.BlockVector3
import org.kingdoms.utils.display.visualizer.StructureVisualizer
import org.kingdoms.utils.fs.FSUtil
import java.awt.Color
import java.time.Duration
import java.util.*

class CommandAdminSchematicOrigin(parent: KingdomsParentCommand) : KingdomsCommand("origin", parent), Listener {
    companion object {
        @JvmField val ORIGIN_TOOL: MutableSet<UUID> = hashSetOf()
        @JvmField val ORIGIN_NS_BEFORE = Namespace.kingdoms("ORIGIN_BEFORE")
        @JvmField val ORIGIN_NS_AFTER = Namespace.kingdoms("ORIGIN_AFTER")
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    fun onClick(event: PlayerInteractEvent) {
        if (event.action == Action.RIGHT_CLICK_BLOCK) return
        val block = event.clickedBlock ?: return
        val player = event.player

        if (!ORIGIN_TOOL.contains(player.uniqueId)) return
        if (!SchematicManager.hasClipboard(player)) {
            return if (SchematicManager.hasSelection(player)) {
                EngineHubLang.COMMAND_ADMIN_SCHEMATIC_SAVE_EMPTY_CLIPBOARD.sendError(player)
            } else {
                EngineHubLang.COMMAND_ADMIN_SCHEMATIC_SAVE_EMPTY_SELECTION.sendError(player)
            }
        }

        val before = WorldEditSchematicHandler.getOriginOfClipboard(player)
        val after =
            WorldEditSchematicHandler.changeClipboardOrigin(player, true, BlockVector3.of(block.x, block.y, block.z))

        EngineHubLang.COMMAND_ADMIN_SCHEMATIC_ORIGIN_CHANGED.sendMessage(player)
        event.isCancelled = true
        sendPreview(player, before, after)
    }

    fun sendPreview(player: Player, before: BlockVector3, after: BlockVector3?) {
        StructureVisualizer(ORIGIN_NS_BEFORE, player, setOf(before), Duration.ofSeconds(15)).apply {
            color = Color.GRAY
            start()
        }
        if (after != null) {
            StructureVisualizer(ORIGIN_NS_AFTER, player, setOf(after), Duration.ofSeconds(15)).apply {
                color = Color.GREEN
                start()
            }
        }
    }

    override fun execute(context: CommandContext): CommandResult {
        if (context.assertPlayer()) return CommandResult.FAILED
        val player = context.senderAsPlayer()

        CommandAdminSchematicSave.hasClipboard(context)?.let { return it }

        if (context.args.isEmpty()) {
            if (ORIGIN_TOOL.add(player.uniqueId)) {
                context.sendMessage(EngineHubLang.COMMAND_ADMIN_SCHEMATIC_ORIGIN_TOOL_ENABLED)
                val before = WorldEditSchematicHandler.getOriginOfClipboard(player)
                sendPreview(player, before, null)
            } else {
                context.sendMessage(EngineHubLang.COMMAND_ADMIN_SCHEMATIC_ORIGIN_TOOL_DISABLED)
                StructureVisualizer.stop(player, ORIGIN_NS_BEFORE)
                StructureVisualizer.stop(player, ORIGIN_NS_AFTER)
                ORIGIN_TOOL.remove(player.uniqueId)
            }
            return CommandResult.SUCCESS
        }

        if (context.requireArgs(3)) return CommandResult.FAILED

        fun getCoord(index: Int): Int? {
            return context.getNumber(index, true, false, null)?.value?.toInt()
        }

        val x = getCoord(0) ?: return CommandResult.FAILED
        val y = getCoord(1) ?: return CommandResult.FAILED
        val z = getCoord(2) ?: return CommandResult.FAILED

        val before = WorldEditSchematicHandler.getOriginOfClipboard(player)
        val after = WorldEditSchematicHandler.changeClipboardOrigin(player, false, BlockVector3.of(x, y, z))

        context.sendMessage(EngineHubLang.COMMAND_ADMIN_SCHEMATIC_ORIGIN_CHANGED)
        sendPreview(player, before, after)
        return CommandResult.SUCCESS
    }

    override fun tabComplete(context: CommandTabContext): MutableList<String> {
        val str = when (context.parameterPosition) {
            1 -> "<x>"
            2 -> "<y>"
            3 -> "<z>"
            else -> null
        }

        return if (str == null) context.emptyTab() else context.tabComplete(str)
    }
}

class CommandAdminSchematicConvertAll(parent: KingdomsParentCommand) : KingdomsCommand("convertAll", parent) {
    override fun execute(context: CommandContext): CommandResult {
        CommandAdminSchematic.handleBasics(context).let { if (it != CommandResult.SUCCESS) return it }
        val formatName = context.arg(0)
        context.`var`("format", formatName)
        val format = ClipboardFormats.getAll().find { it.name == formatName }

        if (format == null) {
            return context.fail(EngineHubLang.COMMAND_ADMIN_SCHEMATIC_CONVERTALL_UNKNOWN_FORMAT)
        }

        FolderZipper.zip(
            "schematics-before-conversion-${FSUtil.removeInvalidFileChars(format.name, "ERR")}",
            SchematicManager.folder,
            Kingdoms.getFolder()
        )

        val stats: MutableMap<String, Int> = IdentityHashMap()
        for (schematic in SchematicManager.getSchematics().values) {
            val storedFormat = ClipboardFormats.findByFile(schematic.storedFile.toFile())
            stats.compute(storedFormat?.name ?: "Unknown") { _, v: Int? -> v?.plus(1) ?: 1 }

            try {
                WorldEditSchematicHandler.saveSchematic(schematic, format)
            } catch (ex: Throwable) {
                context.`var`("file", schematic.storedFile)
                context.`var`("error", ex.message)
                return context.fail(EngineHubLang.COMMAND_ADMIN_SCHEMATIC_CONVERTALL_CONVERT_ERROR)
            }
        }

        SchematicManager.loadAll()
        context.`var`("converted", stats.values.sum())
        context.messageContext.parse("stats", stats.map { "\n  &8| &2${it.key} &7-> &6${it.value}" }.joinToString(""))
        context.sendMessage(EngineHubLang.COMMAND_ADMIN_SCHEMATIC_CONVERTALL_CONVERTED)
        return CommandResult.SUCCESS
    }

    override fun tabComplete(context: CommandTabContext): MutableList<String> {
        return ClipboardFormats.getAll().map { it.name }.toMutableList()
    }
}