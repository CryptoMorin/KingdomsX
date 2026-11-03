package org.kingdoms.utils.internal.iterator;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.*;


import java.util.AbstractSequentialList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Comparator;


import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public final class UnmodifiableLinkedList<E> extends LinkedList<E> {
    private UnmodifiableLinkedList(Collection<? extends E> source) {
        // We use the indexed version because the normal one calls it and
        // that'd cause an unmodifiable exception.
        super.addAll(0, Objects.requireNonNull(source));
    }

    public static <T> UnmodifiableLinkedList<T> of(LinkedList<T> linkedList) {
        return new UnmodifiableLinkedList<>(linkedList);
    }

    private UnsupportedOperationException immutable() {
        return new UnsupportedOperationException("This LinkedList is unmodifiable");
    }

    @Override public boolean add(E e) {
        throw immutable();
    }

    @Override public void addFirst(E e) {
        throw immutable();
    }

    @Override public void addLast(E e) {
        throw immutable();
    }

    @Override public void add(int index, E e) {
        throw immutable();
    }

    @Override public boolean addAll(Collection<? extends E> c) {
        throw immutable();
    }

    @Override public boolean addAll(int index, Collection<? extends E> c) {
        throw immutable();
    }

    @Override public E set(int index, Object e) {
        throw immutable();
    }

    @Override public E remove() {
        throw immutable();
    }

    @Override public E remove(int index) {
        throw immutable();
    }

    @Override public boolean remove(Object o) {
        throw immutable();
    }

    @Override public E removeFirst() {
        throw immutable();
    }

    @Override public E removeLast() {
        throw immutable();
    }

    @Override public boolean removeFirstOccurrence(Object o) {
        throw immutable();
    }

    @Override public boolean removeLastOccurrence(Object o) {
        throw immutable();
    }

    @Override public boolean removeAll(Collection<?> c) {
        throw immutable();
    }

    @Override public boolean retainAll(Collection<?> c) {
        throw immutable();
    }

    @Override public boolean removeIf(Predicate<? super E> filter) {
        throw immutable();
    }

    @Override public void replaceAll(UnaryOperator<E> operator) {
        throw immutable();
    }

    @Override public void sort(Comparator<? super E> c) {
        throw immutable();
    }

    @Override public void clear() {
        throw immutable();
    }

    @Override public boolean offer(E e) {
        throw immutable();
    }

    @Override public boolean offerFirst(E e) {
        throw immutable();
    }

    @Override public boolean offerLast(E e) {
        throw immutable();
    }

    @Override public void push(E e) {
        throw immutable();
    }

    @Override public E pop() {
        throw immutable();
    }

    @Override public Iterator<E> iterator() {
        return readOnly(super.iterator());
    }

    @Override public Iterator<E> descendingIterator() {
        return readOnly(super.descendingIterator());
    }

    @Override public ListIterator<E> listIterator() {
        return readOnly(super.listIterator());
    }

    @Override public ListIterator<E> listIterator(int index) {
        return readOnly(super.listIterator(index));
    }

    private <T> Iterator<T> readOnly(Iterator<T> it) {
        return new Iterator<T>() {
            public boolean hasNext() { return it.hasNext(); }
            public T next() { return it.next(); }
            public void forEachRemaining(java.util.function.Consumer<? super T> action) {
                it.forEachRemaining(action);
            }
            public void remove() { throw immutable(); }
        };
    }

    private <T> ListIterator<T> readOnly(ListIterator<T> it) {
        return new ListIterator<T>() {
            public boolean hasNext() { return it.hasNext(); }
            public T next() { return it.next(); }
            public boolean hasPrevious() { return it.hasPrevious(); }
            public T previous() { return it.previous(); }
            public int nextIndex() { return it.nextIndex(); }
            public int previousIndex() { return it.previousIndex(); }
            public void remove() { throw immutable(); }
            public void set(Object e) { throw immutable(); }
            public void add(Object e) { throw immutable(); }
        };
    }

    @Override public List<E> subList(int from, int to) {
        return java.util.Collections.unmodifiableList(
                super.subList(from, to)
        );
    }
}
