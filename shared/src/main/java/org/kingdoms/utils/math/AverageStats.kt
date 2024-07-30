package org.kingdoms.utils.math

import java.util.concurrent.ConcurrentHashMap

/**
 * Instead of keeping all the data set, we update the
 * values we need in order to take an average from
 * a data set with a much, much smaller memory.
 */
class Avg(var sum: Double, var count: Long) {
    fun plus(additional: Double): Avg = this.apply { count++; sum += additional; }
    fun getAverage(): Double = (sum / count)
}

class AverageStats<K : Any, V : Number> {
    private val data: MutableMap<K, Avg> = ConcurrentHashMap()

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
        return avg.getAverage() as V
    }
}