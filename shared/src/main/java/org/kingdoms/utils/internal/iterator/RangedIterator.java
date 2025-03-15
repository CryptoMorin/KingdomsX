package org.kingdoms.utils.internal.iterator;

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class RangedIterator<E> implements Iterator<E> {
    private final Iterator<E> iterator;
    private final int limit;
    private int totalSkip;
    private Boolean cachedHasNext;
    private int skip;
    private int counted;

    public RangedIterator(Iterator<E> iterator, int skip, int limit) {
        this.iterator = iterator;
        skip(skip);

        if (limit < 0) {
            throw new IllegalArgumentException("Negative number for limit is not allowed, " +
                    "consider using Integer.MAX_VALUE instead for unlimited: " + limit);
        }

        this.limit = limit;
    }

    public static <T> Iterator<T> skipped(Iterator<T> iterator, int skip) {
        if (skip == 0) return iterator;
        return new RangedIterator<>(iterator, skip, Integer.MAX_VALUE);
    }

    public void resetIndex() {
        this.counted = 0;
    }

    public RangedIterator<E> skip(int elements) {
        // Not immediately skipped for lazy evaluation.
        if (elements < 0) throw new IllegalArgumentException("Cannot skip negative elements: " + elements);
        this.totalSkip = this.skip += elements;
        return this;
    }

    private void skip() {
        while (iterator.hasNext() && skip > 0) {
            skip--;
            iterator.next();
        }
    }

    @Override
    public boolean hasNext() {
        if (cachedHasNext != null) return cachedHasNext;
        skip();
        return cachedHasNext = counted < limit && iterator.hasNext();
    }

    @Override
    public E next() {
        if (!hasNext()) throw new NoSuchElementException("Iterator already reached the end: " + this);
        counted++;
        cachedHasNext = null;
        return iterator.next();
    }

    @Override
    public void remove() {
        iterator.remove();
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object obj) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return "SubIterator{" +
                "iterator=" + iterator +
                ", skip=" + totalSkip +
                ", limit=" + limit +
                ", counted=" + counted +
                '}';
    }
}
