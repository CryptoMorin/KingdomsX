package org.kingdoms.utils.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class MapUtil {
    public static <K> Set<K> clone(Set<K> original, Set<K> other) {
        other.addAll(original);
        return other;
    }

    public static <K, V> Map<K, V> clone(Map<K, V> original, BiFunction<K, V, V> handler) {
        Map<K, V> cloned = new HashMap<>(original.size());

        for (Map.Entry<K, V> entry : original.entrySet()) {
            cloned.put(entry.getKey(), handler.apply(entry.getKey(), entry.getValue()));
        }

        return cloned;
    }

    public static <K, V, U> Map<K, V> toHashMap(V value, Collection<U> values, Function<U, K> keyResolver) {
        Map<K, V> map = new HashMap<>(values.size());
        for (U item : values) map.put(keyResolver.apply(item), value);
        return map;
    }
}
