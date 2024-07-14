package org.kingdoms.enginehub.commands

import org.kingdoms.commands.*
import org.kingdoms.config.HoverMessage
import org.kingdoms.data.Pair
import org.kingdoms.enginehub.EngineHubLang
import org.kingdoms.enginehub.building.WorldEditBuildingConstruction
import org.kingdoms.enginehub.schematic.SchematicManager
import org.kingdoms.enginehub.schematic.blocks.ClipboardBlockIterator
import org.kingdoms.locale.compiler.MessageCompiler
import org.kingdoms.locale.messenger.StaticMessenger
import org.kingdoms.locale.provider.MessageProvider
import org.kingdoms.main.Kingdoms
import org.kingdoms.server.location.BlockVector3
import org.kingdoms.utils.string.tree.DefaultStringTreeSettings
import org.kingdoms.utils.string.tree.StringPathBuilder
import org.kingdoms.utils.string.tree.TreeBuilder
import java.util.function.Function

class CommandAdminSchematicSave(parent: KingdomsParentCommand) : KingdomsCommand("save", parent) {
    override fun execute(context: CommandContext): CommandResult {
        CommandAdminSchematic.handleBasics(context).let { if (it != CommandResult.SUCCESS) return it }
        val player = context.senderAsPlayer()
        val schemName = context.arg(0)

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
        CommandAdminSchematic.handleBasics(context).let { if (it != CommandResult.SUCCESS) return it }
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
        val pathBuilder = StringPathBuilder(SchematicManager.getSchematics().values.map {
            val clipboard = it.clipboard
            val blocksSnapshotIter = ClipboardBlockIterator(
                BlockVector3.of(0, 0, 0),
                clipboard,
                WorldEditBuildingConstruction.SortingStrategy.BOTTOM_TO_TOP
            )
            var blocksSnapshot: Int =
                blocksSnapshotIter.asSequence().filter { b -> !b.block.blockType.material.isAir }.count()
            Pair(
                it.name,
                Function<String, String> { x ->
                    HoverMessage(
                        x, "&7Click to load\n\n" +
                                "&2Dimensions&8: &6${clipboard.dimensions}\n" +
                                "&2Blocks&8: &6${blocksSnapshot}\n" +
                                "&2Entities&8: &6${clipboard.entities.size}\n" +
                                "&2Biomes&8: &6${clipboard.hasBiomes()}",
                        "/k admin schematic load ${it.name}"
                    ).toString()
                }
            )
        }.sortedBy { it.key }, null)
        val style = DefaultStringTreeSettings.generateTreeStyle(flatten = true, maxColumns = 10)
        for (line in TreeBuilder(pathBuilder).parse(style).print().lines) {
            context.sendMessage(
                StaticMessenger(
                    MessageProvider(
                        MessageCompiler.compile(line.toString(), DefaultStringTreeSettings.COMPILER_SETTINGS)
                    )
                )
            )
        }
        return CommandResult.SUCCESS
    }
}