@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package org.kingdoms.platform.bukkit.entity

import org.bukkit.*
import org.bukkit.OfflinePlayer
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.profile.PlayerProfile
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * Used because [Bukkit.getOfflinePlayer] connects to Mojang to get player information if they've never joined the server before.
 * We don't want that to happen so we save our own fake player. We only use offline players to get their ID and name and have nothing
 * to do with players that have never joined the server before. If we were to cache players from [org.bukkit.event.player.PlayerJoinEvent] then it'd save a lot of
 * useless data that we absolutely do not need.
 */

class OfflinePlayer(private val id: UUID, private val name: String) : OfflinePlayer {
    @Suppress("NOTHING_TO_INLINE")
    private inline fun unsupported(): Nothing =
        throw UnsupportedOperationException("Cannot use method on fake offline player")

    override fun getName(): String = name
    override fun getUniqueId(): UUID = id
    override fun hasPlayedBefore(): Boolean = true
    override fun isOnline(): Boolean = player != null
    override fun getPlayer(): Player? = Bukkit.getPlayer(id)

    override fun isOp(): Boolean = unsupported()
    override fun setOp(value: Boolean) = unsupported()
    override fun serialize(): MutableMap<String, Any> = unsupported()
    override fun getPlayerProfile(): PlayerProfile = unsupported()

    override fun isBanned(): Boolean = unsupported()
    override fun ban(p0: String?, p1: Date?, p2: String?): BanEntry<PlayerProfile> = unsupported()
    override fun ban(p0: String?, p1: Instant?, p2: String?): BanEntry<PlayerProfile> = unsupported()
    override fun ban(p0: String?, p1: Duration?, p2: String?): BanEntry<PlayerProfile> = unsupported()

    override fun isWhitelisted(): Boolean = unsupported()
    override fun setWhitelisted(value: Boolean) = unsupported()
    override fun getFirstPlayed(): Long = unsupported()
    override fun getLastPlayed(): Long = unsupported()
    override fun getBedSpawnLocation(): Location = unsupported()
    override fun getRespawnLocation(): Location? = unsupported()
    // Paper
    //    override fun getLastLogin(): Long = unsupported()
    //    override fun getLastSeen(): Long = unsupported()

    override fun incrementStatistic(statistic: Statistic) = unsupported()
    override fun incrementStatistic(statistic: Statistic, amount: Int) = unsupported()
    override fun incrementStatistic(statistic: Statistic, material: Material) = unsupported()
    override fun incrementStatistic(statistic: Statistic, material: Material, amount: Int) = unsupported()
    override fun incrementStatistic(statistic: Statistic, entityType: EntityType) = unsupported()
    override fun incrementStatistic(statistic: Statistic, entityType: EntityType, amount: Int) = unsupported()
    override fun decrementStatistic(statistic: Statistic) = unsupported()
    override fun decrementStatistic(statistic: Statistic, amount: Int) = unsupported()
    override fun decrementStatistic(statistic: Statistic, material: Material) = unsupported()
    override fun decrementStatistic(statistic: Statistic, material: Material, amount: Int) = unsupported()
    override fun decrementStatistic(statistic: Statistic, entityType: EntityType) = unsupported()
    override fun decrementStatistic(statistic: Statistic, entityType: EntityType, amount: Int) = unsupported()
    override fun setStatistic(statistic: Statistic, newValue: Int) = unsupported()
    override fun setStatistic(statistic: Statistic, material: Material, newValue: Int) = unsupported()
    override fun setStatistic(statistic: Statistic, entityType: EntityType, newValue: Int) = unsupported()
    override fun getStatistic(statistic: Statistic): Int = unsupported()
    override fun getStatistic(statistic: Statistic, material: Material): Int = unsupported()
    override fun getStatistic(statistic: Statistic, entityType: EntityType): Int = unsupported()
    override fun getLastDeathLocation(): Location = unsupported()
    override fun getLocation(): Location? = unsupported()
}