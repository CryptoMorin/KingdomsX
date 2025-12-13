package org.kingdoms.enginehub.commands

import com.cryptomorin.xseries.reflection.XReflection
import com.sk89q.worldedit.IncompleteRegionException
import com.sk89q.worldedit.LocalSession
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.function.operation.ForwardExtentCopy
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.transform.AffineTransform
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.session.ClipboardHolder
import org.kingdoms.commands.*
import org.kingdoms.commands.annotations.Cmd
import org.kingdoms.commands.annotations.CmdParent
import org.kingdoms.data.Pair
import org.kingdoms.enginehub.EngineHubLang
import org.kingdoms.enginehub.building.WorldEditBuildingConstruction
import org.kingdoms.enginehub.schematic.SchematicManager
import org.kingdoms.enginehub.schematic.WorldEditSchematic
import org.kingdoms.enginehub.schematic.WorldEditSchematicHandler
import org.kingdoms.enginehub.schematic.blocks.ClipboardBlockIterator
import org.kingdoms.enginehub.worldedit.XWorldEditBukkitAdapterFactory
import org.kingdoms.locale.KingdomsLang
import org.kingdoms.locale.compiler.MessageCompiler
import org.kingdoms.locale.messenger.ContextualMessenger
import org.kingdoms.locale.messenger.StaticMessenger
import org.kingdoms.locale.placeholders.context.MessagePlaceholderProvider
import org.kingdoms.locale.placeholders.context.PlaceholderContextBuilder
import org.kingdoms.locale.provider.MessageProvider
import org.kingdoms.main.Kingdoms
import org.kingdoms.server.location.BlockVector3
import org.kingdoms.server.location.Direction
import org.kingdoms.utils.SIPrefix
import org.kingdoms.utils.internal.numbers.Numbers
import org.kingdoms.utils.string.tree.DefaultStringTreeSettings
import org.kingdoms.utils.string.tree.StringPathBuilder
import org.kingdoms.utils.string.tree.TreeBuilder
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Function
import kotlin.io.path.fileSize

@Cmd("copy")
@CmdParent(CommandAdminSchematic::class)
class CommandAdminSchematicCopy : KingdomsCommand() {
    companion object {
        private val ForwardExtentCopy_affectedBlocks =
            XReflection.of(ForwardExtentCopy::class.java)
                .field("private int affectedBlocks")
                .getter()
                .reflectOrNull()
    }

    override fun execute(context: CommandContext): CommandResult {
        // https://github.com/EngineHub/WorldEdit/blob/7c8bb399a110b59102dd5f9a6c5c8434536623db/worldedit-core/src/main/java/com/sk89q/worldedit/command/ClipboardCommands.java#L85-L111
        context.assertPlayer()
        val player = context.senderAsPlayer()
        val wePlayer = BukkitAdapter.adapt(player)

        val session: LocalSession =
            WorldEdit.getInstance().sessionManager.get(wePlayer)

        val region: Region = try {
            session.getSelection(wePlayer.world)
        } catch (_: IncompleteRegionException) {
            return context.fail(EngineHubLang.COMMAND_ADMIN_SCHEMATIC_COPY_NO_SELECTION)
        }

        val limit = session.blockChangeLimit
        val blockSize = region.boundingBox.volume
        context.`var`("max_block_limit", limit)
        context.`var`("block_size", blockSize)

        if (limit >= 0 && blockSize >= limit) {
            return context.fail(EngineHubLang.COMMAND_ADMIN_SCHEMATIC_COPY_LIMIT)
        }

        val clipboard = BlockArrayClipboard(region)
        // clipboard.origin = session.getPlacementPosition(wePlayer)
        clipboard.origin = com.sk89q.worldedit.math.BlockVector3.at(
            region.center.x, region.minimumPoint.y.toDouble(), region.center.z
        )

        @Suppress("DEPRECATION")
        val editSession = WorldEdit.getInstance()
            .editSessionFactory
            .getEditSession(wePlayer.world, -1)

        val copy = ForwardExtentCopy(
            editSession,
            region,
            clipboard,
            region.minimumPoint
        )

        copy.isCopyingBiomes = false
        copy.isCopyingEntities = false
        copy.isRemovingEntities = false
        Operations.complete(copy)
        session.clipboard = ClipboardHolder(clipboard)

        // Normalize rotation after copying
        val playerDir = Direction.fromYaw(player.eyeLocation.yaw)
        context.`var`("current_direction", playerDir.name)
        if (playerDir.type !== Direction.Type.CARDINAL) {
            return context.fail(EngineHubLang.COMMAND_ADMIN_SCHEMATIC_COPY_NOT_CARDINAL_FACING)
        }

        val facing = getYRotationToFace(playerDir, Direction.WEST)
        session.clipboard.transform = AffineTransform().rotateY(facing)

        val affectedBlocks = if (ForwardExtentCopy_affectedBlocks === null) {
            KingdomsLang.UNKNOWN
        } else {
            ForwardExtentCopy_affectedBlocks.invoke(copy)
        }

        context.`var`("entities", clipboard.entities.size)
        context.messageContext.raw("blocks", affectedBlocks)
        context.sendMessage(EngineHubLang.COMMAND_ADMIN_SCHEMATIC_COPY_DONE)
        // copy.statusMessages.forEach { wePlayer.print(it) }
        return CommandResult.SUCCESS
    }

