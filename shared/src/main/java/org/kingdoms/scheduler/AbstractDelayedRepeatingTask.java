package org.kingdoms.scheduler;

import org.jetbrains.annotations.NotNull;
import org.kingdoms.utils.internal.functional.Fn;

import java.time.Duration;
import java.util.function.Consumer;

public abstract class AbstractDelayedRepeatingTask extends AbstractDelayedTask implements DelayedRepeatingTask {
    private final Duration intervalDelays;

    @NotNull
    public Duration getIntervalDelay() {
        return this.intervalDelays;
    }

    public AbstractDelayedRepeatingTask(@NotNull Consumer<DelayedRepeatingTask> runnable, @NotNull Duration initialDelay, @NotNull Duration intervalDelays, @NotNull Task.ExecutionContextType executionContextType) {
        super(Fn.cast(runnable), initialDelay, executionContextType);
        this.intervalDelays = intervalDelays;
    }
}
