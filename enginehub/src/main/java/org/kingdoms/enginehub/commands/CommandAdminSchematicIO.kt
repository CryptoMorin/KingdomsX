package org.kingdoms.enginehub.commands

import org.kingdoms.commands.*
import org.kingdoms.data.Pair
import org.kingdoms.enginehub.EngineHubLang
import org.kingdoms.enginehub.building.WorldEditBuildingConstruction
import org.kingdoms.enginehub.schematic.SchematicManager
import org.kingdoms.enginehub.schematic.WorldEditSchematicHandler
import org.kingdoms.enginehub.schematic.blocks.ClipboardBlockIterator
import org.kingdoms.locale.KingdomsLang
import org.kingdoms.locale.compiler.MessageCompiler
import org.kingdoms.locale.messenger.ContextualMessenger
import org.kingdoms.locale.messenger.StaticMessenger
import org.kingdoms.locale.placeholders.context.MessagePlaceholderProvider
import org.kingdoms.locale.placeholders.context.PlaceholderContextBuilder
import org.kingdoms.locale.provider.MessageProvider
import org.kingdoms.main.Kingdoms
import org.kingdoms.server.location.BlockVector3
import org.kingdoms.utils.SIPrefix
import org.kingdoms.utils.internal.numbers.Numbers
import org.kingdoms.utils.string.tree.DefaultStringTreeSettings
import org.kingdoms.utils.string.tree.StringPathBuilder
import org.kingdoms.utils.string.tree.TreeBuilder
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Function
import kotlin.io.path.fileSize

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
            val clipboard = it.clipboard
            val blocksSnapshotIter = ClipboardBlockIterator(
                BlockVector3.of(0, 0, 0),
                clipboard,
                WorldEditBuildingConstruction.SortingStrategy.BOTTOM_TO_TOP
            )
            val blocksSnapshot: Int =
                blocksSnapshotIter.asSequence().filter { b -> !b.block.blockType.material.isAir }.count()

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
                                "dimensions", clipboard.dimensions,
                                "blocks", blocksSnapshot,
                                "entities", clipboard.entities.size,
                                "has_biomes", clipboard.hasBiomes(),
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