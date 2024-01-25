package org.kingdoms.utils.internal.iterator;

import java.util.Iterator;
import java.util.function.Consumer;

public class SubIterator<E> implements Iterator<E> {
    private final Iterator<E> iterator;
    private final int limit;
    private int counted;

    public SubIterator(Iterator<E> iterator, int limit) {
        this.iterator = iterator;
        this.limit = limit;
    }

    public void resetIndex() {
        this.counted = 0;
    }

    @Override
    public boolean hasNext() {
        return counted < limit && iterator.hasNext();
    }

    @Override
    public E next() {
        counted++;
        return iterator.next();
    }

    @Override
    public void remove() {
        iterator.remove();
    }

    @Override
    public void forEachRemaining(Consumer<? super E> action) {
        iterator.forEachRemaining(action);
    }
}
