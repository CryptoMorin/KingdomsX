package org.kingdoms.utils.cache.single;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

public class CachedObject<T> implements CacheableObject<T> {
    protected T cached;
    protected Boolean present;

    @Override
    public void invalidate() {
        this.cached = null;
    }

    @Override
    public boolean isCached() {
        return present;
    }

    public T get() {
        return cached;
    }

    @Override
    public void set(@Nullable T cache) {
        this.cached = cache;
    }
}
