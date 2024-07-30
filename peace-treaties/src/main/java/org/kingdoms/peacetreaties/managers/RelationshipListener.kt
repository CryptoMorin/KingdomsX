package org.kingdoms.peacetreaties.managers

import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.kingdoms.constants.group.Kingdom
import org.kingdoms.constants.group.model.relationships.KingdomRelation
import org.kingdoms.events.general.GroupRelationshipChangeEvent
import org.kingdoms.events.general.GroupRelationshipRequestEvent
import org.kingdoms.events.general.KingdomDisbandEvent
import org.kingdoms.events.members.KingdomLeaveEvent
import org.kingdoms.locale.placeholders.context.MessagePlaceholderProvider
import org.kingdoms.locale.placeholders.context.PlaceholderContextBuilder
import org.kingdoms.peacetreaties.PeaceTreatiesAddon
import org.kingdoms.peacetreaties.config.PeaceTreatyConfig
import org.kingdoms.peacetreaties.config.PeaceTreatyLang
import org.kingdoms.peacetreaties.data.PeaceTreaties.Companion.getContractWith
import org.kingdoms.peacetreaties.data.PeaceTreaties.Companion.getProposedPeaceTreaties
import org.kingdoms.peacetreaties.data.PeaceTreaties.Companion.getReceivedPeaceTreaties
import org.kingdoms.peacetreaties.data.PeaceTreaty
import org.kingdoms.peacetreaties.data.WarPoint.Companion.getWarPoints
import org.kingdoms.peacetreaties.terms.types.AnnulTreatiesTerm
import org.kingdoms.utils.KingdomsBukkitExtensions.asKingdom
import org.kingdoms.utils.KingdomsBukkitExtensions.getKingdom

class RelationshipListener : Listener {
    companion object {
        @JvmField
        val RELATION_CHANGE_NS = org.kingdoms.constants.namespace.Namespace("PeaceTreaties", "UNDER_CONTRACT")

        @JvmStatic
        fun GroupRelationshipChangeEvent.getPeaceTreaty(): PeaceTreaty? =
            this.getMetadata(RELATION_CHANGE_NS)
    }

    @EventHandler
    fun onJoinNotify(event: PlayerJoinEvent) {
        val player = event.player
        val kingdom = player.getKingdom() ?: return

        Bukkit.getScheduler().runTaskLater(PeaceTreatiesAddon.get(), { ->
            kingdom.getReceivedPeaceTreaties().values
                .filter { x -> !x.isAccepted }
                .forEach { x ->
                    PeaceTreatyLang.NOTIFICATION_RECEIVERS.sendMessage(
                        player,
                        x.getPlaceholderContextProvider(MessagePlaceholderProvider()) as MessagePlaceholderProvider
                    )
                }
        }, 20L)
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onDisband(event: KingdomDisbandEvent) {
        val kingdom = event.kingdom

        // Close treaty editor
        StandardPeaceTreatyEditor.kingdomNotAvailable(kingdom)

        // Remove peace treaty data
        kingdom.getReceivedPeaceTreaties().values.forEach { x -> x.revoke() }
        kingdom.getProposedPeaceTreaties().values.forEach { x -> x.revoke() }

        // Remove war points data
        kingdom.getWarPoints().keys.forEach { x -> x.asKingdom()?.getWarPoints()?.remove(kingdom.id) }
        kingdom.getWarPoints().clear()
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun closeTreatyEditorOnKick(event: KingdomLeaveEvent) {
        StandardPeaceTreatyEditor.outOfKingdom(event.getPlayer())
    }

    @EventHandler(ignoreCancelled = true)
    fun onRelationshipChange(event: GroupRelationshipChangeEvent) {
        if (event.first !is Kingdom) return

        val player = event.getPlayer()?.player
        val from = event.first as Kingdom
        val to = event.second as Kingdom

        if (event.newRelation == KingdomRelation.ENEMY) {
            val contract = from.getContractWith(to)
            if (contract != null) {
                PeaceTreatyLang.UNDER_CONTRACT_ENEMIES.sendError(
                    player,
                    contract.placeholderContextProvider
                )
                event.isCancelled = true
                return
            }
        }

        val fromContract = isUnderAnnulTreaties(from)
        if (fromContract != null) {
            if (player != null) PeaceTreatyLang.UNDER_CONTRACT_ANNUL_TREATIES_FROM.sendError(
                player,
                fromContract.placeholderContextProvider
            )
            event.isCancelled = true
            return
        }

        val toContract = isUnderAnnulTreaties(to)
        if (toContract != null) {
            if (player != null) PeaceTreatyLang.UNDER_CONTRACT_ANNUL_TREATIES_TO.sendError(
                player,
                toContract.placeholderContextProvider
            )
            event.isCancelled = true
            return
        }
    }

    fun isUnderAnnulTreaties(kingdom: Kingdom): PeaceTreaty? {
        for ((_, contract) in kingdom.getReceivedPeaceTreaties()) {
            for ((_, termGrouping) in contract.terms) {
                for ((_, term) in termGrouping.terms) {
                    if (term is AnnulTreatiesTerm) {
                        return contract
                    }
                }
            }
        }
        return null
    }

    @EventHandler(ignoreCancelled = true)
    fun onRelationshipRequest(event: GroupRelationshipRequestEvent) {
        if (event.from !is Kingdom) return // Both checks for kotlin to leave me alone

        val from = event.from as Kingdom
        val to = event.to as Kingdom
        val player = event.getPlayer() ?: return

        // Check if a kingdom is under a contract for this relationship request
        if (to.getReceivedPeaceTreaties().containsKey(from.key)) {
            PeaceTreatyLang.UNDER_CONTRACT_TO.sendError(player.player)
            event.isCancelled = true
            return
        }
        if (from.getReceivedPeaceTreaties().containsKey(to.key)) {
            PeaceTreatyLang.UNDER_CONTRACT_FROM.sendError(player.player)
            event.isCancelled = true
            return
        }

        if (event.relationship != KingdomRelation.NEUTRAL) return
        if (from.getRelationWith(to) != KingdomRelation.ENEMY) return
        event.isCancelled = true

        if (StandardPeaceTreatyEditor.hasPendingContract(player.id)) {
            PeaceTreatyLang.EDITOR_UNFINISHED.sendError(player.player)
            return
        }

        if (to.getReceivedPeaceTreaties().containsKey(from.key)) {
            PeaceTreatyLang.COMMAND_REVOKE_PEACETREATY_ALREADY_SENT.sendError(player.player)
            return
        }
        if (from.getReceivedPeaceTreaties().containsKey(to.key)) {
            PeaceTreatyLang.COMMAND_REVOKE_PEACETREATY_ALREADY_RECEIVED.sendError(player.player)
            return
        }

        val settings = PlaceholderContextBuilder().withContext(from).other(to)
        val durationMillis = PeaceTreatyConfig.DURATION.manager.getTime(settings)
        val contract = PeaceTreaty(
            from.key,
            to.key,
            0,
            System.currentTimeMillis(),
            durationMillis,
            player.id
        )
        StandardPeaceTreatyEditor.fromContract(contract).open()
    }
}