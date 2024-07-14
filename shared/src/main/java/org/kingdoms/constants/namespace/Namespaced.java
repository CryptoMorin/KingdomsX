package org.kingdoms.constants.namespace;

import org.checkerframework.checker.nullness.qual.NonNull;

public interface Namespaced {
    @NonNull
    Namespace getNamespace();
}
