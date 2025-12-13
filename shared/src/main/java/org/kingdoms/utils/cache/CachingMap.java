package org.kingdoms.utils.cache;

import java.util.Map;

public interface CachingMap<K, V> extends Map<K, V> {
    V getIfPresent(K key);

    Map<K, V> getAll(Iterable<? extends K> keys);
}
