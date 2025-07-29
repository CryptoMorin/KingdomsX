package org.kingdoms.scheduler;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * TODO find a way to run the task after returning the value without delay.
 */
public class InstantOrDelayedTaskScheduler implements TaskScheduler {
    private final TaskScheduler scheduler;

    public InstantOrDelayedTaskScheduler(TaskScheduler scheduler) {this.scheduler = scheduler;}

    @NotNull
    @Override
    public Task.ExecutionContextType getExecutionContextType() {
        return Task.ExecutionContextType.SYNC;
    }

    @NotNull
    @Override
    public Executor getExecutor() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public Task execute(@NotNull Consumer<Task> runnable) {
        AbstractTask task = new AbstractTask(getExecutionContextType(), runnable);
        runnable.accept(task);
        return task;
    }

    @NotNull
    @Override
    public DelayedTask delayed(@NotNull Duration delay, @NotNull Consumer<DelayedTask> runnable) {
        return scheduler.delayed(delay, runnable);
    }

    @NotNull
    @Override
    public DelayedRepeatingTask repeating(@NotNull Duration initialDelay, @NotNull Duration intervalDelays, @NotNull Consumer<DelayedRepeatingTask> runnable) {
        return scheduler.repeating(initialDelay, intervalDelays, runnable);
    }
}
