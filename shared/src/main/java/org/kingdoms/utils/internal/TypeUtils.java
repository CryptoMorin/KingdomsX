package org.kingdoms.utils.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class TypeUtils {
    private static final Map<Class<?>, Class<?>> WRAPPER_TO_PRIMITIVE = new HashMap<>();
    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER = new HashMap<>();

    static {
        // Wrapper → primitive
        WRAPPER_TO_PRIMITIVE.put(Boolean.class, boolean.class);
        WRAPPER_TO_PRIMITIVE.put(Byte.class, byte.class);
        WRAPPER_TO_PRIMITIVE.put(Short.class, short.class);
        WRAPPER_TO_PRIMITIVE.put(Integer.class, int.class);
        WRAPPER_TO_PRIMITIVE.put(Long.class, long.class);
        WRAPPER_TO_PRIMITIVE.put(Float.class, float.class);
        WRAPPER_TO_PRIMITIVE.put(Double.class, double.class);
        WRAPPER_TO_PRIMITIVE.put(Character.class, char.class);
        WRAPPER_TO_PRIMITIVE.put(Void.class, void.class);

        // Primitive → wrapper (build reverse map)
        for (Map.Entry<Class<?>, Class<?>> entry : WRAPPER_TO_PRIMITIVE.entrySet()) {
            PRIMITIVE_TO_WRAPPER.put(entry.getValue(), entry.getKey());
        }
    }

    public static boolean isBasicObject(Class<?> clazz) {
        return Character.class.isAssignableFrom(clazz) ||
                String.class.isAssignableFrom(clazz) ||
                Number.class.isAssignableFrom(clazz) ||
                Boolean.class.isAssignableFrom(clazz);
    }

    public static Class<?> toPrimitive(Class<?> clazz) {
        Objects.requireNonNull(clazz, "class");
        if (clazz.isPrimitive()) return clazz;
        return WRAPPER_TO_PRIMITIVE.get(clazz);
    }

    public static Class<?> toWrapper(Class<?> clazz) {
        Objects.requireNonNull(clazz, "class");
        if (!clazz.isPrimitive()) return clazz;
        return PRIMITIVE_TO_WRAPPER.get(clazz);
    }
}
