package org.kingdoms.utils.internal.arrays;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.*;
import java.util.function.Consumer;

@SuppressWarnings({"ReturnOfInnerClass", "NullableProblems"})
public final class UnsafeArrayList<E> extends AbstractCollection<E> implements List<E>, RandomAccess {
    private static final int DEFAULT_CAPACITY = 10;
    private transient E[] array;
    public int size;

    private UnsafeArrayList() {}

    private UnsafeArrayList(E[] array) {
        this.array = array;
        this.size = array.length;
    }

    public static <E> UnsafeArrayList<E> withSize(E[] array) {
        UnsafeArrayList<E> list = new UnsafeArrayList<>();
        list.array = array;
        return list;
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <E> UnsafeArrayList<E> of(E... elements) {
        return new UnsafeArrayList<>(elements);
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <E> UnsafeArrayList<E> copyOf(E... elements) {
        return new UnsafeArrayList<>(Arrays.copyOf(elements, elements.length));
    }

    @Override
    public String toString() {
        if (isEmpty()) return "UnsafeArrayList:[]";
        int iMax = size - 1;

        StringBuilder builder = new StringBuilder(20 + (size * 5));
        builder.append("UnsafeArrayList:[");
        for (int i = 0; ; i++) {
            builder.append(array[i]);
            if (i == iMax) return builder.append(']').toString();
            builder.append(", ");
        }
    }

    public void grow() {
        grow(size + 1);
    }

    @Override
    public int size() {
        return size;
    }

    public void resetPointer() {
        this.size = 0;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) != -1;
    }

    @NonNull
    @Override
    public Iterator<E> iterator() {
        return new Itr(0);
    }

    @Override
    public int indexOf(Object object) {
        return indexOfRange(object, 0, size);
    }

    public int indexOfRange(Object object, int start, int end) {
        E[] array = this.array;
        for (; start < end; start++) {
            if (object.equals(array[start])) return start;
        }
        return -1;
    }

    public void setArray(E[] elements) {
        this.array = elements;
        this.size = elements.length;
    }

    /**
     * Warning: Make sure to check for nulls and terminate if you use this.
     */
    public E[] getArray() {
        return this.array;
    }

    @Override
    public int lastIndexOf(Object o) {
        return lastIndexOfRange(o, 0, size);
    }

    @NonNull
    @Override
    public ListIterator<E> listIterator() {
        return listIterator(0);
    }

    @NonNull
    @Override
    public ListIterator<E> listIterator(int index) {
        return new Itr(index);
    }

    @NonNull
    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
    }

    public int lastIndexOfRange(Object object, int start, int end) {
        E[] array = this.array;
        while (--end >= start) {
            if (object.equals(array[end])) return end;
        }
        return -1;
    }

    @Override
    public E[] toArray() {
        return Arrays.copyOf(array, size);
    }

    @SuppressWarnings({"unchecked", "ConstantConditions", "SuspiciousSystemArraycopy"})
    @NonNull
    @Override
    public <T> T[] toArray(@NonNull T[] a) {
        if (a.length < size) return (T[]) Arrays.copyOf(array, size, a.getClass());
        System.arraycopy(array, 0, a, 0, size);
        if (a.length > size) a[size] = null;
        return a;
    }

    @Override
    public E get(int index) {
        return array[index];
    }

    @Override
    public E set(int index, E element) {
        E oldValue = array[index];
        array[index] = Objects.requireNonNull(element, "Cannot add null object");
        return oldValue;
    }

    @Override
    public void add(int index, E element) {
        if (size == array.length) grow();
        System.arraycopy(array, index,
                array, index + 1,
                size - index);
        array[index] = Objects.requireNonNull(element, "Cannot add null object");
        size++;
    }

    @Override
    public E remove(int index) {
        E oldValue = array[index];
        fastRemove(index);
        return oldValue;
    }

    @Override
    public boolean add(E element) {
        if (size == array.length) grow();
        array[size++] = Objects.requireNonNull(element, "Cannot add null object");
        return true;
    }

    public E getLast() {
        if (isEmpty()) throw new NoSuchElementException("Array is empty");
        return array[size - 1];
    }

    public void replaceLast(E element) {
        if (isEmpty()) throw new NoSuchElementException("Array is empty");
        array[size - 1] = element;
    }

