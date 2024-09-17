package org.kingdoms.utils.internal.functional;

import java.util.function.Supplier;

/**
 * This is simply a copy of {@link Supplier} to be used in methods parameters
 * in order to bypass Java's type erasure backwards compatibility.
 */
@FunctionalInterface
public interface SecondarySupplier<T> extends Supplier<T> {
    T get();
}
