package org.kingdoms.utils.internal;

import org.kingdoms.versioning.JavaVersion;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public final class IllegalInjector {
    private static final boolean LEGAL;

    private static final MethodHandle VAR_HANDLE_SET;
    private static final Object MODIFIERS;

    static {
        LEGAL = JavaVersion.getVersion() < 14;
    }

    static {
        Object modifiers = null;
        MethodHandle varHandleSet = null;
        MethodHandles.Lookup publicLookup = MethodHandles.lookup();

        try {
            if (LEGAL) {
                Field modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                varHandleSet = publicLookup.unreflectSetter(modifiersField);
            } else {
                Class<?> varHandle = Class.forName("java.lang.invoke.VarHandle");
                MethodHandle lookup = publicLookup.findStatic(MethodHandles.class, "privateLookupIn",
                        MethodType.methodType(MethodHandles.Lookup.class, Class.class, MethodHandles.Lookup.class));
                MethodHandle findVarHandle = publicLookup.findVirtual(MethodHandles.Lookup.class, "findVarHandle",
                        MethodType.methodType(varHandle, Class.class, String.class, Class.class));
                varHandleSet = publicLookup.findVirtual(varHandle, "set", MethodType.methodType(void.class, Object[].class));

                try {
                    Object lookupObj = lookup.invoke(Field.class, MethodHandles.lookup());
                    modifiers = findVarHandle.invoke(lookupObj, Field.class, "modifiers", int.class);
                } catch (IllegalAccessException ignored) {

                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        VAR_HANDLE_SET = varHandleSet;
        MODIFIERS = modifiers;
    }

    public static void injectField(Field field, Object instance, Object value) throws IllegalArgumentException, IllegalAccessException {
        field.setAccessible(true);
        int modifiers = field.getModifiers() & ~Modifier.FINAL;

        try {
            if (LEGAL) VAR_HANDLE_SET.invoke(field, modifiers);
            else VAR_HANDLE_SET.invoke(MODIFIERS, new Object[]{field, modifiers});
        } catch (Throwable throwable) {
            if (!throwable.getMessage().contains("java.lang.invoke.VarHandle.asDirect")) throwable.printStackTrace();
        }

        field.set(instance, value);
    }
}
