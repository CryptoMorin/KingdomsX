package org.kingdoms.enginehub.worldguard.handlers

import org.kingdoms.enginehub.worldguard.ServiceWorldGuard
import org.kingdoms.managers.land.map.KingdomsMap
import org.kingdoms.managers.land.map.KingdomsMapHandler
import org.kingdoms.managers.land.map.KingdomsMapHandlerConstructor

class WorldGuardKingdomsMapConstructor(private val worldGuard: ServiceWorldGuard) : KingdomsMapHandlerConstructor {
    override fun construct(map: KingdomsMap): KingdomsMapHandler = WorldGuardKingdomsMapHandler(map, worldGuard)
}

class WorldGuardKingdomsMapHandler(
    private val map: KingdomsMap,
    private val worldGuard: ServiceWorldGuard
) : KingdomsMapHandler {
    override fun getChunkOptionName(): String? {
        map.chunk.apply {
            if (worldGuard.isChunkInRegion(bukkitWorld, x, z, 0)) return "protected"
        }
        return null
    }
}