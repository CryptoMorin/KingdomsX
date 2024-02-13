package org.kingdoms.constants.namespace;

public interface UnregistrableNamespaceRegistry<V extends NamespaceContainer> {
    V unregister(Namespace namespace);
}