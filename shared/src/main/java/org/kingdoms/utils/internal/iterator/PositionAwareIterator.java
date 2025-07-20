package org.kingdoms.utils.internal.iterator;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;

public class PositionAwareIterator<E> implements Iterator<PositionAwareObject<E>>, Iterable<PositionAwareObject<E>> {
    protected final Collection<E> collection;
    protected final Iterator<E> iterator;
    protected int index;

    public PositionAwareIterator(Collection<E> collection, Iterator<E> iterator) {
        this.collection = collection;
        this.iterator = iterator;
    }

    public static <E> PositionAwareIterator<E> of(Collection<E> collection) {
        return new PositionAwareIterator<>(collection, collection.iterator());
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public PositionAwareObject<E> next() {
        index++;
        return new PositionAwareObject<>(this, iterator.next());
    }

    @NotNull
    @Override
    public PositionAwareIterator<E> iterator() {
        return of(collection);
    }
}
