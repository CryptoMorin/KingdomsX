package org.kingdoms.utils.cache.caffeine;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;

public final class CacheHandler {
    private static final ForkJoinPool POOL = new ForkJoinPool();
    private static final Scheduler CACHE_SCHEDULER = Scheduler.forScheduledExecutorService(newScheduler());

    public static Caffeine<Object, Object> newBuilder() {
        return Caffeine.newBuilder().executor(POOL);
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
