package org.kingdoms.enginehub.worldguard.handlers

import org.kingdoms.config.KingdomsConfig
import org.kingdoms.constants.land.location.SimpleChunkLocation
import org.kingdoms.enginehub.worldguard.ServiceWorldGuard
import org.kingdoms.locale.KingdomsLang
import org.kingdoms.locale.messenger.Messenger
import org.kingdoms.managers.land.claiming.ClaimProcessor
import org.kingdoms.tasks.TaskState
import org.kingdoms.tasks.annotations.Before
import org.kingdoms.tasks.annotations.GroupedTask
import org.kingdoms.tasks.annotations.ReturnTaskState
import org.kingdoms.tasks.annotations.Task
import org.kingdoms.tasks.container.LocalTaskSession

@GroupedTask("WorldGuard")
class WorldGuardClaimProcessorTask(private val worldGuard: ServiceWorldGuard) : LocalTaskSession {
    @Task("PROTECTED_REGIONS") @Before("CLAIMS")
    @ReturnTaskState(TaskState.SHOULD_STOP)
    fun ClaimProcessor.checkProtectedRegions(): Messenger? {
        // if (!SoftService.WORLD_GUARD.isAvailable()) return null;
        if (isAdminMode) return null

        // TODO move KingdomsLang here to EngineHubLang, but thatd require to move this class to the enginehub module
        val radius = KingdomsConfig.Claims.PROTECTED_REGION_RADIUS.manager.int
        if (radius <= 1) {
            if (chunk.run {
                    worldGuard.isChunkInRegion(bukkitWorld, x, z, 0)
                }) return KingdomsLang.COMMAND_CLAIM_IN_REGION
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
                return KingdomsLang.COMMAND_CLAIM_NEAR_REGION
            }
        }

        return null
    }
}
