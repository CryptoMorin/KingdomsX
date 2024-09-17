package org.kingdoms.peacetreaties.commands

import org.kingdoms.commands.*
import org.kingdoms.managers.buildings.structures.NexusManager
import org.kingdoms.peacetreaties.config.PeaceTreatyLang
import org.kingdoms.peacetreaties.data.PeaceTreaties.Companion.getReceivedPeaceTreaties
import org.kingdoms.peacetreaties.terms.types.MiscUpgradesTerm

class CommandPeaceTreatyMiscUpgrades(parent: KingdomsParentCommand) : KingdomsCommand("miscUpgrades", parent) {
    override fun execute(context: CommandContext): CommandResult {
        if (context.assertPlayer()) return CommandResult.FAILED
        if (context.assertHasKingdom()) return CommandResult.FAILED
        if (context.requireArgs(1)) return CommandResult.FAILED

        val target = context.getKingdom(0) ?: return CommandResult.FAILED
        val kingdom = context.kingdom
        val contract = target.getReceivedPeaceTreaties()[kingdom.key]

        context.messageContext.withContext(target)
        if (contract == null) {
            context.sendError(PeaceTreatyLang.COMMAND_PEACETREATY_NO_CONTRACT_TO_KINGDOM)
            return CommandResult.FAILED
        }

        if (contract.getSubTerm(MiscUpgradesTerm.PROVIDER) == null) {
            context.sendError(PeaceTreatyLang.COMMAND_PEACETREATY_MISCUPGRADES_DOESNT_HAVE_TERM)
            return CommandResult.FAILED
        }

        NexusManager(target, context.senderAsPlayer()).openMiscUpgrades(true)
        return CommandResult.SUCCESS
    }

    override fun tabComplete(context: CommandTabContext): List<String> {
        return CommandPeaceTreaty.tabCompleteKingdomsWithContract(context)
    }
}