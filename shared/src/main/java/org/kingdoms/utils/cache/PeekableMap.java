package org.kingdoms.utils.cache;

import java.util.Map;

public interface PeekableMap<K, V> extends Map<K, V> {
    V peek(K key);

    V getIfPresent(K key);
}
