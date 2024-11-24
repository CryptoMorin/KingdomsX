package org.kingdoms.utils.cache.single

import java.time.Duration
import java.util.function.Function
import java.util.function.Supplier

open class ExpirableCachedFunction<I, O>(
    private val cacheTime: Duration,
    function: Function<I, O>,
) : CachedFunction<I, O>(function) {
    private var lastChecked: Long = System.currentTimeMillis()

    init {
        if (cacheTime.seconds <= 5) throw IllegalArgumentException("Any cache time under 5 seconds is not likely to help with performance: ${cacheTime.toMillis()}ms")
    }

    override fun apply(input: I): O {
        val currentTime = System.currentTimeMillis()
        val diff = currentTime - lastChecked

        if (cached == null || cacheTime.minusMillis(diff).isNegative) {
            cached = function.apply(input)
            lastChecked = currentTime
        }

        return cached!!
    }

    companion object {
        @JvmStatic fun <I, O> of(cacheTime: Duration, function: Function<I, O>) = ExpirableCachedFunction(cacheTime, function)
    }
}