    fun getYRotationToFace(currentDir: Direction, targetFacing: Direction): Double {
        val currentIndex = when (currentDir) {
            Direction.SOUTH -> 0
            Direction.EAST -> 1
            Direction.NORTH -> 2
            Direction.WEST -> 3
            else -> throw AssertionError("Not a cardinal direction for current facing: $currentDir")
        }

        val targetIndex = when (targetFacing) {
            Direction.SOUTH -> 0
            Direction.EAST -> 1
            Direction.NORTH -> 2
            Direction.WEST -> 3
            else -> throw AssertionError("Not a cardinal direction for target facing: $targetFacing")
        }

        // Clockwise steps needed to go from current to target
        val steps = (targetIndex - currentIndex + 4.0) % 4.0

        // Each step = +90 degrees (clockwise)
        return steps * 90.0
    }
}

class CommandAdminSchematicSave(parent: KingdomsParentCommand) : KingdomsCommand("save", parent) {
    override fun execute(context: CommandContext): CommandResult {
        context.requireArgs(1)
        context.assertPlayer()

        val player = context.senderAsPlayer()
        val schemName = context.arg(0)
        context.`var`("schematic_name", schemName)

        if (schemName.contains('.')) {
            return context.fail(EngineHubLang.COMMAND_ADMIN_SCHEMATIC_EXTENSION)
        }

        hasClipboard(context)?.let { return it }
        val clipboard = WorldEditSchematicHandler.getCurrentClipboard(player)!!
        if (!WorldEditSchematic.isOriginPartOfSchematic(clipboard)) {
            val center = clipboard.region.center
            val origin = clipboard.origin

            context.`var`(
                "origin",
                "[" + (center.x - origin.x) + ", " + (center.y - origin.y) + ", " + (center.z - origin.z) + "]"
            )
            return context.fail(EngineHubLang.COMMAND_ADMIN_SCHEMATIC_ORIGIN_MISMATCH)
        }

        if (SchematicManager.schematicExists(schemName)) {
            if (context.requireConfirmation(EngineHubLang.COMMAND_ADMIN_SCHEMATIC_SAVE_ALREADY_EXISTS)) {
                return CommandResult.PARTIAL
            }
        }

        val path = SchematicManager.saveSchematic(schemName, player)
        context.`var`("schematic_name", schemName)
        context.`var`("schematic_file", Kingdoms.getFolder().toAbsolutePath().relativize(path.toAbsolutePath()))
        context.sendMessage(EngineHubLang.COMMAND_ADMIN_SCHEMATIC_SAVE_SAVED)
        CommandAdminSchematic.showTip(context)
        return CommandResult.SUCCESS
    }

    override fun tabComplete(context: CommandTabContext): MutableList<String> {
        return CommandAdminSchematic.tabCompleteSchematics(context)
    }

    companion object {
        @JvmStatic fun hasClipboard(context: CommandContext): CommandResult? {
            val player = context.senderAsPlayer()
            if (!SchematicManager.hasClipboard(player)) {
                return if (SchematicManager.hasSelection(player)) {
                    context.fail(EngineHubLang.COMMAND_ADMIN_SCHEMATIC_SAVE_EMPTY_CLIPBOARD)
                } else {
                    context.fail(EngineHubLang.COMMAND_ADMIN_SCHEMATIC_SAVE_EMPTY_SELECTION)
                }
            }
            return null
        }
    }
}

