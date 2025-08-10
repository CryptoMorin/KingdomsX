package org.kingdoms.utils;

import org.kingdoms.utils.internal.jdk.Java9;
import org.kingdoms.versioning.JavaVersion;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

/**
 * Copied straight from the JDK.
 */
public final class FutureUtil {
    private static final ScheduledThreadPoolExecutor DELAYER;

    static {
        if (JavaVersion.supports(9)) DELAYER = null;
        else {
            (DELAYER = new ScheduledThreadPoolExecutor(
                    1, new Delayer.DaemonThreadFactory()))
                    .setRemoveOnCancelPolicy(true);
        }
    }

    private static final class Delayer {
        static ScheduledFuture<?> delay(Runnable command, long delay,
                                        TimeUnit unit) {
            return DELAYER.schedule(command, delay, unit);
        }

        static final class DaemonThreadFactory implements ThreadFactory {
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName("CompletableFutureDelayScheduler");
                return t;
            }
        }
    }

    private static final class Canceller implements BiConsumer<Object, Throwable> {
        final Future<?> f;
        Canceller(Future<?> f) { this.f = f; }
        public void accept(Object ignore, Throwable ex) {
            if (f != null && !f.isDone())
                f.cancel(false);
        }
    }

    private static final class Timeout implements Runnable {
        final CompletableFuture<?> f;
        Timeout(CompletableFuture<?> f) { this.f = f; }
        public void run() {
            if (f != null && !f.isDone())
                f.completeExceptionally(new TimeoutException());
        }
    }

    private FutureUtil() {}

    public static <T> CompletableFuture<T> orTimeout(CompletableFuture<T> future, Duration duration) {
        Objects.requireNonNull(future, "Null future");
        Objects.requireNonNull(duration, "Null timeout duration");

        if (DELAYER == null) {
            return Java9.orTimeout(future, duration.toMillis(), TimeUnit.MILLISECONDS);
        }
        if (future.isDone()) return future;

        return future.whenComplete(
                new Canceller(
                        Delayer.delay(
                                new Timeout(future), duration.toMillis(), TimeUnit.MILLISECONDS)));
    }

    public static void shutdown() {
        if (DELAYER != null) DELAYER.shutdown();
    }
}
