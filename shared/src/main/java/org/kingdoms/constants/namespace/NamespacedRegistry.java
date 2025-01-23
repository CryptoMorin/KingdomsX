package org.kingdoms.constants.namespace;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Unmodifiable;
import org.kingdoms.utils.internal.nonnull.NonNullMap;

import java.util.*;

/**
 * A simple mapping that associates {@link Namespace}s with {@link V}s which is intended to be
 * used as a global registry and not merely a data holder. (For that purpose use {@link NamespacedMap} instead.)
 *
 * @param <V> the value that the {@link Namespace} associates with.
 * @see NamespacedMap
 */
public class NamespacedRegistry<V extends Namespaced> {
    protected final Map<Namespace, V> registry;

    public NamespacedRegistry() {
        this.registry = NonNullMap.of(new HashMap<>());
    }

    public NamespacedRegistry(Map<Namespace, V> registry) {
        this.registry = registry;
    }

    public static <V extends Namespaced> NamespacedRegistry<V> ordered() {
        return new NamespacedRegistry<>(NonNullMap.of(new LinkedHashMap<>()));
    }

    /**
     * @param value the value to be registered.
     * @throws IllegalArgumentException if this namespace has already been registered before.
     */
    public void register(@NonNull V value) {
        register(value, false);
    }

    private void register(@NonNull V value, boolean replace) {
        Namespace namespace = value.getNamespace();
        Objects.requireNonNull(namespace, "Cannot register object with null namespace");
        Objects.requireNonNull(value, "Cannot register null object");

        if (replace) {
            registry.put(namespace, value);
        } else {
            V prev = registry.putIfAbsent(namespace, value);
            if (prev != null) throw new IllegalArgumentException(namespace + " was already registered");
        }
    }

    public void replace(V value) {
        register(value, true);
    }

    public V getRegistered(@NonNull Namespace namespace) {
        return registry.get(namespace);
    }

    public V getRegistered(@NonNull V namespaced) {
        return registry.get(namespaced.getNamespace());
    }

    public boolean isRegisetered(@NonNull V namespaced) {
        return registry.containsKey(namespaced.getNamespace());
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