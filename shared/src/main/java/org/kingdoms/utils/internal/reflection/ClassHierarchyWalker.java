package org.kingdoms.utils.internal.reflection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class ClassHierarchyWalker<V> {
    private final Class<?> base;
    private final Function<Class<?>, V> find;
    private final List<Class<?>> walkedPath = new ArrayList<>();

    private ClassHierarchyWalker(Class<?> base, Function<Class<?>, V> find) {
        this.base = Objects.requireNonNull(base);
        this.find = Objects.requireNonNull(find);
        if (base == Class.class) throw new IllegalArgumentException("Recursive class parameter: " + base);
    }

    public static final class ClassData<V> {
        @NotNull
        public final List<Class<?>> walkedPath;
        @NotNull
        public final Class<?> clazz;
        @NotNull
        public final V data;

        public ClassData(@NotNull List<Class<?>> walkedPath, Class<?> clazz, V data) {
            this.walkedPath = walkedPath;
            this.clazz = clazz;
            this.data = data;
        }
    }

    private ClassData<V> check(Class<?> check) {
        if (check == Object.class) return null;
        {
            V found = find.apply(check);
            if (found != null) {
                return new ClassData<>(walkedPath, check, found);
            }
        }

        Class<?> superClass = check.getSuperclass();
        if (superClass != null) {
            ClassData<V> any = check(superClass);
            if (any != null) {
                walkedPath.add(superClass);
                return any;
            }
        }

        for (Class<?> interfacee : check.getInterfaces()) {
            ClassData<V> any = check(interfacee);
            if (any != null) {
                walkedPath.add(interfacee);
                return any;
            }
        }

        return null;
    }

    public ClassData<V> find() {
        ClassData<V> data = check(base);
        walkedPath.add(base);
        return data;
    }

    @Nullable
    public static <V> ClassData<V> walk(Class<?> base, Function<Class<?>, V> find) {
        return new ClassHierarchyWalker<>(base, find).find();
    }
}
