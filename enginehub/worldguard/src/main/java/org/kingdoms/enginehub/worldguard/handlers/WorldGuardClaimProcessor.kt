package org.kingdoms.enginehub.worldguard.handlers

import org.kingdoms.constants.land.location.SimpleChunkLocation
import org.kingdoms.enginehub.EngineHubConfig
import org.kingdoms.enginehub.EngineHubLang
import org.kingdoms.enginehub.worldguard.ServiceWorldGuard
import org.kingdoms.locale.messenger.Messenger
import org.kingdoms.managers.land.claiming.ClaimProcessor
import org.kingdoms.permissions.KingdomsDefaultPluginPermission
import org.kingdoms.tasks.TaskState
import org.kingdoms.tasks.annotations.Before
import org.kingdoms.tasks.annotations.GroupedTask
import org.kingdoms.tasks.annotations.ReturnTaskState
import org.kingdoms.tasks.annotations.Task
import org.kingdoms.tasks.container.LocalTaskSession

@GroupedTask(namespace = "WorldGuard")
class WorldGuardClaimProcessorTask(private val worldGuard: ServiceWorldGuard) : LocalTaskSession {
    @Task(key = "PROTECTED_REGIONS") @Before(other = "CLAIMS")
    @ReturnTaskState(state = TaskState.SHOULD_STOP)
    fun ClaimProcessor.checkProtectedRegions(): Messenger? {
        // if (!SoftService.WORLD_GUARD.isAvailable()) return null;
        if (isAdminMode) return null
        if (KingdomsDefaultPluginPermission.`WORLDGUARD_BYPASS_CLAIM$PROTECTION`.hasPermission(
                kingdomPlayer.player!!,
                false
            )
        ) return null

        val radius = EngineHubConfig.WORLDGUARD_PROTECTED_REGION_RADIUS.manager.int
        if (radius <= 1) {
            if (chunk.run {
                    worldGuard.isChunkInRegion(bukkitWorld, x, z, 0)
                }) return EngineHubLang.COMMAND_CLAIM_IN_REGION
        } else {
            val world = chunk.bukkitWorld
            val x = chunk.x
            val z = chunk.z

            val protectedRegion = chunk.findFromSurroundingChunks(
                radius
            ) { current: SimpleChunkLocation? ->
                if (worldGuard.isChunkInRegion(world, x, z, radius)) current else null
            }

            if (protectedRegion != null) {
                return EngineHubLang.COMMAND_CLAIM_NEAR_REGION
            }
        }

        return null
    }
}
