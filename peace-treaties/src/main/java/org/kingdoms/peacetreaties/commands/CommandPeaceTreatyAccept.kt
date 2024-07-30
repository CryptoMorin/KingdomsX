package org.kingdoms.peacetreaties.commands

import org.kingdoms.commands.*
import org.kingdoms.locale.messenger.Messenger
import org.kingdoms.peacetreaties.config.PeaceTreatyLang
import org.kingdoms.peacetreaties.data.PeaceTreaties.Companion.getReceivedPeaceTreaties
import org.kingdoms.peacetreaties.data.PeaceTreaty

class CommandPeaceTreatyAccept(parent: KingdomsParentCommand) : KingdomsCommand("accept", parent) {
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

        val errors = arrayListOf<Messenger>()
        for (termGrouping in contract.terms.values) {
            for (term in termGrouping.terms.values) {
                term.addEdits(context.messageContext)
                val canApply = term.canAccept(termGrouping.options, contract)
                if (canApply != null) errors.add(canApply)
            }
        }

        contract.getPlaceholderContextProvider(context.messageContext)
        return if (errors.isEmpty()) {
            if (contract.accept().isCancelled) return CommandResult.FAILED

            val victim = contract.victimKingdom
            val proposer = contract.proposerKingdom

            victim.onlineMembers.forEach { x ->
                context.sendMessage(
                    x,
                    PeaceTreatyLang.COMMAND_PEACETREATY_ACCEPT_ACCEPTED_RECEIVER
                )
            }
            proposer.onlineMembers.forEach { x ->
                context.sendMessage(
                    x,
                    PeaceTreatyLang.COMMAND_PEACETREATY_ACCEPT_ACCEPTED_SENDER
                )
            }

            CommandResult.SUCCESS
        } else {
            context.sendError(PeaceTreatyLang.COMMAND_PEACETREATY_ACCEPT_FAILED)
            for (error in errors) context.sendError(error)
            CommandResult.FAILED
        }
    }

    override fun tabComplete(context: CommandTabContext): List<String> {
        return CommandPeaceTreaty.tabCompleteKingdomsWithContract(context)
    }
}