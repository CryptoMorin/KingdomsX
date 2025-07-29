package org.kingdoms.scheduler;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * TODO find a way to run the task after returning the value without delay.
 */
public class InstantTaskScheduler implements TaskScheduler {
    public static final InstantTaskScheduler INSTANCE = new InstantTaskScheduler();

    private InstantTaskScheduler() {}

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

    private static void checkDuration(Duration duration, String fieldName) {
        if (!duration.isZero()) throw new IllegalArgumentException(
                "Instant task scheduler cannot run task with " + fieldName + " of " + duration);
    }

    private static final class InstantDelayedTask extends AbstractDelayedTask {
        public InstantDelayedTask(@NotNull Consumer<DelayedTask> runnable, @NotNull Duration delay, @NotNull ExecutionContextType executionContextType) {
            super(runnable, delay, executionContextType);
        }
    }

    private static final class InstantDelayedRepeatingTask extends AbstractDelayedRepeatingTask {
        public InstantDelayedRepeatingTask(@NotNull Consumer<DelayedRepeatingTask> runnable, @NotNull Duration initialDelay, @NotNull Duration intervalDelays, @NotNull ExecutionContextType executionContextType) {
            super(runnable, initialDelay, intervalDelays, executionContextType);
        }
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
        checkDuration(delay, "delay");
        return new InstantDelayedTask(runnable, delay, getExecutionContextType());
    }

    @NotNull
    @Override
    public DelayedRepeatingTask repeating(@NotNull Duration initialDelay, @NotNull Duration intervalDelays, @NotNull Consumer<DelayedRepeatingTask> runnable) {
        checkDuration(initialDelay, "initialDelay");
        checkDuration(intervalDelays, "intervalDelays");
        return new InstantDelayedRepeatingTask(runnable, initialDelay, intervalDelays, getExecutionContextType());
    }
}
