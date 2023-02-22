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
import org.kingdoms.locale.compiler.placeholders.PlaceholderContextBuilder
import org.kingdoms.locale.provider.MessageBuilder
import org.kingdoms.peacetreaties.PeaceTreatiesAddon
import org.kingdoms.peacetreaties.config.PeaceTreatyConfig
import org.kingdoms.peacetreaties.config.PeaceTreatyLang
import org.kingdoms.peacetreaties.data.PeaceTreaties.Companion.getReceivedPeaceTreaties
import org.kingdoms.peacetreaties.data.PeaceTreaty
import org.kingdoms.utils.KingdomsBukkitExtensions.getKingdom
import java.time.Duration

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
                .forEach { x -> PeaceTreatyLang.NOTIFICATION_RECEIVERS.sendMessage(player, x.getPlaceholderContextProvider(MessageBuilder()) as MessageBuilder) }
        }, 20L)
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun closeTreatyEditorOnDisband(event: KingdomDisbandEvent) {
        StandardPeaceTreatyEditor.kingdomNotAvailable(event.kingdom)
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun closeTreatyEditorOnKick(event: KingdomLeaveEvent) {
        StandardPeaceTreatyEditor.outOfKingdom(event.getPlayer())
    }

    @EventHandler(ignoreCancelled = true)
    fun onNeutralRelationshipRequest(event: GroupRelationshipRequestEvent) {
        if (event.relationship != KingdomRelation.NEUTRAL) return
        if (event.from !is Kingdom) return // Both checks for kotlin to leave me alone

        val from = event.from as Kingdom
        val to = event.to as Kingdom

        if (from.getRelationWith(to) != KingdomRelation.ENEMY) return
        event.isCancelled = true

        val player = event.getPlayer() ?: return
        if (StandardPeaceTreatyEditor.hasPendingContract(player.id)) {
            PeaceTreatyLang.EDITOR_UNFINISHED.sendError(player.player)
            return
        }

        if (to.getReceivedPeaceTreaties().containsKey(from.id)) {
            PeaceTreatyLang.COMMAND_REVOKE_PEACETREATY_ALREADY_SENT.sendError(player.player)
            return
        }
        if (from.getReceivedPeaceTreaties().containsKey(to.id)) {
            PeaceTreatyLang.COMMAND_REVOKE_PEACETREATY_ALREADY_RECEIVED.sendError(player.player)
            return
        }

        val settings = PlaceholderContextBuilder().withContext(from).other(to)
        val durationMillis = PeaceTreatyConfig.DURATION.manager.getTime(settings)
        val contract = PeaceTreaty(from.id, to.id, 0, System.currentTimeMillis(), Duration.ofMillis(durationMillis), player.id)
        StandardPeaceTreatyEditor.fromContract(contract).open()
    }
}