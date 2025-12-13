package org.kingdoms.utils.cache;

import com.github.benmanes.caffeine.cache.CacheLoader;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

public final class JavaMapWrapper<K, V> implements CachingMap<K, V> {
    private final ConcurrentHashMap<K, V> cache;
    private final AllKnowingCacheLoader<K, V> loader;

    public JavaMapWrapper(ConcurrentHashMap<K, V> cache, AllKnowingCacheLoader<K, V> loader) {
        this.cache = cache;
        this.loader = loader;
    }

    @Override
    public int size() {
        return cache.size();
    }

    @Override
    public boolean isEmpty() {
        return cache.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return cache.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    @Override
    public V get(Object key) {
        return cache.computeIfAbsent((K) key, k -> {
            try {
                return loader.load(k);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Nullable
    @Override
    public V put(K key, V value) {
        cache.put(key, value);
        return null;
    }

    @Override
    public V remove(Object key) {
        cache.remove(key);
        return null;
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) {
        cache.putAll(m);
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @NotNull
    @Override
    public Set<K> keySet() {
        return cache.keySet();
    }

    @NotNull
    @Override
    public Collection<V> values() {
        return cache.values();
    }

    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        return cache.entrySet();
    }

    @Override
    public V getIfPresent(K key) {
        return cache.get(key);
    }

    @Override
    public Map<K, V> getAll(Iterable<? extends K> keys) {
        Map<K, V> finalProduct = new HashMap<>();
        Set<K> uncachedKeys = new HashSet<>();

        for (K key : keys) {
            V cached = get(key);
            if (cached != null) {
                finalProduct.put(key, cached);
            } else if (!loader.shouldCacheExistence() || loader.doesntExistCache().getIfPresent(key) == null) {
                uncachedKeys.add(key);
            }
        }

        if (!uncachedKeys.isEmpty()) {
            try {
                // TODO values that don't exist aren't loaded into doesntExistCache()
                Map<@NonNull K, @NonNull V> loaded = loader.loadAll(uncachedKeys);
                finalProduct.putAll(loaded);
                cache.putAll(loaded);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return finalProduct;
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable V replace(K key, V value) {
        throw new UnsupportedOperationException();
    }
}
