package org.kingdoms.utils.internal.iterator;

import java.util.Iterator;
import java.util.stream.Stream;

public final class Iterables {
    private Iterables() {}

    public static <T> java.lang.Iterable<T> of(Iterator<T> iterator) {return () -> iterator;}

    public static <T> java.lang.Iterable<T> of(Stream<T> stream) {return stream::iterator;}
}
