package org.kingdoms.utils.cache.caffeine

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Policy
import com.github.benmanes.caffeine.cache.stats.CacheStats
import com.google.errorprone.annotations.CompatibleWith
import java.util.concurrent.ConcurrentMap
import java.util.function.Function

class ExpirableMap<K : Any, V : Any> : Cache<K, V> {
    val defaultExpirationStrategy: ExpirationStrategy?
    private val cache: Cache<K, ReferencedExpirableObject<V>>

    @JvmOverloads
    constructor(defaultExpirationStrategy: ExpirationStrategy? = null) {
        this.defaultExpirationStrategy = defaultExpirationStrategy
        this.cache = CacheHandler.newBuilder()
            .expireAfter(ExpirableObjectExpiry<K, ReferencedExpirableObject<V>>())
            .build()
    }

    @JvmOverloads
    constructor(
        builder: Caffeine<K, ReferencedExpirableObject<V>>,
        defaultExpirationStrategy: ExpirationStrategy? = null
    ) {
        this.defaultExpirationStrategy = defaultExpirationStrategy
        this.cache = builder.build()

        if (defaultExpirationStrategy != null) {
            builder.expireAfter(ExpirableObjectExpiry<K, ReferencedExpirableObject<V>>())
        }
    }

    override fun estimatedSize(): Long = cache.estimatedSize()

    override fun stats(): CacheStats = cache.stats()

    override fun asMap(): ConcurrentMap<K, V> = throw UnsupportedOperationException()

    override fun cleanUp() {
        cache.cleanUp()
    }

    override fun policy(): Policy<K, V> = throw UnsupportedOperationException()

    override fun invalidate(@CompatibleWith(value = "K") key: Any) {
        cache.invalidate(key)
    }

    override fun invalidateAll(keys: MutableIterable<*>) {
        cache.invalidateAll(keys)
    }

    fun put(key: K, value: V, expiry: ExpirationStrategy) {
        cache.put(key, ReferencedExpirableObject(value, expiry))
    }

    private val assertDefaultStrategy
        get() = defaultExpirationStrategy
            ?: throw UnsupportedOperationException("No default expiration strategy was set")

    override fun put(key: K, value: V) {
        cache.put(key, ReferencedExpirableObject(value, assertDefaultStrategy))
    }

    override fun getIfPresent(@CompatibleWith(value = "K") key: Any): V? = cache.getIfPresent(key)?.reference

    fun contains(key: K) = cache.getIfPresent(key) != null

    override fun get(key: K, mappingFunction: Function<in K, out V>): V {
        return cache.get(key) { ReferencedExpirableObject(mappingFunction.apply(key), assertDefaultStrategy) }!!.reference
    }

    override fun getAllPresent(keys: MutableIterable<*>): MutableMap<K, V> {
        return cache.getAllPresent(keys)?.asSequence()!!.associateTo(hashMapOf()) { it.key to it.value.reference }
    }

    override fun getAll(
        keys: MutableIterable<K>,
        mappingFunction: Function<MutableIterable<K>, MutableMap<K, V>>
    ): MutableMap<K, V> = throw UnsupportedOperationException()

    override fun putAll(map: MutableMap<out K, out V>) = throw UnsupportedOperationException()

    override fun invalidateAll() {
        cache.invalidateAll()
    }
}