package org.kingdoms.services.pets

import org.bukkit.OfflinePlayer
import org.bukkit.entity.Entity
import org.kingdoms.services.Service
import java.util.*

interface ServicePet : Service {
    fun getPetInfo(entity: Entity): PetInfo?
}

class PetInfo(val owner: UUID?, @get:JvmName("canBeAttacked") val canBeAttacked: Boolean) {
    fun isOwnedBy(player: OfflinePlayer) = owner !== null && owner == player.uniqueId
}