    public boolean batchRemove(Collection<?> c, boolean complement, int from, int end) {
        E[] array = this.array;
        for (; ; from++) {
            if (from == end) return false;
            if (c.contains(array[from]) != complement) break;
        }

        int w = from++;
        try {
            for (E e; from < end; from++) {
                if (c.contains(e = array[from]) == complement) array[w++] = e;
            }
        } catch (Throwable ex) {
            System.arraycopy(array, from, array, w, end - from);
            w += end - from;
            throw ex;
        } finally {
            shiftTailOverGap(array, w, end);
        }
        return true;
    }

    public void shiftTailOverGap(Object[] es, int lo, int hi) {
        System.arraycopy(es, hi, es, lo, size - hi);
        for (int to = size, i = (size -= hi - lo); i < to; i++) es[i] = null;
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        E[] array = this.array;
        for (E element : array) hashCode = 31 * hashCode + element.hashCode();
        return hashCode;
    }

    @Override
    public boolean remove(Object obj) {
        E[] array = this.array;
        for (int i = 0; i < size; i++) {
            if (obj.equals(array[i])) {
                fastRemove(i);
                return true;
            }
        }
        return false;
    }

    public void fastRemove(int i) {
        // Shift the array backwards if the element wasn't removed from the head.
        if (--size > i) System.arraycopy(array, i + 1, array, i, size - i);
        array[size] = null;
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> collection) {
        for (Object e : collection)
            if (!contains(e))
                return false;
        return true;
    }

    @Override
    public void clear() {
        Arrays.fill(array, null);
        size = 0;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean addAll(Collection<? extends E> c) {
        return addAll((E[]) c.toArray(new Object[0]));
    }

    public boolean addAll(final E[] e) {
        final int len = e.length;
        if (len == 0) return false;

        //  java.lang.ArrayIndexOutOfBoundsException: arraycopy: last destination index 11 out of bounds for object array[10]
        if (size + len > array.length) grow(size + len);
        System.arraycopy(e, 0, array, size, len);
        size += len;
        return true;
    }

    public void grow(int minCapacity) {
        this.array = Arrays.copyOf(array, newCapacity(minCapacity));
    }

    public int newCapacity(int minCapacity) {
        int currentCapacity = array.length;
        int newCapacity = currentCapacity + (currentCapacity >> 1);
        if (newCapacity - minCapacity <= 0) {
            if (currentCapacity == 0) return Math.max(DEFAULT_CAPACITY, minCapacity);
            if (minCapacity < 0) throw new OutOfMemoryError();
            return minCapacity;
        }
        return newCapacity;
    }

    @SuppressWarnings("SuspiciousSystemArraycopy")
    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        Object[] a = c.toArray();
        int numNew = a.length;
        if (numNew == 0) return false;
        if (numNew > array.length - size) grow(size + numNew);

        int numMoved = size - index;
        if (numMoved > 0)
            System.arraycopy(array, index,
                    array, index + numNew,
                    numMoved);
        System.arraycopy(a, 0, array, index, numNew);
        size += numNew;
        return true;
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> c) {
        return batchRemove(c, false, 0, size);
    }

    @Override
    public boolean retainAll(@NonNull Collection<?> c) {
        return batchRemove(c, true, 0, size);
    }

    @Override
    public void forEach(@NonNull Consumer<? super E> action) {
        for (E element : array) action.accept(element);
    }

    private class Itr implements ListIterator<E> {
        public int cursor;       // index of next element to return

        // prevent creating a synthetic constructor
        Itr(int cursor) {
            this.cursor = cursor;
        }

        @Override
        public boolean hasNext() {
            return cursor != size;
        }

        @Override
        public E next() {
            return array[cursor++];
        }

        @Override
        public void remove() {
            UnsafeArrayList.this.remove(cursor - 1);
        }

        @Override
        public void forEachRemaining(Consumer<? super E> action) {
            int size = UnsafeArrayList.this.size;
            if (cursor < size) {
                while (cursor < size) action.accept(array[cursor++]);
            }
        }

        @Override
        public boolean hasPrevious() {
            return cursor != 0;
        }

        @Override
        public int nextIndex() {
            return cursor;
        }

        @Override
        public int previousIndex() {
            return cursor - 1;
        }

        @Override
        public E previous() {
            return array[--cursor];
        }

        @Override
        public void set(E e) {
            UnsafeArrayList.this.set(cursor - 1, e);
        }

        @Override
        public void add(E e) {
            UnsafeArrayList.this.add(cursor++, e);
        }
    }
}
