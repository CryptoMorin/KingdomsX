package org.kingdoms.enginehub.commands

import org.kingdoms.commands.CommandContext
import org.kingdoms.commands.CommandTabContext
import org.kingdoms.commands.KingdomsParentCommand
import org.kingdoms.enginehub.EngineHubLang
import org.kingdoms.enginehub.schematic.SchematicManager
import org.kingdoms.main.Kingdoms
import java.time.Duration
import java.util.*

class CommandAdminSchematic(parent: KingdomsParentCommand) : KingdomsParentCommand("schematic", parent) {
    init {
        CommandAdminSchematicLoad(this)
        CommandAdminSchematicSave(this)
        CommandAdminSchematicList(this)
        CommandAdminSchematicCopy()

        CommandAdminSchematicOrigin(this)
        CommandAdminSchematicConvertAll(this)
        CommandAdminSchematicSetup(this)
    }

    companion object {
        @JvmField val RECEIVED_TIPS: MutableSet<UUID> = hashSetOf()

        @JvmStatic
        fun showTip(context: CommandContext) {
            val player = context.senderAsPlayer()
            if (RECEIVED_TIPS.add(player.uniqueId)) {
                Kingdoms.taskScheduler().sync().delayed(Duration.ofSeconds(15)) { ->
                    context.sendMessage(EngineHubLang.COMMAND_ADMIN_SCHEMATIC_TIPS_SIDE_EFFECTS)
                }
            }
        }

        @JvmStatic
        fun tabCompleteSchematics(context: CommandTabContext): MutableList<String> {
            if (context.isAtArg(0)) {
                return context.suggest(0, SchematicManager.getSchematics().keys)
            }
            return context.emptyTab()
        }
    }
}