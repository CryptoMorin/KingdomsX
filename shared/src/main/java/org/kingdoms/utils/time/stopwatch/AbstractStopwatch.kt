package org.kingdoms.utils.time.stopwatch

import org.jetbrains.annotations.ApiStatus.OverrideOnly
import java.time.Duration

abstract class AbstractStopwatch(var passed: Long = 0) : Stopwatch {
    private var lastCheckedTicks: Long = 0L
    private var state: StopwatchState = StopwatchState.NOT_STARTED

    override fun elapsed(): Duration {
        if (state == StopwatchState.TICKING) updateTicks()
        return Duration.ofMillis(this.passed)
    }

    @OverrideOnly
    abstract fun getCurrentTime(): Long

    final override fun start(): AbstractStopwatch {
        checkStopped()
        resume()
        return this
    }

    final override fun stop(): AbstractStopwatch {
        checkStopped()
        state = StopwatchState.STOPPED
        return this
    }

    private fun updateTicks() {
        if (state == StopwatchState.NOT_STARTED) return

        val currTicks = getCurrentTime()
        val ticksPassedSinceLastCheck = currTicks - lastCheckedTicks
        this.passed += ticksPassedSinceLastCheck
        this.lastCheckedTicks = currTicks
    }

    private fun checkStopped() {
        if (state == StopwatchState.STOPPED) throw IllegalStateException("Counter has stopped")
        updateTicks()
    }

    final override fun resume(): AbstractStopwatch {
        checkStopped()
        if (state == StopwatchState.TICKING) throw IllegalStateException("Already ticking")
        state = StopwatchState.TICKING
        this.lastCheckedTicks = getCurrentTime()
        return this
    }

    final override fun getState(): StopwatchState = state

    final override fun pause(): AbstractStopwatch {
        checkStopped()
        if (state == StopwatchState.PAUSED) throw IllegalStateException("Already paused")
        state = StopwatchState.PAUSED
        updateTicks()
        return this
    }
}