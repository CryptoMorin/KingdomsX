package org.kingdoms.enginehub.commands

import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.inventory.EquipmentSlot
import org.kingdoms.commands.*
import org.kingdoms.constants.namespace.Namespace
import org.kingdoms.enginehub.EngineHubLang
import org.kingdoms.enginehub.schematic.SchematicFolderRegistry
import org.kingdoms.enginehub.schematic.SchematicManager
import org.kingdoms.enginehub.schematic.WorldEditSchematic
import org.kingdoms.enginehub.schematic.WorldEditSchematicHandler
import org.kingdoms.managers.backup.FolderZipper
import org.kingdoms.managers.backup.KingdomsBackup
import org.kingdoms.platform.bukkit.adapters.BukkitAdapter
import org.kingdoms.server.location.BlockVector3
import org.kingdoms.utils.LocationUtils
import org.kingdoms.utils.display.visualizer.StructureVisualizer
import org.kingdoms.utils.fs.FSUtil
import org.kingdoms.utils.hash.EntityHashSet
import java.awt.Color
import java.time.Duration
import java.util.*

class CommandAdminSchematicOrigin(parent: KingdomsParentCommand) : KingdomsCommand("origin", parent), Listener {
    companion object {
        private val ORIGIN_TOOL: MutableSet<Player> = EntityHashSet
            .weakBuilder(Player::class.java)
            .onLeave { set, player -> set.remove(player) }
            .build()

        private val ORIGIN_NS_BEFORE: Namespace = Namespace.kingdoms("ORIGIN_BEFORE")
        private val ORIGIN_NS_AFTER: Namespace = Namespace.kingdoms("ORIGIN_AFTER")
    }

