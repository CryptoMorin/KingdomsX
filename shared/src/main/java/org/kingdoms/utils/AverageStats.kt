package org.kingdoms.utils

import java.util.concurrent.ConcurrentHashMap

class AverageStats<K : Any, V : Number> {
    private val data: MutableMap<K, Avg> = ConcurrentHashMap()

    /**
     * Instead of keeping all the data set, we update the
     * values we need in order to take an average from
     * a data set with a much, much smaller memory.
     */
    private class Avg(var sum: Double, var count: Long)

    fun addData(key: K, data: V) {
        val avg = this.data.getOrPut(key) { Avg(0.0, 0L) }
        avg.apply {
            sum += data.toDouble()
            count++

            if (sum >= Double.MAX_VALUE) {
                // Time to free up some memory.
                sum = Double.MAX_VALUE / count
                count = 1
            }
        }
    }

    @Suppress("UNCHECKED_CAST") fun getAverage(key: K): V? {
        val avg = this.data[key] ?: return null
        return (avg.sum / avg.count) as V
    }
}