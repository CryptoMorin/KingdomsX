package org.kingdoms.utils.cache.caffeine;

import com.github.benmanes.caffeine.cache.LoadingCache;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kingdoms.utils.cache.PeekableMap;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class CaffeineWrapper<K, V> implements PeekableMap<K, V> {
    private final LoadingCache<K, V> cache;

    public CaffeineWrapper(LoadingCache<K, V> cache) {
        this.cache = cache;
    }

    @Override
    public int size() {
        cache.cleanUp();
        return (int) cache.estimatedSize();
    }

    @Override
    public boolean isEmpty() {
        return size() != 0;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean containsKey(Object key) {
        return cache.getIfPresent((K) key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    @Override
    public V get(Object key) {
        return cache.get((K) key);
    }

    @Override
    public Map<K, V> loadAll(Iterable<? extends K> keys, Function<Iterable<? extends K>, Map<K, V>> mappingFunction) {
        return cache.getAll(keys, mappingFunction);
    }

    @Nullable
    @Override
    public V put(K key, V value) {
        cache.put(key, value);
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public V remove(Object key) {
        cache.invalidate((K) key);
        return null;
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) {
        cache.putAll(m);
    }

    @Override
    public void clear() {
        cache.invalidateAll();
    }

    @NotNull
    @Override
    public Set<K> keySet() {
        return cache.asMap().keySet();
    }

    @NotNull
    @Override
    public Collection<V> values() {
        return cache.asMap().values();
    }

    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        return cache.asMap().entrySet();
    }

    @Override
    public V peek(K key) {
        return cache.getIfPresent(key);
    }

    @Override
    public V getIfPresent(K key) {
        return cache.getIfPresent(key);
    }
}
