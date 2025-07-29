package org.kingdoms.scheduler

import org.kingdoms.abstraction.Cancellable
import org.kingdoms.scheduler.convert.UnconsumedRunnable
import java.time.Duration
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.Executor
import java.util.function.Consumer

interface ScheduledTask : Task, Cancellable

interface DelayedTask : ScheduledTask {
    fun getDelay(): Duration
}

interface DelayedRepeatingTask : DelayedTask {
    fun getIntervalDelay(): Duration
}