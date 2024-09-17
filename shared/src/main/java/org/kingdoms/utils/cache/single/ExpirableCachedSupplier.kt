package org.kingdoms.utils.cache.single

import java.time.Duration
import java.util.function.Supplier

open class ExpirableCachedSupplier<T>(
    getter: Supplier<T>,
    private val cacheTime: Duration,
) : CachedSupplier<T>(getter) {
    private var lastChecked: Long = System.currentTimeMillis()

    init {
        if (cacheTime.seconds <= 5) throw IllegalArgumentException("Any cache time under 5 seconds is not likely to help with performance: ${cacheTime.toMillis()}ms")
    }

    override fun get(): T {
        val currentTime = System.currentTimeMillis()
        val diff = currentTime - lastChecked

        if (cached == null || cacheTime.minusMillis(diff).isNegative) {
            cached = supplier.get()
            lastChecked = currentTime
        }

        return cached!!
    }
}