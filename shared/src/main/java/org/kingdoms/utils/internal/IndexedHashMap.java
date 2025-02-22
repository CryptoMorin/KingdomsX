package org.kingdoms.utils.internal;

import org.kingdoms.utils.internal.arrays.UnsafeArrayList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A mix of {@link Map} with {@link List} that provides low-level, unsafe operations for performance.
 */
public class IndexedHashMap<K, V> {
    private final Map<K, V> map = new UnsafeHashMap<>();
    private final UnsafeArrayList<K> list;

    public IndexedHashMap(K[] sample) {
        this.list = UnsafeArrayList.withSize(sample);
    }

    public K[] asArray() {
        return list.getArray();
    }

    public K[] iterator() {
        return list.getArray();
    }

    public V get(K key) {
        return map.get(key);
    }

    public K at(int i) {
        return i < list.size ? list.getArray()[i] : null;
    }

    public V get(K key, V def) {
        return map.getOrDefault(key, def);
    }

    public int size() {
        return list.size;
    }

    public void add(K key, V val) {
        map.put(key, val);
        list.add(key);
    }

    public void set(K[] keys, Function<Integer, V> converter) {
        clear();
        list.setArray(keys);

        for (int i = 0; i < keys.length; i++)
            map.put(keys[i], converter.apply(i));
    }

    /**
     * @param converter Acts as a predicate as well. If returned null, doesn't add it to the list.
     */
    public <U> List<U> subList(final int skip, final int limit, final Function<K, U> converter) {
        if (skip >= list.size) return new ArrayList<>();

        List<U> filtered = new ArrayList<>(limit);
        int count = 0; // Total non-null items converted.
        int index = 0; // Total items iterated.

        while ((count - skip) < limit && index < list.size) {
            // We're starting from the beginning because if we don't, it causes
            // issues for pagination.
            U item = converter.apply(list.getArray()[index++]);
            if (item != null) {
                count++;
                if (count > skip) filtered.add(item);
            }
        }

        return filtered;
    }

    public void clear() {
        map.clear();
        list.clear();
    }
}
