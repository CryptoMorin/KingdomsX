package org.kingdoms.peacetreaties.managers

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.kingdoms.constants.group.model.relationships.KingdomRelation
import org.kingdoms.constants.land.structures.objects.SiegeCannon
import org.kingdoms.constants.land.turrets.Turret
import org.kingdoms.events.invasion.KingdomInvadeEvent
import org.kingdoms.events.items.KingdomItemBreakEvent
import org.kingdoms.locale.compiler.placeholders.PlaceholderContextBuilder
import org.kingdoms.main.KLogger
import org.kingdoms.managers.PvPManager
import org.kingdoms.peacetreaties.config.PeaceTreatyConfig
import org.kingdoms.peacetreaties.data.WarPoint.Companion.addWarPoints
import org.kingdoms.utils.KingdomsBukkitExtensions.asKingdomPlayer
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

        val ctx = PlaceholderContextBuilder().withContext(attacker).other(defender)
        val gained = MathUtils.eval(PeaceTreatyConfig.WAR_POINTS_SCORES_GAIN_INVADE.manager.mathExpression, ctx)
        val lost = MathUtils.eval(PeaceTreatyConfig.WAR_POINTS_SCORES_LOSE_INVADE.manager.mathExpression, ctx)

        attacker.addWarPoints(defender, gained)
        defender.addWarPoints(attacker, -lost)

        KLogger.debug(DEBUG_NS) { "Added $gained war points to ${attacker.name} kingdom: invaded ${defender.name}" }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onKill(event: PlayerDeathEvent) {
        val dead = event.entity
        val killer = PvPManager.getKiller(event) as? Player ?: return

        val deadKp = dead.asKingdomPlayer()
        val killerKp = killer.asKingdomPlayer()

        val deadKingdom = deadKp.kingdom ?: return
        val killerKingdom = killerKp.kingdom ?: return

        if (deadKingdom.getRelationWith(killerKingdom) != KingdomRelation.ENEMY) return

        val ctx = PlaceholderContextBuilder().withContext(killer).other(dead)
        val gained = MathUtils.eval(PeaceTreatyConfig.WAR_POINTS_SCORES_GAIN_KILL.manager.mathExpression, ctx)
        val lost = MathUtils.eval(PeaceTreatyConfig.WAR_POINTS_SCORES_LOSE_KILL.manager.mathExpression, ctx)

        killerKingdom.addWarPoints(deadKingdom, gained)
        deadKingdom.addWarPoints(killerKingdom, -lost)

        KLogger.debug(DEBUG_NS) { "Added $gained war points to ${killerKingdom.name} kingdom: ${killer.name} killed ${dead.name}" }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onKingdomItemBreak(event: KingdomItemBreakEvent<*>) {
        event.getMetadata<SiegeCannon>(SiegeCannon.NS) ?: return
        val item = event.getKingdomItem()!!

        val itemKingdom = item.getLand()!!.kingdom!!
        val player = event.getPlayer()!!
        val playerKingdom = player.kingdom!!

        if (itemKingdom.getRelationWith(playerKingdom) != KingdomRelation.ENEMY) return

        val ctx = PlaceholderContextBuilder().withContext(player.offlinePlayer).other(itemKingdom)

        val isTurret = item is Turret
        val gainedOpt = if (isTurret) PeaceTreatyConfig.WAR_POINTS_SCORES_GAIN_BREAK_TURRET else PeaceTreatyConfig.WAR_POINTS_SCORES_GAIN_BREAK_STRUCTURE
        val lostOpt = if (isTurret) PeaceTreatyConfig.WAR_POINTS_SCORES_LOSE_BREAK_TURRET else PeaceTreatyConfig.WAR_POINTS_SCORES_LOSE_BREAK_STRUCTURE

        val gained = MathUtils.eval(gainedOpt.manager.mathExpression, ctx)
        val lost = MathUtils.eval(lostOpt.manager.mathExpression, ctx)

        playerKingdom.addWarPoints(itemKingdom, gained)
        itemKingdom.addWarPoints(playerKingdom, -lost)

        KLogger.debug(DEBUG_NS) {
            "Added $gained war points to ${playerKingdom.name} kingdom because ${player.offlinePlayer.name} destroyed a " +
                    "${item.style.name} kingdom item of ${itemKingdom.name} kingdom."
        }
    }
}