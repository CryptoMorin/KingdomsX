package org.kingdoms.utils.cache;

import java.util.Map;
import java.util.function.Function;

public interface PeekableMap<K, V> extends Map<K, V> {
    V peek(K key);

    V getIfPresent(K key);

    Map<K, V> loadAll(Iterable<? extends K> keys, Function<Iterable<? extends K>, Map<K, V>> mappingFunction);
}