class CommandAdminSchematicLoad(parent: KingdomsParentCommand) : KingdomsCommand("load", parent) {
    override fun execute(context: CommandContext): CommandResult {
        context.requireArgs(1)
        context.assertPlayer()

        val player = context.senderAsPlayer()
        val schemName = context.arg(0)
        context.`var`("schematic_name", schemName)

        if (!SchematicManager.schematicExists(schemName)) {
            return context.fail(EngineHubLang.COMMAND_ADMIN_SCHEMATIC_LOAD_DOESNT_EXISTS)
        }

        SchematicManager.loadSchematic(schemName, player)
        context.sendMessage(EngineHubLang.COMMAND_ADMIN_SCHEMATIC_LOAD_LOADED)
        CommandAdminSchematic.showTip(context)
        return CommandResult.SUCCESS
    }

    override fun tabComplete(context: CommandTabContext): MutableList<String> {
        return CommandAdminSchematic.tabCompleteSchematics(context)
    }
}

class CommandAdminSchematicList(parent: KingdomsParentCommand) : KingdomsCommand("list", parent) {
    override fun execute(context: CommandContext): CommandResult {
        val entryMap: MutableMap<Int, ContextualMessenger> = hashMapOf()
        val entryInc = AtomicInteger(0)

        if (SchematicManager.getSchematics().isEmpty()) {
            context.sendError(EngineHubLang.COMMAND_ADMIN_SCHEMATIC_LIST_EMPTY)
            return CommandResult.SUCCESS
        }

        val pathBuilder = StringPathBuilder(SchematicManager.getSchematics().values.map {
            val clipboard: Clipboard = it.clipboard

            val blocksSnapshot: String = if (!XWorldEditBukkitAdapterFactory.v6()) {
                val blocksSnapshotIter = ClipboardBlockIterator(
                    BlockVector3.of(0, 0, 0),
                    clipboard,
                    WorldEditBuildingConstruction.SortingStrategy.BOTTOM_TO_TOP
                )
                blocksSnapshotIter.asSequence().filter { b -> !b.block.blockType.material.isAir }.count().toString()
            } else {
                "Not supported in WorldEdit v6"
            }

            return@map Pair(
                it.name,
                Function<String, String> { schemName ->
                    val fileSize = it.storedFile.fileSize()
                    val fileSizeSuffix: String = when (val prefix = SIPrefix.bestFor(fileSize)) {
                        null -> " bytes"
                        else -> prefix.prefixSymbol + "b"
                    }

                    val entryId: Int = entryInc.getAndIncrement()
                    entryMap[entryId] = ContextualMessenger(
                        EngineHubLang.COMMAND_ADMIN_SCHEMATIC_LIST_ENTRY,
                        MessagePlaceholderProvider().withContext(context.getMessageReceiver()).addChild(
                            "schematic", PlaceholderContextBuilder().raws(
                                "name", schemName,
                                "path", it.name,
                                "size", "${Numbers.toFancyNumber(fileSize.toDouble()).toLong()}$fileSizeSuffix",
                                "format",
                                WorldEditSchematicHandler.tryGetStdClipboardFormat(it.storedFile)?.name
                                    ?: KingdomsLang.UNKNOWN,
                                "dimensions", XWorldEditBukkitAdapterFactory.INSTANCE.adapt(clipboard).dimensions,
                                "blocks", blocksSnapshot,
                                "entities", clipboard.entities.size,
                                "has_biomes", if (XWorldEditBukkitAdapterFactory.v6()) false else clipboard.hasBiomes(),
                            )
                        )
                    )

                    return@Function "%entry_$entryId%"
                }
            )
        }.sortedBy { it.key }, null)

        val style = DefaultStringTreeSettings.generateTreeStyle(flatten = true, maxColumns = 10).apply {
            columizeFromLevel = 2
        }
        for (line in TreeBuilder(pathBuilder).parse(style).print().lines) {
            val entryPhContainer = MessagePlaceholderProvider().addChild("entry") { ph -> entryMap[ph.toInt()] }

            context.sendMessage(
                StaticMessenger(
                    MessageProvider(
                        MessageCompiler.compile(line.toString(), DefaultStringTreeSettings.COMPILER_SETTINGS)
                    )
                ),
                entryPhContainer
            )
        }
        return CommandResult.SUCCESS
    }
}