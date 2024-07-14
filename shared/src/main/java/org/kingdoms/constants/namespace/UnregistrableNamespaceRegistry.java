package org.kingdoms.constants.namespace;

public interface UnregistrableNamespaceRegistry<V extends Namespaced> {
    V unregister(Namespace namespace);
}