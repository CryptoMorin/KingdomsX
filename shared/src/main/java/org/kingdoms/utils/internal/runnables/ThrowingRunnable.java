package org.kingdoms.utils.internal.runnables;

@FunctionalInterface
public interface ThrowingRunnable {
    void run() throws Throwable;
}
