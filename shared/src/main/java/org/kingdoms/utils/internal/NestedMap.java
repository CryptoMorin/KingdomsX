package org.kingdoms.utils.internal;

import java.util.Map;
import java.util.Objects;

class NestedMap<K, V> {
    private Map<K, Object> child;
    private V value;

    public NestedMap() {
        this.child = null;
        this.value = null;
    }

    public boolean isTypeKnown() {
        return this.child != null || this.value != null;
    }

    public boolean hasChildren() {
        return this.child != null;
    }

    public NestedMap(Map<K, Object> child) {
        this.child = Objects.requireNonNull(child);
        this.value = null;
    }

    public NestedMap(V value) {
        this.child = null;
        this.value = value;
    }

    public NestedMap<K, V> getChild() {
        assertType();
        if (!hasChildren()) throw new IllegalStateException("This map isn't nested");
        return Fn.cast(this.child);
    }

    public void setChild() {
        setChild(new NestedMap<>());
    }

    public void setChild(NestedMap<K, V> child) {
        this.child = Fn.cast(child);
    }

    private void assertType() {
        if (!isTypeKnown()) throw new IllegalStateException("This map has no known type to get its values");
    }

    public V getValue() {
        assertType();
        if (hasChildren()) throw new IllegalStateException("This map has more nested children");
        return value;
    }

    public void setValue(V v) {
        if (!isTypeKnown() || hasChildren()) throw new IllegalStateException("This map has more nested children");
        this.value = v;
    }
}