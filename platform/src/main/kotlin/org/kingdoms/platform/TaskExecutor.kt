package org.kingdoms.platform

import java.util.concurrent.TimeUnit

interface TaskExecutor {
    fun run(runnable: Runnable): Task

    fun runDelayed(runnable: Runnable, delay: Long, timeUnit: TimeUnit?): Task

    fun repeat(runnable: Runnable, initialDelay: Long, repeatInterval: Long, timeUnit: TimeUnit?): Task

    companion object {
        @JvmStatic fun ticksToMillis(ticks: Long): Long = (ticks / 20) * 1000
    }
}