package org.kingdoms.utils.cache.caffeine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Objects;

public class CacheSet<K> {
    private final Cache<K, Boolean> map;

    public CacheSet(Caffeine<K, Boolean> builder) {
        Objects.requireNonNull(builder, "Builder is null");
        this.map = builder.build();
    }

    public void add(K key) {
        this.map.put(key, true);
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

    public boolean isEmpty() {
        return map.asMap().isEmpty();
    }
}
