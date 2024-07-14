package org.kingdoms.utils.internal.iterator;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public class SharedSubIterable<E> implements Iterable<E> {
    private final RangedIterator<E> subIterable;

    public SharedSubIterable(Iterable<E> iterable, int limit) {
        this.subIterable = new RangedIterator<>(iterable.iterator(), 0, limit);
    }

    public RangedIterator<E> getSharedIterator() {
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
