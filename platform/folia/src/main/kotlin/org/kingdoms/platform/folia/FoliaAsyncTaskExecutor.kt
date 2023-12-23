package org.kingdoms.platform.folia

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.kingdoms.platform.Task
import org.kingdoms.platform.TaskExecutor
import java.util.concurrent.TimeUnit

class FoliaAsyncTaskExecutor(private val plugin: Plugin) : TaskExecutor {
    override fun run(runnable: Runnable): Task =
        FoliaTask(Bukkit.getAsyncScheduler().runNow(plugin) { _ -> runnable.run() })

    override fun runDelayed(runnable: Runnable, delay: Long, timeUnit: TimeUnit?): Task {
        val millis = timeUnit?.toMillis(delay) ?: TaskExecutor.ticksToMillis(delay)
        return FoliaTask(
            Bukkit.getAsyncScheduler().runDelayed(plugin, { _ -> runnable.run() }, millis, TimeUnit.MILLISECONDS)
        )
    }

    override fun repeat(runnable: Runnable, initialDelay: Long, repeatInterval: Long, timeUnit: TimeUnit?): Task {
        val initialMillis = timeUnit?.toMillis(initialDelay) ?: TaskExecutor.ticksToMillis(initialDelay)
        val intervalMillis = timeUnit?.toMillis(repeatInterval) ?: TaskExecutor.ticksToMillis(repeatInterval)

        return FoliaTask(
            Bukkit.getAsyncScheduler()
                .runAtFixedRate(plugin, { _ -> runnable.run() }, initialMillis, intervalMillis, TimeUnit.MILLISECONDS)
        )
    }
}

class FoliaTask(private val scheduledTask: ScheduledTask) : Task {
    override fun cancel(): Task.CancelledState = Task.CancelledState.valueOf(scheduledTask.cancel().name)

    override fun getExecutionState(): Task.ExecutionState =
        Task.ExecutionState.valueOf(scheduledTask.executionState.name)
}