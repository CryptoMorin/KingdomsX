package org.kingdoms.scheduler;

import org.jetbrains.annotations.NotNull;
import org.kingdoms.utils.internal.functional.Fn;

import java.time.Duration;
import java.util.function.Consumer;

public abstract class AbstractDelayedTask extends AbstractTask implements DelayedTask {
    private boolean cancelled;
    private final Duration delay;

    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public boolean cancel() {
        return this.cancelled = true;
    }

    @NotNull
    public Duration getDelay() {
        return this.delay;
    }

    public AbstractDelayedTask(@NotNull Consumer<DelayedTask> runnable, @NotNull Duration delay, @NotNull Task.ExecutionContextType executionContextType) {
        super(executionContextType, Fn.cast(runnable));
        this.delay = delay;
    }
}