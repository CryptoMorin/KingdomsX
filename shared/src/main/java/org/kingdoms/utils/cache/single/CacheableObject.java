package org.kingdoms.utils.cache.single;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Any operation that can be cached. It may expire multiple times.
 */
public interface CacheableObject<T> extends Supplier<T> {
    /**
     * Invalidate the cache, but don't re-evaluate. Only re-evaluates if {@link #get()} is called again.
     */
    void invalidate();

    /**
     * Whether this operation is cached.
     */
    boolean isCached();

    /**
     * If the cached object equals the given object.
     * If the object is not cached, it will be.
     * <p>
     * Equivalent to:
     * <p><blockquote><pre>
     *     Objects.equals(get(), other);
     * </pre></blockquote>
     */
    default boolean contains(T other) {
        return Objects.equals(get(), other);
    }

    /**
     * Equivalent to:
     * <p><blockquote><pre>
     *     get() == null
     * </pre></blockquote>
     * @see #isPresent()
     */
    default boolean isNull() {
        return get() == null;
    }

    /**
     * Equivalent to:
     * <p><blockquote><pre>
     *     get() != null
     * </pre></blockquote>
     * @see #isNull()
     */
    default boolean isPresent() {
        return get() != null;
    }

    /**
     * Gets the cached object or caches it and returns it.
     */
    @Override
    T get();

    /**
     * Sets the cached value.
     */
    void set(T cache);
}
