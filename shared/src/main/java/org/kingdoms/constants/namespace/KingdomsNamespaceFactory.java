package org.kingdoms.constants.namespace;

import org.jetbrains.annotations.NotNull;
import org.kingdoms.utils.internal.functional.Fn;

import java.util.Optional;

public class KingdomsNamespaceFactory<T> implements Namespaced {
    private final Namespace namespace;

    public KingdomsNamespaceFactory(Namespace namespace) {this.namespace = namespace;}

    @Override
    public @NotNull Namespace getNamespace() {
        return namespace;
    }

    public void set(NamespacedMap<? super T> map, T value) {
        map.put(namespace, value);
    }

    public void set(NamespacedMetadataContainer map, T value) {
        this.set(map.getMetadata(), value);
    }

    public Optional<T> get(NamespacedMap<? super T> map) {
        return Fn.cast(Optional.ofNullable(map.get(namespace)));
    }

    public Optional<T> get(NamespacedMetadataContainer map) {
        return this.get(map.getMetadata());
    }

    public boolean has(NamespacedMap<? super T> map) {
        return map.containsKey(namespace);
    }

    public void remove(NamespacedMap<? super T> map) {
        map.remove(namespace);
    }
}
