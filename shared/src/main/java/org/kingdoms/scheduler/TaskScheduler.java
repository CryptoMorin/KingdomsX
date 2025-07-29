package org.kingdoms.scheduler;

import com.cryptomorin.xseries.reflection.XReflection;
import org.jetbrains.annotations.NotNull;
import org.kingdoms.scheduler.convert.UnconsumedRunnable;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * NOTE: This class must be in Java to prevent Kingdoms bootstrap issues because of Kotlin stdlib.
 */
public interface TaskScheduler {
    @NotNull
    Task.ExecutionContextType getExecutionContextType();

    @NotNull
    Executor getExecutor();

    @NotNull
    default <T extends Task> Consumer<T> unconsumableRunnable(@NotNull Runnable runnable) {
        return new UnconsumedRunnable<>(runnable);
    }

    @NotNull
    default Task execute(@NotNull Runnable runnable) {
        return this.execute(unconsumableRunnable(runnable));
    }

    @NotNull
    default DelayedTask delayed(@NotNull Duration delay, @NotNull Runnable runnable) {
        return this.delayed(delay, unconsumableRunnable(runnable));
    }

    @NotNull
    default DelayedRepeatingTask repeating(@NotNull Duration initialDelay, @NotNull Duration intervalDelays, @NotNull Runnable runnable) {
        return this.repeating(initialDelay, intervalDelays, unconsumableRunnable(runnable));
    }

    @NotNull
    Task execute(@NotNull Consumer<Task> var1);

    @NotNull
    DelayedTask delayed(@NotNull Duration var1, @NotNull Consumer<DelayedTask> var2);

    @NotNull
    DelayedRepeatingTask repeating(@NotNull Duration var1, @NotNull Duration var2, @NotNull Consumer<DelayedRepeatingTask> var3);

    @NotNull
    default CompletableFuture<Void> runFuture(@NotNull Runnable runnable) {
        return CompletableFuture.runAsync(runnable, this.getExecutor());
    }

    default <T> CompletableFuture<T> supplyFuture(Callable<T> callable) {
        return CompletableFuture.supplyAsync(() -> supplyFuture0(callable), this.getExecutor());
    }

    static <T> T supplyFuture0(Callable<T> callable) {
        try {
            return callable.call();
        } catch (Exception ex) {
            if (ex instanceof RuntimeException) {
                throw XReflection.throwCheckedException(ex);
            } else {
                throw new CompletionException(ex);
            }
        }
    }
}
