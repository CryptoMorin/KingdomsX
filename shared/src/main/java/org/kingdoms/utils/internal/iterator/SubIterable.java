package org.kingdoms.utils.internal.iterator;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public class SubIterable<E> implements Iterable<E> {
    private final Iterable<E> iterable;
    private final int limit;

    public SubIterable(Iterable<E> iterable, int limit) {
        this.iterable = iterable;
        this.limit = limit;
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        return new SubIterator<>(iterable.iterator(), limit);
    }
}
