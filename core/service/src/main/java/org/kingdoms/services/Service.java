package org.kingdoms.services;

public interface Service {
    default boolean isAvailable() {
        return true;
    }

    default void enable() {
    }

    default void disable() {
    }
}
