package org.kingdoms.asm;

import org.jetbrains.annotations.NotNull;

public interface ClassDefiner {
    /**
     * Returns if the defined classes can bypass access checks
     *
     * @return if classes bypass access checks
     */
    default boolean isBypassAccessChecks() {
        return false;
    }

    /**
     * Define a class
     *
     * @param parentLoader the parent classloader
     * @param name         the name of the class
     * @param data         the class data to load
     * @return the defined class
     * @throws ClassFormatError     if the class data is invalid
     * @throws NullPointerException if any of the arguments are null
     */
    @NotNull
    Class<?> defineClass(@NotNull ClassLoader parentLoader, @NotNull String name, @NotNull byte[] data);

    @NotNull
    static ClassDefiner getInstance() {
        return SafeClassDefiner.INSTANCE;
    }
}
