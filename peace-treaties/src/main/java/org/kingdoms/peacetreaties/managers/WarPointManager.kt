package org.kingdoms.peacetreaties.managers

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.kingdoms.constants.group.model.relationships.KingdomRelation
import org.kingdoms.constants.land.structures.objects.SiegeCannon
import org.kingdoms.constants.land.turrets.Turret
import org.kingdoms.constants.player.KingdomPlayer
import org.kingdoms.events.invasion.KingdomInvadeEvent
import org.kingdoms.events.items.KingdomItemBreakEvent
import org.kingdoms.locale.compiler.placeholders.PlaceholderContextBuilder
import org.kingdoms.main.KLogger
import org.kingdoms.managers.PvPManager
import org.kingdoms.peacetreaties.config.PeaceTreatyConfig
import org.kingdoms.peacetreaties.data.WarPoint.Companion.addWarPoints
import org.kingdoms.utils.MathUtils
import org.kingdoms.utils.debugging.DebugNS
import org.kingdoms.utils.debugging.KingdomsDebug

class WarPointManager : Listener {
    companion object {
        @JvmField
        val DEBUG_NS: DebugNS = KingdomsDebug.register("PEACE_TREATIES/WAR_POINTS")
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onInvade(event: KingdomInvadeEvent) {
        val invasion = event.getInvasion()
        if (!invasion.result.isSuccessful) return

        val attacker = invasion.attacker
        val defender = invasion.defender
        if (attacker.getRelationWith(defender) != KingdomRelation.ENEMY) return

        val amount = MathUtils.eval(
            PeaceTreatyConfig.WAR_POINTS_SCORES_INVADE.manager.mathExpression,
            PlaceholderContextBuilder().withContext(attacker).other(defender)
        )
        attacker.addWarPoints(defender, amount)

        KLogger.debug(DEBUG_NS) { "Added $amount war points to ${attacker.name} kingdom: invaded ${defender.name}" }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onKill(event: PlayerDeathEvent) {
        val dead = event.entity
        val killer = PvPManager.getKiller(event) as? Player ?: return

        val deadKp = KingdomPlayer.getKingdomPlayer(dead)
        val killerKp = KingdomPlayer.getKingdomPlayer(killer)

        val deadKingdom = deadKp.kingdom ?: return
        val killerKingdom = killerKp.kingdom ?: return

        if (deadKingdom.getRelationWith(killerKingdom) != KingdomRelation.ENEMY) return

        val amount = MathUtils.eval(
            PeaceTreatyConfig.WAR_POINTS_SCORES_KILL.manager.mathExpression,
            PlaceholderContextBuilder().withContext(killer).other(dead)
        )
        killerKingdom.addWarPoints(killerKingdom, amount)

        KLogger.debug(DEBUG_NS) { "Added $amount war points to ${killerKingdom.name} kingdom: ${killer.name} killed ${dead.name}" }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onKingdomItemBreak(event: KingdomItemBreakEvent<*>) {
        if (event.getKingdomItem() !is Turret) return
        event.getMetadata<SiegeCannon>(SiegeCannon.NS) ?: return
        val turret = event.getKingdomItem()!!

        val itemKingdom = turret.getLand()!!.kingdom!!
        val player = event.getPlayer()!!
        val playerKingdom = player.kingdom!!

        if (itemKingdom.getRelationWith(playerKingdom) != KingdomRelation.ENEMY) return

        val amount = MathUtils.eval(
            PeaceTreatyConfig.WAR_POINTS_SCORES_KILL.manager.mathExpression,
            PlaceholderContextBuilder().withContext(player.offlinePlayer).other(itemKingdom)
        )
        playerKingdom.addWarPoints(itemKingdom, amount)

        KLogger.debug(DEBUG_NS) {
            "Added $amount war points to ${playerKingdom.name} kingdom because ${player.offlinePlayer.name} destroyed a " +
                    "${turret.style.name} turret of ${itemKingdom.name} kingdom."
        }
    }
}