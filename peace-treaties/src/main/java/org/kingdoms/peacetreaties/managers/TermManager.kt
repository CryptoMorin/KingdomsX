package org.kingdoms.peacetreaties.managers

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.kingdoms.constants.group.Kingdom
import org.kingdoms.constants.group.model.relationships.KingdomRelation
import org.kingdoms.constants.group.upgradable.MiscUpgrade
import org.kingdoms.constants.land.structures.Structure
import org.kingdoms.constants.land.turrets.Turret
import org.kingdoms.events.general.GroupRelationshipChangeEvent
import org.kingdoms.events.general.upgrade.GroupUpgradeToggleEvent
import org.kingdoms.events.items.KingdomItemPlaceEvent
import org.kingdoms.events.lands.ClaimLandEvent
import org.kingdoms.events.members.NationJoinEvent
import org.kingdoms.peacetreaties.config.PeaceTreatyLang
import org.kingdoms.peacetreaties.data.PeaceTreaties.Companion.getAcceptedPeaceTreaty
import org.kingdoms.peacetreaties.data.PeaceTreaties.Companion.getProposedPeaceTreaties
import org.kingdoms.peacetreaties.terms.types.AnnulTreatiesTerm
import org.kingdoms.peacetreaties.terms.types.LeaveDisbandNationTerm
import org.kingdoms.peacetreaties.terms.types.LimitClaimsTerm
import org.kingdoms.peacetreaties.terms.types.LimitTurretsTerm

/**
 * Handles long-term terms which affects the kingdom until the end of the accepted peace treaty contract.
 */
class TermManager : Listener {
    @EventHandler(ignoreCancelled = true)
    fun onNationJoin(event: NationJoinEvent) {
        val kingdom = event.kingdom
        val accepted = kingdom.getAcceptedPeaceTreaty() ?: return
        if (accepted.getSubTerm(LeaveDisbandNationTerm.PROVIDER) == null) return

        event.isCancelled = true
        val onlinePlayer = event.getPlayer()?.player ?: return
        PeaceTreatyLang.TERMS_LEAVE_NATION_CANT_JOIN.sendMessage(onlinePlayer)
    }

    fun shouldHandleEvent(event: GroupRelationshipChangeEvent): Boolean {
        if (event.newRelation != KingdomRelation.ALLY && event.newRelation != KingdomRelation.TRUCE) return false
        return event.first is Kingdom && event.second is Kingdom
    }

    @EventHandler(ignoreCancelled = true)
    fun onRelationSync(event: GroupRelationshipChangeEvent) {
        if (!shouldHandleEvent(event)) return
        val first = event.first as Kingdom
        val second = event.second as Kingdom

        if (!isUnderRelationContract(first, event)) return
        isUnderRelationContract(second, event)
    }

    @EventHandler(ignoreCancelled = true)
    fun onClaim(event: ClaimLandEvent) {
        val kingdom = event.kingdom
        val contract = kingdom.getAcceptedPeaceTreaty() ?: return
        val term = contract.getSubTerm(LimitClaimsTerm.PROVIDER) ?: return
        val maxClaims = (term as LimitClaimsTerm).maxClaims

        if (kingdom.landLocations.size + event.landLocations.size > maxClaims) {
            event.isCancelled = true
            event.getPlayer()?.player?.let { PeaceTreatyLang.TERMS_MAX_CLAIMS_LIMITED.sendMessage(it, term.edits) }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onKingdomItemPlace(event: KingdomItemPlaceEvent<*>) {
        val kingdom = event.getKingdomItem().getLand()?.kingdom ?: return
        val contract = kingdom.getAcceptedPeaceTreaty() ?: return

        if (event.getKingdomItem() is Turret) {
            val term = contract.getSubTerm(LimitTurretsTerm.PROVIDER) as? LimitTurretsTerm
                ?: return
            val settings = term.edits
            val turrets = kingdom.lands.sumOf { x -> x.turrets.size }
            if (turrets + 1 > term.maxTurrets) {
                event.isCancelled = true
                event.getPlayer()?.player?.let { PeaceTreatyLang.TERMS_MAX_TURRETS_LIMITED.sendMessage(it, settings) }
            }
        } else if (event.getKingdomItem() is Structure) {
            val term = contract.getSubTerm(LimitTurretsTerm.PROVIDER) as? LimitTurretsTerm
                ?: return
            val settings = term.edits
            val turrets = kingdom.lands.sumOf { x -> x.turrets.size }
            if (turrets + 1 > term.maxTurrets) {
                event.isCancelled = true
                event.getPlayer()?.player?.let {
                    PeaceTreatyLang.TERMS_MAX_STRUCTURES_LIMITED.sendMessage(
                        it,
                        settings
                    )
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onRelationSyncSecond(event: GroupRelationshipChangeEvent) {
        if (!shouldHandleEvent(event)) return
        val first = event.first as Kingdom
        val second = event.second as Kingdom
        val rel = event.newRelation

        syncRelation(first, second, rel)
        syncRelation(second, first, rel)
    }

    @EventHandler(ignoreCancelled = true)
    fun upgradeToggle(event: GroupUpgradeToggleEvent) {
        if (event.upgrade !is MiscUpgrade) return

        val kingdom = event.getGroup() as? Kingdom ?: return
        val contract = kingdom.getAcceptedPeaceTreaty() ?: return

        contract.getSubTerm(AnnulTreatiesTerm.PROVIDER) ?: return

        val player = event.getPlayer()
        if (player != null) {
            if (contract.proposerKingdom.isMember(player)) return
            player.player?.let { PeaceTreatyLang.TERMS_MISC_UPGRADES_RESTRICTIED.sendMessage(it); }
        }
        event.isCancelled = true
    }

    fun isUnderRelationContract(kingdom: Kingdom, event: GroupRelationshipChangeEvent): Boolean {
        val contract = kingdom.getAcceptedPeaceTreaty()
        if (contract?.getSubTerm(AnnulTreatiesTerm.PROVIDER) != null) {
            event.isCancelled = true
            event.getPlayer()?.player?.let { PeaceTreatyLang.TERMS_MAX_STRUCTURES_RESTRICTED.sendMessage(it); }
            return true
        }
        return false
    }

    fun syncRelation(kingdom: Kingdom, otherKingdom: Kingdom, relation: KingdomRelation) {
        val accepted = kingdom.getProposedPeaceTreaties().values.filter { x -> x.isAccepted }
        for (contract in accepted) {
            val victim = contract.victimKingdom
            victim.relations[otherKingdom.key] = relation
            victim.onlineMembers.forEach { PeaceTreatyLang.TERMS_MAX_STRUCTURES_SYNCHRONIZED.sendMessage(it); }
        }
    }
}