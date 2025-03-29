package org.kingdoms.utils.cache.caffeine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Objects;

public class CacheSet<K> extends AbstractSet<K> {
    private final Cache<K, Boolean> map;

    public CacheSet(Caffeine<K, Boolean> builder) {
        Objects.requireNonNull(builder, "Builder is null");
        this.map = builder.build();
    }

    public boolean add(K key) {
        this.map.put(key, true);
        return true;
    }

    public void clear() {
        map.invalidateAll();
    }

    public boolean contains(Object key) {
        return this.map.getIfPresent(key) != null;
    }

    public boolean remove(Object key) {
        this.map.invalidate(key);
        return true;
    }

    public void cleanUp() {
        map.cleanUp();
    }

    @Override
    public Iterator<K> iterator() {
        return map.asMap().keySet().iterator();
    }

    public int size() {
        return (int) map.estimatedSize();
    }

    public boolean isEmpty() {
        return map.asMap().isEmpty();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + '(' + this.map.asMap().keySet() + ')';
    }
}
