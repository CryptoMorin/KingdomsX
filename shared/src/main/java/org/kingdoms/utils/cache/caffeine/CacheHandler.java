package org.kingdoms.utils.cache.caffeine;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import org.kingdoms.utils.internal.reflection.Reflect;

import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;

public final class CacheHandler {
    private static final ForkJoinPool POOL = new ForkJoinPool();
    private static final Scheduler CACHE_SCHEDULER;

    static {
        // TODO what do we do with services that use this if it's not supported?
        if (Reflect.classExists("com.github.benmanes.caffeine.cache.Scheduler")) {
            CACHE_SCHEDULER = Scheduler.forScheduledExecutorService(newScheduler());
        } else {
            CACHE_SCHEDULER = null;
        }
    }

    public static Caffeine<Object, Object> newBuilder() {
        return Caffeine.newBuilder().executor(POOL);
    }

    @SuppressWarnings("unchecked")
    public static <K, V> Caffeine<K, V> typedBuilder() {
        return (Caffeine<K, V>) newBuilder();
    }

    public static ForkJoinPool getPool() {
        return POOL;
    }

    public static Scheduler getCacheScheduler() {
        return CACHE_SCHEDULER;
    }

    public static ScheduledExecutorService newScheduler() {
        return Executors.newSingleThreadScheduledExecutor(Executors.defaultThreadFactory());
    }
}
