package org.kingdoms.utils.internal.functional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A class that holds multiple {@link Runnable} which are added to this object when a certain condition is met
 * but these runnables themselves change the state of other objects and they must only be run if all other
 * conditions are met.
 * <p>
 * All runnables are executed in insertion order.
 * This finalizer is not re-usable and can be only ran once.
 */
public final class Finalizer implements Runnable {
    private final List<Runnable> tasks = new ArrayList<>(5);
    private boolean ran = false;

    private void ensureOpen() {
        if (ran) throw new IllegalStateException("Finalizer was already executed");
    }

    public void add(Finalizer finalizer) {
        ensureOpen();
        this.tasks.addAll(finalizer.tasks);
    }

    public void add(Runnable runnable) {
        ensureOpen();
        Objects.requireNonNull(runnable, "Cannot add null task");
        tasks.add(runnable);
    }

    @Override
    public void run() {
        ensureOpen();
        ran = true;
        for (Runnable task : tasks) {
            task.run();
        }
    }
}
