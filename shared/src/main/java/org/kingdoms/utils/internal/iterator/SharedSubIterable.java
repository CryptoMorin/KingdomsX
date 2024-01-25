package org.kingdoms.utils.internal.iterator;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public class SharedSubIterable<E> implements Iterable<E> {
    private final SubIterator<E> subIterable;

    public SharedSubIterable(Iterable<E> iterable, int limit) {
        this.subIterable = new SubIterator<>(iterable.iterator(), limit);
    }

    public SubIterator<E> getSharedIterator() {
        return subIterable;
    }

    public boolean hasRemaining() {
        return subIterable.hasNext();
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        return subIterable;
    }
}
