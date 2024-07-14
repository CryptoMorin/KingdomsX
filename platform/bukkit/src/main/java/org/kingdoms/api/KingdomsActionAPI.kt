package org.kingdoms.api

import org.bukkit.OfflinePlayer
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import org.kingdoms.server.location.Location

interface KingdomsActionProcessor {
    fun canBreak(offlinePlayer: OfflinePlayer, itemStack: ItemStack?, location: Location): Boolean
    fun canPlace(offlinePlayer: OfflinePlayer, itemStack: ItemStack?, location: Location): Boolean
    fun canUseBlock(offlinePlayer: OfflinePlayer, itemStack: ItemStack?, location: Location): Boolean
    fun canUseItem(offlinePlayer: OfflinePlayer, itemStack: ItemStack?, location: Location): Boolean
    fun canInteractWithEntity(offlinePlayer: OfflinePlayer, entity: Entity, itemStack: ItemStack?): Boolean
    fun canHurtEntity(first: Entity, other: Entity, itemStack: ItemStack?): Boolean
}