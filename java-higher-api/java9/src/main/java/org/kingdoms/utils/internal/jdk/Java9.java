package org.kingdoms.utils.internal.jdk;

public final class Java9 {
    public static Class<?> getCallerClass() {
        return StackWalker
                .getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .getCallerClass();
    }
}
