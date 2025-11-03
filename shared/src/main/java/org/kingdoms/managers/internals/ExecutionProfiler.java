package org.kingdoms.managers.internals;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * Minimalist code profiler.
 */
public class ExecutionProfiler {
    private final long start;

    /**
     * Creates a new stopwatch and starts timing immediately.
     */
    public ExecutionProfiler() {
        this.start = System.nanoTime();
    }

    /**
     * Asserts that the elapsed time since construction is less than the given duration.
     * If not, runs {@code orElse} with details on the overrun.
     *
     * @param duration the maximum allowed duration
     */
    public boolean assertLessThan(Duration duration, Consumer<Long> orElse) {
        long elapsedNs = System.nanoTime() - start;
        if (elapsedNs >= duration.toNanos()) {
            orElse.accept(elapsedNs);
            return true;
        }
        return false;
    }

    public long elapsedNs() {
        return System.nanoTime() - start;
    }

    /**
     * The elapsed time since start.
     */
    public Duration elapsed() {
        return Duration.ofNanos(elapsedNs());
    }
}