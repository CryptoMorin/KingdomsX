package org.kingdoms.utils.internal.iterator;

import java.util.ArrayList;
import java.util.Collection;

public final class Collections {
    private Collections() {}

    /**
     * Optimizes both CPU and memory usage when using this collection.
     * This should not be used on a {@link java.util.Set}.
     */
    public static <T> Collection<T> optimize(Collection<T> collection) {
        if (collection.isEmpty()) return java.util.Collections.emptyList();
        if (collection.size() == 1) return java.util.Collections.singletonList(collection.iterator().next());
        else return new ArrayList<>(collection);
    }
}
