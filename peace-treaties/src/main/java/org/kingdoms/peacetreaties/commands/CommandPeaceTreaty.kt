package org.kingdoms.peacetreaties.commands

import org.kingdoms.commands.CommandTabContext
import org.kingdoms.commands.KingdomsParentCommand
import org.kingdoms.peacetreaties.data.PeaceTreaties.Companion.getProposedPeaceTreaties

class CommandPeaceTreaty : KingdomsParentCommand("peacetreaty", true) {
    init {
        CommandPeaceTreatyResume(this)
        CommandPeaceTreatyAccept(this)
        CommandPeaceTreatyReview(this)
        CommandPeaceTreatyReject(this)
        CommandPeaceTreatyMiscUpgrades(this)
    }

    companion object {
        @JvmStatic
        fun tabCompleteKingdomsWithContract(context: CommandTabContext): List<String> {
            if (context.isPlayer() && context.isAtArg(0)) {
                val kingdom = context.kingdom ?: return emptyList()
                return context.suggest(0, kingdom.getProposedPeaceTreaties().values.map { x -> x.victimKingdom.name })
            }

            return emptyList()
        }
    }
}