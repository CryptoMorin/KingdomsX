package org.kingdoms.utils.internal.functional;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.*;

/**
 * Used when required type is {@link Object} so instead of casting this is slightly more concise.
 */
public final class Fn {
    public static <T> Supplier<T> supply(Supplier<T> supplier) {
        return supplier;
    }

    public static <T> Supplier<T> supply(T constant) {
        return new ConstantSupplier<>(constant);
    }

    public static <T> Callable<T> call(Callable<T> callable) {
        return callable;
    }

    public static <T> Predicate<T> predicate(Predicate<T> predicate) {
        return predicate;
    }

    public static <T> Consumer<T> consume(Consumer<T> consumer) {
        return consumer;
    }

    public static Runnable run(Runnable runnable) {
        return runnable;
    }

    public static <T, R> Function<T, R> function(Function<T, R> function) {
        return function;
    }

    public static <T, U, R> BiFunction<T, U, R> function(BiFunction<T, U, R> function) {
        return function;
    }

    /**
     * Used for Java's spastic generics' system.
     * E.g. converting {@literal Set<Integer>} to {@literal Set<Number>}
     */
    @SuppressWarnings("unchecked")
    public static <T> T cast(Object obj) {
        return (T) obj;
    }

    @SuppressWarnings("unchecked")
    public static <T> @Nullable T safeCast(@NonNull Object object, Class<T> clazz) {
        Objects.requireNonNull(object, "Cannot safely cast null to generic type");
        // We can't simply just catch ClassCastException, that won't work.
        if (clazz.isInstance(object)) {
            // Don't use class.cast(), extra work.
            return (T) object;
        } else {
            return null;
        }
    }

    private static final Predicate<?> TRUE_PREDICATE = x -> true;
    private static final Predicate<?> FALSE_PREDICATE = x -> false;
    private static final Supplier<?> NULL_SUPPLIER = new ConstantSupplier<>(null);

    @SuppressWarnings("unchecked")
    public static <T> Supplier<T> nullSupplier() {
        return (Supplier<T>) NULL_SUPPLIER;
    }

    @SuppressWarnings("unchecked")
    public static <T> Predicate<T> alwaysTrue() {
        return (Predicate<T>) TRUE_PREDICATE;
    }

    @SuppressWarnings("unchecked")
    public static <T> Predicate<T> alwaysFalse() {
        return (Predicate<T>) FALSE_PREDICATE;
    }

    private static final class ConstantSupplier<T> implements Supplier<T> {
        private final T obj;

        private ConstantSupplier(T obj) {this.obj = obj;}

        @Override
        public T get() {
            return obj;
        }
    }

    private static final class ConstantCallable<T> implements Callable<T> {
        private final T obj;

        private ConstantCallable(T obj) {this.obj = obj;}

        @Override
        public T call() {
            return obj;
        }
    }

    @NonNull
    public static <T> ChainedCallable<T> take(Callable<T> supplier) {
        return new ChainedCallable<>(supplier);
    }

    public static final class ChainedCallable<R> {
        private Callable<R> handle;
        private boolean hasResult;
        private R result;
        private RuntimeException error;

        private ChainedCallable(Callable<R> handle) {
            this.handle = Objects.requireNonNull(handle);
            checkResult();
        }

        private void fail(Throwable ex) {
            if (error == null) error = new RuntimeException("All possibilities failed");
            error.addSuppressed(ex);
        }

        private boolean checkResult() {
            try {
                result = handle.call();
                return hasResult = true;
            } catch (Throwable e) {
                fail(e);
                return false;
            }
        }

        @NonNull
        public ChainedCallable<R> or(Callable<R> other) {
            if (!hasResult) {
                handle = other;
                checkResult();
            }
            return this;
        }

        /**
         * Adds the other value if not null, otherwise nothing happens.
         */
        @NonNull
        public ChainedCallable<R> or(@Nullable R other) {
            if (other == null) return this;
            return or(new ConstantCallable<>(other));
        }

        @Nullable
        public R orNull() {
            return orElse(new ConstantCallable<>(null));
        }

        public boolean isPresent() {
            return result != null;
        }

        @Nullable
        public R get() {
            return result;
        }

        @NonNull
        public R orElse(R finalCheck) {
            Objects.requireNonNull(finalCheck, "Final non-lambda object cannot be null");
            return orElse(new ConstantCallable<>(finalCheck));
        }

        @Nullable
        public R orElse(Callable<R> finalCheck) {
            if (!hasResult) {
                handle = finalCheck;
                checkResult();
            }

            if (hasResult) return result;
            else throw error;
        }
    }
}
