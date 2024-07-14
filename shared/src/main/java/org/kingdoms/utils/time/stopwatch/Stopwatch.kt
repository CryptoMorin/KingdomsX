package org.kingdoms.utils.time.stopwatch

import java.time.Duration

enum class StopwatchState {
    NOT_STARTED, TICKING, PAUSED, STOPPED;

    fun hasStarted() = this != NOT_STARTED
}

interface Stopwatch {
    fun getState(): StopwatchState
    fun elapsed(): Duration
    fun start(): Stopwatch
    fun stop(): Stopwatch
    fun resume(): Stopwatch
    fun pause(): Stopwatch

    companion object {
        @JvmStatic fun withTickAccuracy(passedTicks: Int = 0): Stopwatch = TickStopwatch(passedTicks)
        @JvmStatic fun withMillisAccuracy(passed: Duration = Duration.ZERO): Stopwatch = MillisStopwatch(passed)
    }
}