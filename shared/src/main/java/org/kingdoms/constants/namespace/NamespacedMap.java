package org.kingdoms.constants.namespace;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kingdoms.utils.internal.nonnull.NonNullMap;
import org.kingdoms.utils.internal.string.ObjectPrettyStringFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * A simple mapping that associates {@link Namespace}s with {@link V}s
 *
 * @param <V> the value that the {@link Namespace} associates with.
 * @see NamespacedRegistry
 */
public class NamespacedMap<V> implements Map<Namespace, V> {
    protected final Map<Namespace, V> map;

    public NamespacedMap() {
        this.map = new NonNullMap<>();
    }

    public NamespacedMap(Map<Namespace, V> map) {
        this.map = NonNullMap.of(map);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean contains(Namespaced object) {
        return containsKey(object.getNamespace());
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return map.get(key);
    }

    @Nullable
    public V get(@NotNull Namespaced namespaced) {
        return get(namespaced.getNamespace());
    }

    @Nullable
    @Override
    public V put(Namespace key, V value) {
        return map.put(key, value);
    }

    @Override
    public V remove(Object key) {
        return map.remove(key);
    }

    @Override
    public void putAll(@NotNull Map<? extends Namespace, ? extends V> m) {
        map.putAll(m);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @NotNull
    @Override
    public Set<Namespace> keySet() {
        return map.keySet();
    }

    @NotNull
    @Override
    public Collection<V> values() {
        return map.values();
    }

    @NotNull
    @Override
    public Set<Entry<Namespace, V>> entrySet() {
        return map.entrySet();
    }

    @Override
    public String toString() {
        return ObjectPrettyStringFactory.toDefaultPrettyString(this);
    }
}