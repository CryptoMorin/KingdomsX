package org.kingdoms.utils.cache.single;

import org.kingdoms.server.core.Server;

import java.util.Optional;

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
        return this.value == null || (getPassedTicks() >= expirationTicks);
    }

    public long getPassedTicks() {
        return Server.get().getTicks() - lastUpdateTicks;
    }

    @Override
    public boolean isCached() {
        return !hasExpired();
    }

    public boolean hasValue() {
        return value != null;
    }

    @Override
    public void invalidate() {
        this.value = null;
    }

    @Override
    public T get() {
        if (hasExpired()) throw new IllegalStateException("Cannot access expired value: " + value);
        return value;
    }

    public Optional<T> getIfNotExpired() {
        if (hasExpired()) return Optional.empty();
        return Optional.of(value);
    }

    @Override
    public void set(T value) {
        this.value = value;
        update();
    }

    public void update() {
        if (this.value == null) throw new IllegalStateException("Cannot update cache when no value is set: " + this);
        this.lastUpdateTicks = Server.get().getTicks();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + '(' + expirationTicks + " | " + value + ')';
    }
}
