package org.kingdoms.services;

public interface Service {
    default Throwable checkAvailability() {
        return null;
    }

    default String getServiceName() {return this.getClass().getSimpleName();}

    default void enable() {}

    default void disable() {}
}
