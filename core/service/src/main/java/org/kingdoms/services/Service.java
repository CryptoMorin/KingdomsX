package org.kingdoms.services;

public interface Service {
    default Throwable checkAvailability() {
        return null;
    }

    default String getServiceName() {return this.getClass().getSimpleName();}

    default void enable() {}

    default void disable() {}

    /**
     * Used when multiple versions of a service is available.
     */
    default int version() { return 0; }
}
