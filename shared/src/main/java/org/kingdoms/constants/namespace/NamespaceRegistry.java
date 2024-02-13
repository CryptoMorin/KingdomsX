package org.kingdoms.constants.namespace;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Unmodifiable;
import org.kingdoms.utils.internal.nonnull.NonNullMap;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * A simple mapping that associates {@link Namespace}s with {@link V}s
 *
 * @param <V> the value that the {@link Namespace} associates with.
 */
public class NamespaceRegistry<V extends NamespaceContainer> {
    protected final Map<Namespace, V> registry = new NonNullMap<>();

    /**
     * @param value the value to be registered.
     * @throws IllegalArgumentException if this namespace has already been registered before.
     */
    public void register(@NonNull V value) {
        Namespace namespace = value.getNamespace();
        Objects.requireNonNull(namespace, "Cannot register object with null namespace");
        Objects.requireNonNull(value, "Cannot register null object");

        V prev = registry.putIfAbsent(namespace, value);
        if (prev != null) throw new IllegalArgumentException(namespace + " was already registered");
    }

    public V getRegistered(@NonNull Namespace namespace) {
        return registry.get(namespace);
    }

    public boolean isRegisetered(@NonNull Namespace namespace) {
        return registry.containsKey(namespace);
    }

    /**
     * @return an unmodifiable map of the registry.
     */
    public @NonNull @Unmodifiable Map<Namespace, V> getRegistry() {
        return Collections.unmodifiableMap(registry);
    }
}