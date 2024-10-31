package org.kingdoms.enginehub.worldguard.handlers

import org.kingdoms.enginehub.worldguard.ServiceWorldGuard
import org.kingdoms.managers.land.map.KingdomsMap
import org.kingdoms.managers.land.map.KingdomsMapElement

class WorldGuardKingdomsMapHandler(
    private val worldGuard: ServiceWorldGuard
) : KingdomsMapElement {

    override fun getElement(map: KingdomsMap): String? {
        map.chunk.apply {
            if (worldGuard.isChunkInRegion(bukkitWorld, x, z, 0)) return "protected"
        }
        return null
    }
}