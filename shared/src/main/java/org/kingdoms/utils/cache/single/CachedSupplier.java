package org.kingdoms.utils.cache.single;

import java.util.Objects;
import java.util.function.Supplier;

public class CachedSupplier<T> extends CachedObject<T> {
    protected final Supplier<T> supplier;

    /**
     * This is intended for very micro-optimizaiton purposes where the checks of {@link #of(Supplier)}
     * are known be useless.
     */
    public CachedSupplier(Supplier<T> supplier) {
        this.supplier = Objects.requireNonNull(supplier);
    }

    public static <T> CachedSupplier<T> of(Supplier<T> supplier) {
        if (supplier instanceof CachedSupplier) return (CachedSupplier<T>) supplier;
        return new CachedSupplier<>(supplier);
    }

    @Override
    public T get() {
        if (cached == null) {
            cached = supplier.get();
            present = cached != null;
        }
        return cached;
    }
}
