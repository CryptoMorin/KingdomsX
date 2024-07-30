package org.kingdoms.peacetreaties.commands

import org.kingdoms.commands.*
import org.kingdoms.peacetreaties.config.PeaceTreatyLang
import org.kingdoms.peacetreaties.data.PeaceTreaties.Companion.getReceivedPeaceTreaties
import org.kingdoms.peacetreaties.data.PeaceTreaty

class CommandPeaceTreatyReject(parent: KingdomsParentCommand) : KingdomsCommand("reject", parent) {
    override fun execute(context: CommandContext): CommandResult {
        if (context.assertPlayer()) return CommandResult.FAILED
        if (context.assertHasKingdom()) return CommandResult.FAILED
        if (context.requireArgs(1)) return CommandResult.FAILED
        val kingdom = context.kingdom

        if (kingdom.getReceivedPeaceTreaties().isEmpty()) {
            context.sendError(PeaceTreatyLang.COMMAND_PEACETREATY_NO_CONTRACTS)
            return CommandResult.FAILED
        }

        val targetKingdom = context.getKingdom(0) ?: return CommandResult.FAILED
        context.messageContext.withContext(targetKingdom)

        val contract: PeaceTreaty = kingdom.getReceivedPeaceTreaties()[targetKingdom.key] ?: run {
            context.sendError(PeaceTreatyLang.COMMAND_PEACETREATY_NO_CONTRACT_FROM_KINGDOM)
            return CommandResult.FAILED
        }

        if (contract.isAccepted) return context.fail(PeaceTreatyLang.COMMAND_PEACETREATY_ALREADY_ACCEPTED)

        contract.getPlaceholderContextProvider(context.messageContext)
        contract.removeContract()
        val victim = contract.victimKingdom
        val proposer = contract.proposerKingdom

        victim.onlineMembers.forEach { x ->
            context.sendMessage(
                x,
                PeaceTreatyLang.COMMAND_PEACETREATY_REJECT_NOTIFICATIONS_RECEIVER
            )
        }
        proposer.onlineMembers.forEach { x ->
            context.sendMessage(
                x,
                PeaceTreatyLang.COMMAND_PEACETREATY_REJECT_NOTIFICATIONS_SENDER
            )
        }

        return CommandResult.SUCCESS
    }

    override fun tabComplete(context: CommandTabContext): List<String> {
        return CommandPeaceTreaty.tabCompleteKingdomsWithContract(context)
    }
}