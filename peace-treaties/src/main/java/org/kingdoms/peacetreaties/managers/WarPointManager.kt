package org.kingdoms.peacetreaties.managers

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.kingdoms.constants.group.Kingdom
import org.kingdoms.constants.group.model.relationships.StandardRelationAttribute
import org.kingdoms.constants.land.structures.objects.SiegeCannon
import org.kingdoms.constants.land.turrets.Turret
import org.kingdoms.events.invasion.KingdomInvadeEndEvent
import org.kingdoms.events.items.KingdomItemBreakEvent
import org.kingdoms.locale.placeholders.context.MessagePlaceholderProvider
import org.kingdoms.main.KLogger
import org.kingdoms.managers.pvp.PvPManager
import org.kingdoms.peacetreaties.config.PeaceTreatyConfig
import org.kingdoms.peacetreaties.config.PeaceTreatyLang
import org.kingdoms.peacetreaties.data.WarPoint.Companion.addWarPoints
import org.kingdoms.utils.KingdomsBukkitExtensions.asKingdomPlayer
import org.kingdoms.utils.MathUtils
import org.kingdoms.utils.debugging.DebugNS
import org.kingdoms.utils.debugging.KingdomsDebug

class WarPointManager : Listener {
    companion object {
        @JvmField
        val DEBUG_NS: DebugNS = KingdomsDebug.register("PEACE_TREATIES/WAR_POINTS")

        private fun allowedRelationShip(kingdom: Kingdom, other: Kingdom): Boolean {
            return PeaceTreatyConfig.WAR_POINTS_ALLOWED_RELATIONSHIPS.manager.stringList.contains(
                kingdom.getRelationWith(
                    other
                ).name
            )
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onInvade(event: KingdomInvadeEndEvent) {
        val invasion = event.getInvasion()
        if (!invasion.result.isSuccessful) return

        val attacker = invasion.attacker
        val defender = invasion.defender

        if (!allowedRelationShip(attacker, defender)) return

        val ctx = MessagePlaceholderProvider().withContext(attacker).other(defender)
        val gained = MathUtils.eval(PeaceTreatyConfig.WAR_POINTS_SCORES_GAIN_INVADE.manager.mathExpression, ctx)
        val lost = MathUtils.eval(PeaceTreatyConfig.WAR_POINTS_SCORES_LOSE_INVADE.manager.mathExpression, ctx)

        attacker.addWarPoints(defender, gained)
        defender.addWarPoints(attacker, -lost)

        ctx.raw("war_points", gained)
        PeaceTreatyLang.WAR_POINTS_GAIN_INVADE.sendMessage(invasion.invaderPlayer, ctx)
        ctx.raw("war_points", lost)
        for (member in defender.onlineMembers) {
            PeaceTreatyLang.WAR_POINTS_LOST_INVADE.sendError(member, ctx)
        }

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

        if (!allowedRelationShip(deadKingdom, killerKingdom)) return

        if (deadKingdom.hasAttribute(killerKingdom, StandardRelationAttribute.CEASEFIRE)) return

        val ctx = MessagePlaceholderProvider().withContext(killer).other(dead)
        val gained = MathUtils.eval(PeaceTreatyConfig.WAR_POINTS_SCORES_GAIN_KILL.manager.mathExpression, ctx)
        val lost = MathUtils.eval(PeaceTreatyConfig.WAR_POINTS_SCORES_LOSE_KILL.manager.mathExpression, ctx)

        killerKingdom.addWarPoints(deadKingdom, gained)
        deadKingdom.addWarPoints(killerKingdom, -lost)

        ctx.raw("war_points", gained)
        PeaceTreatyLang.WAR_POINTS_GAIN_KILL.sendMessage(killer, ctx)
        ctx.raw("war_points", lost)
        deadKingdom.onlineMembers.forEach { x -> PeaceTreatyLang.WAR_POINTS_LOST_KILL.sendError(x, ctx) }

        KLogger.debug(DEBUG_NS) { "Added $gained war points to ${killerKingdom.name} kingdom: ${killer.name} killed ${dead.name}" }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onKingdomItemBreak(event: KingdomItemBreakEvent<*>) {
        event.getMetadata<SiegeCannon>(SiegeCannon.NS) ?: return
        val item = event.getKingdomItem()!!

        val player = event.getPlayer() ?: return // Not caused by a player
        val itemKingdom = item.getLand()!!.kingdom!!
        val playerKingdom = player.kingdom!!

        if (!allowedRelationShip(itemKingdom, playerKingdom)) return

        val ctx = MessagePlaceholderProvider().withContext(player.offlinePlayer).other(itemKingdom)

        val isTurret = item is Turret
        val gainedOpt =
            if (isTurret) PeaceTreatyConfig.WAR_POINTS_SCORES_GAIN_BREAK_TURRET else PeaceTreatyConfig.WAR_POINTS_SCORES_GAIN_BREAK_STRUCTURE
        val lostOpt =
            if (isTurret) PeaceTreatyConfig.WAR_POINTS_SCORES_LOSE_BREAK_TURRET else PeaceTreatyConfig.WAR_POINTS_SCORES_LOSE_BREAK_STRUCTURE

        val gained = MathUtils.eval(gainedOpt.manager.mathExpression, ctx)
        val lost = MathUtils.eval(lostOpt.manager.mathExpression, ctx)

        playerKingdom.addWarPoints(itemKingdom, gained)
        itemKingdom.addWarPoints(playerKingdom, -lost)

        ctx.raw("style", item.style.displayName)
        ctx.raw("war_points", gained)
        val gainedMsg =
            if (isTurret) PeaceTreatyLang.WAR_POINTS_GAIN_BREAK_TURRET else PeaceTreatyLang.WAR_POINTS_GAIN_BREAK_STRUCTURE
        player.player?.let { gainedMsg.sendMessage(it, ctx) }

        ctx.raw("war_points", lost)
        val lostMsg =
            if (isTurret) PeaceTreatyLang.WAR_POINTS_LOST_BREAK_TURRET else PeaceTreatyLang.WAR_POINTS_LOST_BREAK_STRUCTURE
        itemKingdom.onlineMembers.forEach { x -> lostMsg.sendError(x, ctx) }

        KLogger.debug(DEBUG_NS) {
            "Added $gained war points to ${playerKingdom.name} kingdom because ${player.offlinePlayer.name} destroyed a " +
                    "${item.style.name} kingdom item of ${itemKingdom.name} kingdom."
        }
    }
}