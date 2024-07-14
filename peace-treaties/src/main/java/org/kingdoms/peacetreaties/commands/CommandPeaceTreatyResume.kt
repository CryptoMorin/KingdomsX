package org.kingdoms.peacetreaties.commands

import org.kingdoms.commands.*
import org.kingdoms.peacetreaties.config.PeaceTreatyLang
import org.kingdoms.peacetreaties.managers.StandardPeaceTreatyEditor

class CommandPeaceTreatyResume(parent: KingdomsParentCommand) : KingdomsCommand("resume", parent) {
    override fun execute(context: CommandContext): CommandResult {
        if (context.assertPlayer()) return CommandResult.FAILED
        if (context.assertHasKingdom()) return CommandResult.FAILED

        val resumed = StandardPeaceTreatyEditor.resume(context.senderAsPlayer())
        if (resumed == null) {
            context.sendError(PeaceTreatyLang.COMMAND_PEACETREATY_RESUME_NONE)
            return CommandResult.FAILED
        }

        return CommandResult.SUCCESS
    }

    override fun tabComplete(context: CommandTabContext): List<String> {
        return CommandPeaceTreaty.tabCompleteKingdomsWithContract(context)
    }
}