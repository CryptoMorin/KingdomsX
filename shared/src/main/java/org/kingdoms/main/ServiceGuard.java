package org.kingdoms.main;

import org.jetbrains.annotations.ApiStatus;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Prevents the static initialization of classes when out of order. This is useful
 * for classes that their initialization are sensitive and require other services
 * to be checked/loaded first.
 */
@ApiStatus.Internal
public final class ServiceGuard {
    public enum ServiceState {GUARDED, AWAITING_INIT, LOADED}

    private ServiceGuard() {}

    private static final Map<Class<?>, ServiceState> SERVICES = new IdentityHashMap<>();
    private static ClassLoader LOADER;
    private static Supplier<Boolean> STRICT;

    public static void loader(Class<?> clazz, Supplier<Boolean> strict) {
        if (LOADER != null) throw new IllegalStateException("Loader already set: " + LOADER + " -> " + clazz);
        LOADER = clazz.getClassLoader();
        STRICT = strict;
    }

    private static void assertLoader() {
        if (LOADER == null)
            throw new IllegalStateException("Loader not set yet");
    }

    private static boolean isStrict() {
        return STRICT.get();
    }

    public static boolean isLoaded(Class<?> clazz) {
        assertLoader();
        if (!isStrict()) return true;
        return SERVICES.get(clazz) == ServiceState.LOADED;
    }

    public static void load(Class<?> clazz) {
        assertLoader();
        if (!isStrict()) return;

        try {
            SERVICES.put(clazz, ServiceState.AWAITING_INIT);
            Class.forName(clazz.getName(), true, LOADER);
            SERVICES.put(clazz, ServiceState.LOADED);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Error while intializing: " + clazz, e);
        }
    }

    public static void assertAwaitingInit() {
        assertLoader();
        if (!isStrict()) return;

        // Called from static <cinit> so it must be the second one.
        String className = new Throwable().getStackTrace()[1].getClassName();
        Class<?> service;
        ServiceState state;

        try {
            service = Class.forName(className, false, LOADER);
            state = SERVICES.get(service);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }

        if (state != ServiceState.AWAITING_INIT) {
            throw new IllegalStateException("Invalid state for service " + service + " is in state: "
                    + (state == null ? ServiceState.GUARDED : state).name());
        }
    }
}
