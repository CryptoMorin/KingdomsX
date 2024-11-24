package org.kingdoms.utils.cache.single;

import java.time.Duration;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ExpirableObject {
    private ExpirableObject() {}

    public static <I, O> ExpirableCachedFunction<I, O> fn(Duration expiry, Function<I, O> function) {
        return of(expiry, function);
    }

    public static <I, O> ExpirableCachedFunction<I, O> of(Duration expiry, Function<I, O> function) {
        return new ExpirableCachedFunction<>(expiry, function);
    }

    public static <T> ExpirableCachedSupplier<T> of(Duration expiry, Supplier<T> supplier) {
        return new ExpirableCachedSupplier<>(expiry, supplier);
    }
}
