package org.kingdoms.enginehub

import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.kingdoms.constants.namespace.Namespace
import org.kingdoms.constants.player.KingdomPlayer
import org.kingdoms.enginehub.worldguard.ServiceWorldGuard
import org.kingdoms.enginehub.worldguard.handlers.WorldGuardClaimProcessorTask
import org.kingdoms.enginehub.worldguard.handlers.WorldGuardKingdomsMapHandler
import org.kingdoms.enginehub.worldguard.handlers.WorldGuardLandVisualizerPreparation
import org.kingdoms.enginehub.worldguard.handlers.WorldGuradPvPHandler
import org.kingdoms.events.lands.LandChangeEvent
import org.kingdoms.managers.FlyManager
import org.kingdoms.managers.entity.KingdomEntityRegistry
import org.kingdoms.managers.entity.types.KingdomChampionEntity
import org.kingdoms.managers.land.LandChangeWatcher
import org.kingdoms.managers.land.claiming.ClaimProcessor
import org.kingdoms.managers.land.map.KingdomsMap
import org.kingdoms.managers.pvp.PvPManager
import org.kingdoms.tasks.annotations.TaskSessionConstructor
import org.kingdoms.tasks.container.LocalTaskSession
import org.kingdoms.tasks.context.InputTaskContext

class WorldGuardHandler(private val addon: EngineHubAddon) : Listener {
    private class ClaimProcessorHandler(private val wg: ServiceWorldGuard) :
        TaskSessionConstructor<InputTaskContext<ClaimProcessor>> {
        override fun createSession(context: InputTaskContext<ClaimProcessor>): LocalTaskSession =
            WorldGuardClaimProcessorTask(wg)
    }

    init {
        val wg: ServiceWorldGuard = addon.worldGuard!!
        if (EngineHubConfig.WORLDGUARD_INDICATOR_IGNORE_REGIONS.manager.boolean) {
            LandChangeWatcher.register(WorldGuardLandVisualizerPreparation(wg))
        }

        if (EngineHubConfig.WORLDGUARD_MAP_MARKERS.manager.boolean) {
            KingdomsMap.registerElement(WorldGuardKingdomsMapHandler(wg))
        }

        if (EngineHubConfig.WORLDGUARD_REGION_CLAIM_PROTECTION.manager.boolean) {
            PvPManager.registerHandler(WorldGuradPvPHandler(wg))
            ClaimProcessor.getTasks().register(WorldGuardClaimProcessorTask::class.java, ClaimProcessorHandler(wg))
        }

        if (EngineHubConfig.WORLDGUARD_REGISTER_FLAGS.manager.boolean) {
            addon.server.pluginManager.registerEvents(this, addon)
        }

        // https://github.com/aromaa/WorldGuardExtraFlags/blob/master/WG/src/main/java/net/goldtreeservers/worldguardextraflags/flags/Flags.java#L49
        val flyFlag = EngineHubConfig.WORLDGUARD_KEEP_KINGDOMS_FLIGHT_WITH_FLAG.manager.string
        if (flyFlag !== null && flyFlag.isNotBlank()) {
            FlyManager.addLandCheckup(LandCheckup(wg, flyFlag))
        }
    }

    private class LandCheckup(private val worldguard: ServiceWorldGuard, private val flagName: String) :
        FlyManager.LandCheckup {
        companion object {
            private val NS = Namespace("EngineHub", "WORLD_GUARD")
        }

        override fun getNamespace(): Namespace = NS

        override fun canFly(event: LandChangeEvent): Boolean? {
            if (worldguard.hasFlag(event.player, event.to, flagName)) return true
            return null
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onChampionDamage(event: EntityDamageByEntityEvent) {
        val damager: Player = PvPManager.getDamager(event.damager) ?: return

        val victim: Entity = event.entity
        val kEntity = KingdomEntityRegistry.get(victim) ?: return
        if (kEntity !is KingdomChampionEntity) return

        val attackerKp: KingdomPlayer = KingdomPlayer.getKingdomPlayer(damager)
        if (attackerKp.isAdmin) return

        if (!addon.worldGuard!!.canDamageChampion(damager)) {
            EngineHubLang.INVASION_BLOCKED_DAMAING_CHAMPION_IN_PROTECTED_REGION.sendError(damager)
            event.isCancelled = true
            return
        }
    }
}