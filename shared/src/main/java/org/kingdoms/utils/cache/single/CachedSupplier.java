package org.kingdoms.utils.cache.single;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

public class CachedSupplier<T> implements CacheableObject<T> {
    protected final Supplier<T> supplier;
    protected T cached;
    protected Boolean present;

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
    public void invalidate() {
        this.cached = null;
    }

    @Override
    public boolean isCached() {
        return present;
    }

    public T get() {
        if (cached == null) {
            cached = supplier.get();
            present = cached != null;
        }
        return cached;
    }

    @Override
    public void set(@Nullable T cache) {
        this.cached = cache;
    }
}
