package org.kingdoms.utils.time.stopwatch

import org.jetbrains.annotations.ApiStatus.OverrideOnly
import java.time.Duration

abstract class AbstractStopwatch(var passed: Long = 0) : Stopwatch {
    private var lastCheckedTicks: Long = 0L
    override var state: StopwatchState = StopwatchState.NOT_STARTED

    @Suppress("INAPPLICABLE_TARGET_ON_PROPERTY_WARNING")
    override val elapsed: Duration
        @get:JvmName("elapsed")
        get() {
            if (state === StopwatchState.TICKING) updateTicks()
            return Duration.ofMillis(this.passed)
        }

    @OverrideOnly
    abstract fun getCurrentTime(): Long

    final override fun start(): AbstractStopwatch {
        if (state !== StopwatchState.NOT_STARTED) exception("Cannot start in this state")
        start0()
        return this
    }

    final override fun stop(): AbstractStopwatch {
        if (state === StopwatchState.STOPPED) exception("Already stopped")
        updateTicks()
        state = StopwatchState.STOPPED
        return this
    }

    override fun reset(): Stopwatch {
        if (state === StopwatchState.STOPPED) exception("Stopwatch is stopped")
        passed = 0
        lastCheckedTicks = 0L
        state = StopwatchState.NOT_STARTED
        return this
    }

    private fun updateTicks() {
        if (state === StopwatchState.NOT_STARTED) return

        val currTicks = getCurrentTime()
        val ticksPassedSinceLastCheck = currTicks - lastCheckedTicks
        this.passed += ticksPassedSinceLastCheck
        this.lastCheckedTicks = currTicks
    }

    final override fun resume(): AbstractStopwatch {
        // Don't use ensureRunning here, it'll call updateTicks()
        if (state !== StopwatchState.PAUSED) exception("Can't resume")
        start0()
        return this
    }

    private fun exception(msg: String): Nothing = throw IllegalStateException("$msg: $this")

    private fun start0() {
        state = StopwatchState.TICKING
        this.lastCheckedTicks = getCurrentTime()
    }

    final override fun pause(): AbstractStopwatch {
        if (state !== StopwatchState.TICKING) exception("Can't pause")
        state = StopwatchState.PAUSED
        updateTicks()
        return this
    }

    override fun toString(): String = "${javaClass.simpleName}($state | $elapsed)"
}