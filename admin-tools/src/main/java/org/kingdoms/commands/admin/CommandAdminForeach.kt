package org.kingdoms.commands.admin

import org.bukkit.permissions.PermissionDefault
import org.kingdoms.admintools.AdminToolsLang
import org.kingdoms.commands.CommandContext
import org.kingdoms.commands.CommandResult
import org.kingdoms.commands.KingdomsCommand
import org.kingdoms.commands.KingdomsParentCommand
import org.kingdoms.commands.annotations.Cmd
import org.kingdoms.commands.annotations.CmdParent
import org.kingdoms.commands.annotations.CmdPerm
import org.kingdoms.constants.group.Kingdom
import org.kingdoms.constants.group.Nation
import org.kingdoms.locale.KingdomsLang
import org.kingdoms.locale.placeholders.context.MessagePlaceholderProvider
import org.kingdoms.main.Kingdoms
import org.kingdoms.utils.commands.ConfigCommand
import org.kingdoms.utils.compilers.ConditionalCompiler
import org.kingdoms.utils.conditions.ConditionProcessor

@Cmd("forEach")
@CmdParent(CommandAdmin::class)
@CmdPerm(PermissionDefault.OP)
class CommandAdminForeach : KingdomsCommand() {
    override fun execute(context: CommandContext): CommandResult {
        context.requireArgs(3)

        try {
            val contextType = context.required("contextType")
            val commandStr = context.required("command")
            val conditionStr = context.optional("condition")

            val condition =
                if (conditionStr.isNullOrEmpty()) null
                else ConditionalCompiler.compile(conditionStr).evaluate()

            val command = ConfigCommand.parse(arrayListOf(commandStr))

            when (contextType) {
                "allKingdoms" -> {
                    Kingdoms.get().dataCenter.kingdomManager.peekAllData().forEach { kingdom ->
                        val settings = MessagePlaceholderProvider().withContext(kingdom)
                        if (condition == null || ConditionProcessor.process(condition, settings)) {
                            ConfigCommand.execute(null, command, settings, false)
                        }
                    }
                }

                "allNations" -> {
                    Kingdoms.get().dataCenter.nationManager.peekAllData().forEach { nation ->
                        val settings = MessagePlaceholderProvider().withContext(nation)
                        if (condition == null || ConditionProcessor.process(condition, settings)) {
                            ConfigCommand.execute(null, command, settings, false)
                        }
                    }
                }

                "kingdomsInNation" -> {
                    val contextStr = context.required("context")
                    val nation = Nation.getNation(contextStr)
                    context.`var`("nation", contextStr)
                    if (nation == null) return context.fail(KingdomsLang.NOT_FOUND_NATION)

                    for (kingdom in nation.kingdoms) {
                        val settings = MessagePlaceholderProvider().withContext(kingdom)
                        if (condition == null || ConditionProcessor.process(condition, settings)) {
                            ConfigCommand.execute(null, command, settings, false)
                        }
                    }
                }

                "playersInKingdom" -> {
                    val contextStr = context.required("context")
                    val kingdom = Kingdom.getKingdom(contextStr)
                    context.`var`("kingdom", contextStr)
                    if (kingdom == null) return context.fail(KingdomsLang.NOT_FOUND_KINGDOM)

                    for (member in kingdom.playerMembers) {
                        val settings = MessagePlaceholderProvider().withContext(member)
                        if (condition == null || ConditionProcessor.process(condition, settings)) {
                            ConfigCommand.execute(member.player, command, settings, true)
                        }
                    }
                }

                else -> return context.fail(AdminToolsLang.COMMAND_ADMIN_FOREACH_UNKNOWN_CONTEXT_TYPE)
            }
        } catch (ex: Throwable) {
            throw RuntimeException("Error while running command: '${context.args}' ", ex)
        }

        return CommandResult.SUCCESS
    }
}