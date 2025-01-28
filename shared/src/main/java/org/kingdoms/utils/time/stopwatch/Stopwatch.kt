package org.kingdoms.utils.time.stopwatch

import java.time.Duration

enum class StopwatchState {
    NOT_STARTED, TICKING, PAUSED, STOPPED;

    fun hasStarted() = this != NOT_STARTED
}

class StopwatchException(message: String) : RuntimeException(message)

/**
 * A simple timer to keep track of the amount of time that has passed.
 * Most methods may throw [StopwatchException] depending on their [state]
 */
interface Stopwatch {
    val state: StopwatchState

    /**
     * The amount of time passed. (Considering pauses)
     */
    var elapsed: Duration

    /**
     * Sets [elapsed] to zero and [pause]s the timer.
     */
    fun reset(): Stopwatch

    /**
     * Start the timer.
     * @throws IllegalStateException if the timer is already started.
     */
    fun start(): Stopwatch

    /**
     * Stop the timer so it cannot be used anymore.
     * @throws IllegalStateException if the timer is already stopped.
     */
    fun stop(): Stopwatch

    /**
     * Resumes the timer after it was paused with [pause].
     * @throws IllegalStateException if the timer is not paused or started at all.
     */
    fun resume(): Stopwatch

    /**
     * Pauses the timer to be resumed again with [resume]
     * @throws IllegalStateException if the timer is not started yet or is stopped.
     */
    fun pause(): Stopwatch

    companion object {
        @JvmStatic @JvmOverloads fun withTickAccuracy(passedTicks: Int = 0): Stopwatch = TickStopwatch(passedTicks)
        @JvmStatic @JvmOverloads fun withMillisAccuracy(passed: Duration = Duration.ZERO): Stopwatch =
            MillisStopwatch(passed)
    }
}