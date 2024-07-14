package org.kingdoms.data;

import java.util.Map;

public final class Pair<K, V> implements Map.Entry<K, V> {
    private K key;
    private V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public static <K, V> Pair<K, V> of(K key, V value) {
        return new Pair<>(key, value);
    }

    public static <K, V> Pair<K, V> of(Map.Entry<K, V> entry) {
        return new Pair<>(entry.getKey(), entry.getValue());
    }

    public static <K, V> Pair<K, V> empty() {
        return new Pair<>(null, null);
    }

    @Override
    public K getKey() {
        return key;
    }

    public void setKey(K key) {
        this.key = key;
    }

    @Override
    public V getValue() {
        return value;
    }

    @Override
    public V setValue(V value) {
        return this.value = value;
    }

    public boolean isKeyPresent() {
        return key != null;
    }

    public boolean isValuePresent() {
        return value != null;
    }

    public boolean areBothPresent() {
        return isKeyPresent() && isValuePresent();
    }

    public boolean areBothNull() {
        return !isKeyPresent() && !isValuePresent();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Map.Entry)) return false;
        Map.Entry<?, ?> entry = (Map.Entry<?, ?>) obj;

        return (key == null ? entry.getKey() == null : key.equals(entry.getKey())) &&
                (value == null ? entry.getValue() == null : value.equals(entry.getValue()));
    }

    @Override
    public int hashCode() {
        return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
    }

    @Override
    public String toString() {
        return "Pair{" + key + " | " + value + '}';
    }
}
