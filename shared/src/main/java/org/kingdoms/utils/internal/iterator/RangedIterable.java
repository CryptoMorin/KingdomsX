package org.kingdoms.utils.internal.iterator;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public class RangedIterable<E> implements Iterable<E> {
    private final Iterable<E> iterable;
    private final int limit, skip;

    public RangedIterable(Iterable<E> iterable, int skip, int limit) {
        this.iterable = iterable;
        this.limit = limit;
        this.skip = skip;
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        return new RangedIterator<>(iterable.iterator(), skip, limit);
    }
}
