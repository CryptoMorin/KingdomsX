package org.kingdoms.utils.internal.enumeration;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.*;

/**
 * Extremely memory inefficient yet fast enum set.
 */
public class QuickEnumSet<E extends Enum<E>> extends AbstractSet<E> {
    private final transient E[] universe;
    private final transient boolean[] elements;
    private transient int size;
    private transient int modCount;

    public QuickEnumSet(E[] universe) {
        this.universe = universe;
        elements = new boolean[universe.length];
    }

    public static <E extends Enum<E>> QuickEnumSet<E> allOf(E[] universe) {
        QuickEnumSet<E> set = new QuickEnumSet<>(universe);
        set.size = universe.length;
        for (int i = 0; i < universe.length; i++) set.elements[i] = true;
        return set;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean contains(Object o) {
        return elements[((Enum<?>) o).ordinal()];
    }

    @SuppressWarnings("ReturnOfInnerClass")
    @NonNull
    @Override
    public Iterator<E> iterator() {
        return new EnumSetIterator();
    }

    @NonNull
    @Override
    public Object[] toArray() {
        Object[] array = new Object[size];
        int index = 0;
        for (E element : this) {
            array[index++] = element;
        }
        return array;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    @Override
    public <T> T[] toArray(@NonNull T[] a) {
        T[] array = a.length >= size ? a : (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
        int index = 0;
        for (E element : this) {
            array[index++] = (T) element;
        }
        return array;
    }

    @Override
    public boolean add(E e) {
        int ordinal = e.ordinal();
        boolean contained = elements[ordinal];
        if (contained) return false;

        elements[ordinal] = true;
        modCount++;
        size++;

        return true;
    }

    @Override
    public boolean remove(Object o) {
        int ordinal = ((Enum<?>) o).ordinal();
        boolean contained = elements[ordinal];
        if (contained) {
            elements[ordinal] = false;
            modCount++;
            size--;
        }
        return contained;
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> c) {
        @SuppressWarnings("unchecked") Collection<E> e = (Collection<E>) c;
        for (E a : e) {
            if (!elements[a.ordinal()]) return false;
        }
        return true;
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends E> c) {
        modCount++;
        boolean changed = false;
        for (E e : c) {
            boolean contained = elements[e.ordinal()];
            if (!contained) {
                changed = true;
                elements[e.ordinal()] = true;
                size++;
            }
        }
        return changed;
    }

    @SafeVarargs
    public final QuickEnumSet<E> addAll(@NonNull E... enums) {
        for (E e : enums) elements[e.ordinal()] = true;
        return this;
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> c) {
        modCount++;
        boolean changed = false;
        @SuppressWarnings("unchecked") Collection<E> ce = (Collection<E>) c;
        for (E e : ce) {
            boolean contained = elements[e.ordinal()];
            if (contained) {
                changed = true;
                elements[e.ordinal()] = false;
                size--;
            }
        }
        return changed;
    }

    @Override
    public void clear() {
        Arrays.fill(elements, false);
        modCount++;
        size = 0;
    }

    private final class EnumSetIterator implements Iterator<E> {
        private int cursor;
        private int iterModCount = modCount;

        @Override
        public boolean hasNext() {
            checkModCount();
            while (true) {
                if (cursor == elements.length) return false;
                if (elements[cursor]) return true;
                cursor++;
            }
        }

        @Override
        public E next() {
            if (!hasNext()) throw new NoSuchElementException("Size=" + size);
            return universe[cursor++];
        }

        void checkModCount() {
            if (iterModCount != modCount) throw new ConcurrentModificationException();
        }

        @Override
        public void remove() {
            checkModCount();
            if (elements[cursor - 1]) {
                elements[cursor - 1] = false;
                iterModCount = ++modCount;
                size--;
            }
        }
    }
}
