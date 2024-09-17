package org.kingdoms.utils.cache.single;

import org.kingdoms.server.core.Server;

/**
 * A very simple wrapper class that contains a cached object that is cached based on the
 * server ticks. Similar to Caffeine's {@link com.github.benmanes.caffeine.cache.Cache}.
 *
 * @param <T> the type of the cached object.
 */
public class TickedCache<T> implements CacheableObject<T> {
    private T value;
    private final int expirationTicks;
    private int lastUpdateTicks;

    public TickedCache(int expirationTicks) {
        if (expirationTicks <= 0)
            throw new IllegalArgumentException("Expiration ticks cannot be less than 1: " + expirationTicks);
        this.expirationTicks = expirationTicks;
    }

    public boolean hasExpired() {
        return this.value == null || ((Server.get().getTicks() - lastUpdateTicks) >= expirationTicks);
    }

    @Override
    public boolean isCached() {
        return !hasExpired();
    }

    @Override
    public void invalidate() {
        set(null);
    }

    @Override
    public T get() {
        if (hasExpired()) throw new IllegalStateException("Cannot access expired value: " + value);
        return value;
    }

    @Override
    public void set(T value) {
        this.value = value;
        this.lastUpdateTicks = Server.get().getTicks();
    }
}
