package org.kingdoms.utils.cache.caffeine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.Objects;

public class ExpirableSet<K> {
    private final ExpirableMap<K, Long> map;
    private final long duration;

    public ExpirableSet(ExpirationStrategy expirationStrategy) {
        Objects.requireNonNull(expirationStrategy, "Expiration etrategies cannot be null");
        this.map = new ExpirableMap<>(expirationStrategy);
        this.duration = map.getDefaultExpirationStrategy().getExpiryAfterCreate().toMillis();
    }

    public void add(K key) {
        this.map.put(key, System.currentTimeMillis());
    }

    public Duration getTimeLeft(K key) {
        Long added = this.map.getIfPresent(key);
        if (added == null) return Duration.ZERO;

        long passed = System.currentTimeMillis() - added;
        long left = duration - passed;

        return left <= 0 ? Duration.ZERO : Duration.ofMillis(left);
    }

    public void clear() {
        map.invalidateAll();
    }

    public boolean contains(K key) {
        return this.map.getIfPresent(key) != null;
    }

    public void remove(K key) {
        this.map.invalidate(key);
    }

    public void cleanUp() {
        map.cleanUp();
    }
}
