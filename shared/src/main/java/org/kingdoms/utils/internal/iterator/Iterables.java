package org.kingdoms.utils.internal.iterator;

import org.kingdoms.utils.internal.arrays.ArrayIterator;

import java.util.*;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Stream;

public final class Iterables {
    private Iterables() {}

    public static <T> Iterator<T> ofIterator(T[] array) {return ArrayIterator.of(array);}

    public static <T> java.lang.Iterable<T> of(Iterator<T> iterator) {return () -> iterator;}

    public static <T> java.lang.Iterable<T> of(Stream<T> stream) {return stream::iterator;}

    public static <T> List<T> getOrCollectList(Iterable<T> iterator) {
        if (iterator instanceof List) return (List<T>) iterator;
        if (iterator instanceof Collection) return new ArrayList<>((Collection<T>) iterator);
        return collect(iterator.iterator(), new ArrayList<>());
    }

    public static <T, C extends Collection<T>> C collect(Iterator<T> from, C to) {
        while (from.hasNext()) {
            to.add(from.next());
        }
        return to;
    }

    public static <T> void chooseRandom(Iterator<T> items, Function<T, Boolean> fromList) {
        List<T> shuffled = Iterables.collect(items, new ArrayList<>());
        Collections.shuffle(shuffled);

        for (T item : shuffled) {
            if (fromList.apply(item)) return;
        }
    }

    public static <T, C extends Collection<T>> C collectAny(int elements, Collection<T> collectFrom, C collectTo) {
        if (elements < 0) throw new IllegalArgumentException("Cannot collect negative elements: " + elements);
        if (elements == 0) return collectTo;

        Iterator<T> iter = collectFrom.iterator();

        while (elements-- > 0 && iter.hasNext()) {
            collectTo.add(iter.next());
        }

        return collectTo;
    }

    public static <T, C extends Collection<T>> C removeAndCollect(int elements, Collection<T> removeFrom, C collectTo) {
        if (elements < 0) throw new IllegalArgumentException("Cannot collect negative elements: " + elements);
        if (elements == 0) return collectTo;

        Iterator<T> iter = removeFrom.iterator();

        while (elements-- > 0 && iter.hasNext()) {
            collectTo.add(iter.next());
            iter.remove();
        }

        return collectTo;
    }
}