    private fun shiftOrigin(player: Player, absolute: Boolean, newOrigin: BlockVector3): Boolean {
        if (!ORIGIN_TOOL.contains(player)) return false
        if (!SchematicManager.hasClipboard(player)) {
            if (SchematicManager.hasSelection(player)) {
                EngineHubLang.COMMAND_ADMIN_SCHEMATIC_SAVE_EMPTY_CLIPBOARD.sendError(player)
            } else {
                EngineHubLang.COMMAND_ADMIN_SCHEMATIC_SAVE_EMPTY_SELECTION.sendError(player)
            }
            return true
        }

        val before = WorldEditSchematicHandler.getOriginOfClipboard(player)
        val finalOrigin = if (absolute) newOrigin else before.add(newOrigin)
        if (before == finalOrigin) {
            EngineHubLang.COMMAND_ADMIN_SCHEMATIC_ORIGIN_SAME.sendError(player)
            return true
        }

        val after = WorldEditSchematicHandler.changeClipboardOrigin(player, true, finalOrigin)

        EngineHubLang.COMMAND_ADMIN_SCHEMATIC_ORIGIN_CHANGED.sendMessage(
            player,
            "origin", LocationUtils.locationMessenger(after)
        )
        sendPreview(player, before, after)
        return true
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    fun onClick(event: PlayerInteractEvent) {
        if (event.hand === EquipmentSlot.OFF_HAND) return
        val block = event.clickedBlock ?: return

        val cancel: Boolean = when (event.action) {
            Action.RIGHT_CLICK_BLOCK, Action.RIGHT_CLICK_AIR -> {
                shiftOrigin(event.player, false, BlockVector3.of(0, +1, 0))
            }

            Action.LEFT_CLICK_BLOCK -> {
                shiftOrigin(event.player, true, BukkitAdapter.adaptLocation(block).toVector())
            }

            else -> false
        }

        if (cancel) event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    fun onShiftDown(event: PlayerToggleSneakEvent) {
        if (!event.isSneaking) return
        shiftOrigin(event.player, false, BlockVector3.of(0, -1, 0))
        // No point in cancelling this.
    }

    fun sendPreview(player: Player, before: BlockVector3, after: BlockVector3?) {
        // We stop both first to prevent one visualizer from overriding the other if before and after are switched.
        StructureVisualizer.stop(player, ORIGIN_NS_BEFORE)
        StructureVisualizer.stop(player, ORIGIN_NS_AFTER)

        StructureVisualizer(ORIGIN_NS_BEFORE, player, setOf(before), Duration.ofSeconds(15)).apply {
            color = Color.GRAY
            blockMarkers()
            particles()
            start()
        }
        if (after !== null) {
            StructureVisualizer(ORIGIN_NS_AFTER, player, setOf(after), Duration.ofSeconds(15)).apply {
                color = Color.GREEN
                blockMarkers()
                particles()
                start()
            }
        }
    }

    override fun execute(context: CommandContext): CommandResult {
        context.assertPlayer()
        val player = context.senderAsPlayer()
        CommandAdminSchematic.RECEIVED_TIPS.add(player.uniqueId)

        CommandAdminSchematicSave.hasClipboard(context)?.let { return it }

        if (context.args.isEmpty()) {
            if (ORIGIN_TOOL.add(player)) {
                context.sendMessage(EngineHubLang.COMMAND_ADMIN_SCHEMATIC_ORIGIN_TOOL_ENABLED)
                val before = WorldEditSchematicHandler.getOriginOfClipboard(player)
                sendPreview(player, before, null)
            } else {
                context.sendMessage(EngineHubLang.COMMAND_ADMIN_SCHEMATIC_ORIGIN_TOOL_DISABLED)
                StructureVisualizer.stop(player, ORIGIN_NS_BEFORE)
                StructureVisualizer.stop(player, ORIGIN_NS_AFTER)
                ORIGIN_TOOL.remove(player)
            }
            return CommandResult.SUCCESS
        }

        context.requireArgs(3)

        fun getCoord(index: Int): Int? {
            return context.getNumber(index, true, false, null)?.value?.toInt()
        }

        val x = getCoord(0) ?: return CommandResult.FAILED
        val y = getCoord(1) ?: return CommandResult.FAILED
        val z = getCoord(2) ?: return CommandResult.FAILED

        val before = WorldEditSchematicHandler.getOriginOfClipboard(player)
        val after = WorldEditSchematicHandler.changeClipboardOrigin(player, false, BlockVector3.of(x, y, z))

        context.`var`("origin", LocationUtils.locationMessenger(after))
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
        context.requireArgs(1)

        val formatName = context.arg(0)
        context.`var`("format", formatName)
        val format = ClipboardFormats.getAll().find { it.name == formatName }

        if (format == null) {
            return context.fail(EngineHubLang.COMMAND_ADMIN_SCHEMATIC_CONVERTALL_UNKNOWN_FORMAT)
        }

        val zipTo = FSUtil.findSlotForCounterFile(
            KingdomsBackup.BACKUPS_FOLDER,
            "schematics-before-conversion-${FSUtil.removeInvalidFileChars(format.name, "ERR")}", "zip"
        )
        FolderZipper.zip(
            null,
            SchematicManager.folder,
            zipTo
        )

        val stats: MutableMap<String, Int> = IdentityHashMap()
        for (schematic in SchematicManager.getSchematics().values) {
            val storedFormat = ClipboardFormats.findByFile(schematic.storedFile.toFile())
            stats.compute(storedFormat?.name ?: "Unknown") { _, v: Int? -> v?.plus(1) ?: 1 }

            try {
                val newSchematic = schematic.apply {
                    WorldEditSchematic(name, storedFile, clipboard, format)
                }
                WorldEditSchematicHandler.saveSchematic(newSchematic)
                SchematicManager.loadAll()
            } catch (ex: Throwable) {
                context.`var`("file", schematic.storedFile)
                context.`var`("error", ex.message)
                return context.fail(EngineHubLang.COMMAND_ADMIN_SCHEMATIC_CONVERTALL_CONVERT_ERROR)
            }
        }

        FSUtil.deleteFolder(SchematicManager.folder)
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