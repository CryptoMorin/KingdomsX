package org.kingdoms.utils.internal.iterator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public final class Iterables {
    private Iterables() {}

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
}
