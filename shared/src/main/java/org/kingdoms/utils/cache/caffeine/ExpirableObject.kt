package org.kingdoms.utils.cache.caffeine

import com.github.benmanes.caffeine.cache.Expiry
import java.time.Duration

interface ExpirableObject {
    val expirationStrategy: ExpirationStrategy
}

class ExpirationStrategy(
    val expiryAfterCreate: Duration,
    val expiryAfterUpdate: Duration? = null,
    val expiryAfterRead: Duration? = null
) {
    companion object {
        @JvmStatic fun all(duration: Duration) = ExpirationStrategy(duration, duration, duration)
        @JvmStatic fun expireAfterRead(duration: Duration) = ExpirationStrategy(duration, duration, duration)
        @JvmStatic fun expireAfterCreate(duration: Duration) = ExpirationStrategy(duration, null, null)
    }
}

class ReferencedExpirableObject<T>(
    val reference: T,
    override val expirationStrategy: ExpirationStrategy
) : ExpirableObject

class ExpirableObjectExpiry<K : Any, V : ExpirableObject> : Expiry<K, V> {
    override fun expireAfterCreate(
        key: K,
        value: V,
        currentTime: Long
    ): Long = value.expirationStrategy.expiryAfterCreate.toNanos()

    override fun expireAfterUpdate(
        key: K,
        value: V,
        currentTime: Long,
        currentDuration: Long,
    ): Long = value.expirationStrategy.expiryAfterUpdate?.toNanos() ?: currentDuration

    override fun expireAfterRead(
        key: K,
        value: V,
        currentTime: Long,
        currentDuration: Long,
    ): Long = value.expirationStrategy.expiryAfterRead?.toNanos() ?: currentDuration
}