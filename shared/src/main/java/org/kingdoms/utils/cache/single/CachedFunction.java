package org.kingdoms.utils.cache.single;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public class CachedFunction<I, O> extends CachedObject<O> implements Function<I, O> {
    protected final Function<I, O> function;

    public CachedFunction(Function<I, O> function) {
        this.function = Objects.requireNonNull(function);
    }

    public static <I, O> CachedFunction<I, O> of(Function<I, O> function) {
        if (function instanceof CachedFunction) return (CachedFunction<I, O>) function;
        return new CachedFunction<>(function);
    }

    @Override
    public O apply(I input) {
        if (cached == null) {
            cached = function.apply(input);
            present = cached != null;
        }
        return cached;
    }
}
