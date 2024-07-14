package org.kingdoms.utils.internal.arrays;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.common.value.qual.IntRange;
import org.checkerframework.dataflow.qual.Pure;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

public final class ArrayUtils {
    @Pure
    public static <T> T[] reverse(T[] array) {
        for (int i = 0, j = array.length - 1; j > i; i++, j--) {
            T tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
        }
        return array;
    }

    public static <T> T[] require(T[] array, Predicate<T> requirement) {
        for (T element : array) {
            if (!requirement.test(element)) {
                throw new IllegalStateException("Array element did not pass requirement");
            }
        }
        return array;
    }

    public static int sizeOfIterator(Iterator<?> iterator) {
        int count = 0;
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }
        return count;
    }

    public static <T> T getLast(List<T> list) {
        return list.isEmpty() ? null : list.get(list.size() - 1);
    }

    public static String[] mergeObjects(Object... objects) {
        List<String> list = new ArrayList<>(objects.length * 2);
        for (Object object : objects) {
            if (object instanceof Object[]) {
                for (Object o : ((Object[]) object)) {
                    list.add(o.toString());
                }
            } else list.add(object.toString());
        }

        return list.toArray(new String[0]);
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] merge(T[] array1, T[] array2) {
        T[] joinedArray = (T[]) Array.newInstance(array1.getClass().getComponentType(), array1.length + array2.length);
        System.arraycopy(array1, 0, joinedArray, 0, array1.length);
        System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
    }

    public static Object[] shift(Object[] array) {
        Object[] result = new Object[array.length - 1];
        System.arraycopy(array, 1, result, 0, result.length);
        return result;
    }

    public static <T> ConditionalBuilder<T> when(boolean cond, T item) {
        return new ConditionalBuilder<T>(new ArrayList<>()).when(cond, item);
    }

    public static final class ConditionalBuilder<T> {
        protected final Collection<T> collection;

        private ConditionalBuilder(Collection<T> collection) {
            this.collection = collection;
        }

        public ConditionalBuilder<T> when(boolean cond, T item) {
            if (cond) collection.add(item);
            return this;
        }

        public Collection<T> build() {
            return collection;
        }

        public T[] toArray(T[] a) {
            return collection.toArray(a);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] malloc(@NonNull T[] initial, @IntRange(from = 1, to = Integer.MAX_VALUE) int length) {
        if (length <= initial.length) throw new IllegalArgumentException(
                "Cannot allocate array with the same or smaller size than the initial array " + length + " <= " + initial.length);

        T[] arr = (T[]) Array.newInstance(initial.getClass().getComponentType(), length);
        System.arraycopy(initial, 0, arr, 0, initial.length);
        return arr;
    }
}
