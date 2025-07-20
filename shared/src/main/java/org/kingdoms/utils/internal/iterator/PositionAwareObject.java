package org.kingdoms.utils.internal.iterator;

public class PositionAwareObject<E> {
    private final PositionAwareIterator<E> origin;
    private final E object;

    public PositionAwareObject(PositionAwareIterator<E> origin, E object) {
        this.origin = origin;
        this.object = object;
    }

    public E getObject() {
        return object;
    }

    public boolean isFirst() {
        return origin.index == 0;
    }

    public boolean isLast() {
        return origin.index == origin.collection.size();
    }

    public boolean hasNext() {
        return !isLast();
    }
}
