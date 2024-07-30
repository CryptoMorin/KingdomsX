package org.kingdoms.peacetreaties.managers

import org.kingdoms.constants.group.Kingdom
import org.kingdoms.managers.fsck.HealthCheckupHandler
import org.kingdoms.peacetreaties.config.PeaceTreatyLang
import org.kingdoms.peacetreaties.data.PeaceTreaties.Companion.getProposedPeaceTreaties
import org.kingdoms.peacetreaties.data.PeaceTreaties.Companion.getReceivedPeaceTreaties
import org.kingdoms.peacetreaties.data.PeaceTreaty
import org.kingdoms.utils.KingdomsBukkitExtensions.asKingdom

class PeaceTreatyFSCK : HealthCheckupHandler() {
    init {
        addPredefinedHandlers(Kingdom::class.java) { kingdom, processor ->
            for (contract in kingdom.getReceivedPeaceTreaties().values.toTypedArray()) {
                val proposer = contract.proposerKingdomId.asKingdom()
                if (proposer == null) {
                    processor.addEntry(PeaceTreatyLang.COMMAND_ADMIN_FSCK_PEACETREATIES_KINGDOM_UNKNOWN) {
                        raw("victim", kingdom.id)
                        raw("proposer", contract.proposerKingdomId)
                    }
                    if (processor.fix) contract.revoke()
                }
            }

            for (contract in kingdom.getProposedPeaceTreaties().values.toTypedArray()) {
                val victim = contract.victimKingdomId.asKingdom()
                if (victim == null) {
                    processor.addEntry(PeaceTreatyLang.COMMAND_ADMIN_FSCK_PEACETREATIES_KINGDOM_UNKNOWN) {
                        raw("victim", contract.victimKingdomId)
                        raw("proposer", kingdom.id)
                    }
                    if (processor.fix) contract.revoke()
                }
            }
        }
    }

    fun removeUnknownTerms(contract: PeaceTreaty) {
    }
}