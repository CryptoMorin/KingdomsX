package org.kingdoms.utils.time.internal

import org.kingdoms.utils.time.internal.TickDuration.MILLISECONDS_IN_ONE_TICK
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import java.time.temporal.TemporalAmount
import java.time.temporal.TemporalUnit

object TickDuration {
    const val MILLISECONDS_IN_ONE_TICK: Long = 50L
    @JvmField val ONE = of(1)
    @JvmStatic fun of(ticks: Long) = Duration.ofMillis(ticks * MILLISECONDS_IN_ONE_TICK)
    // @JvmStatic fun of(ticks: Long) = TickTemporalAmount(ticks)
    // @JvmField val ONE = of(1)
}

object TickTemporalUnit : TemporalUnit {
    @JvmStatic val INSTANCE = this

    /**
     * Gets the duration in Minecraft ticks (estimated with constant server ticks 50ms)
     */
    @JvmStatic fun Duration.toTicks(): Long {
        // We can't use this.get(TickTemporalUnit) because of Java's weird ass temporal API.
        return this.toMillis() / MILLISECONDS_IN_ONE_TICK
    }

    override fun getDuration(): Duration = TickDuration.ONE
    override fun isDurationEstimated(): Boolean = true
    override fun isDateBased(): Boolean = false
    override fun isTimeBased(): Boolean = true

    override fun <R : Temporal> addTo(temporal: R, amount: Long): R {
        // ChronoUtils also uses this type of casting hack.
        @Suppress("UNCHECKED_CAST")
        return temporal.plus(MILLISECONDS_IN_ONE_TICK, ChronoUnit.MILLIS) as R
    }

    override fun between(temporal1Inclusive: Temporal, temporal2Exclusive: Temporal): Long {
        // Copied from ChronoUtils
        return temporal1Inclusive.until(temporal2Exclusive, this)
    }
}

class TickTemporalAmount(val ticks: Long) : TemporalAmount {
    override fun get(unit: TemporalUnit): Long {
        return when (unit) {
            is TickTemporalUnit -> ticks
            else -> throw UnsupportedOperationException("Unsupported unit: $unit")
        }
    }

    override fun getUnits(): MutableList<TemporalUnit> = mutableListOf(TickTemporalUnit)

    override fun addTo(temporal: Temporal): Temporal {
        return temporal.plus(MILLISECONDS_IN_ONE_TICK, ChronoUnit.MILLIS)
    }

    override fun subtractFrom(temporal: Temporal): Temporal {
        return temporal.minus(MILLISECONDS_IN_ONE_TICK, ChronoUnit.MILLIS)
    }
}