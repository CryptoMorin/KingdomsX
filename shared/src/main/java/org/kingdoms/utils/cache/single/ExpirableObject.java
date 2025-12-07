package org.kingdoms.utils.cache.single;

import org.kingdoms.server.core.Server;

import java.time.Duration;
import java.util.Optional;

public final class ExpirableObject<T> implements CacheableObject<T> {
    private T value;
    private final Duration expirationDuration;
    private int lastUpdateMillis;

    public ExpirableObject(Duration expirationDuration) {
        if (expirationDuration == null || expirationDuration.isNegative() || expirationDuration.isZero())
            throw new IllegalArgumentException("Expiration ticks cannot be less than 1: " + expirationDuration);
        this.expirationDuration = expirationDuration;
    }

    public boolean hasExpired() {
        return this.value == null || (getPassedMillis() >= expirationDuration.toMillis());
    }

    public long getPassedMillis() {
        return System.currentTimeMillis() - lastUpdateMillis;
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
        this.lastUpdateMillis = Server.get().getTicks();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + '(' + expirationDuration + " | " + value + ')';
    }
}
